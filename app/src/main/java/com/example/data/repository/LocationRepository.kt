package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.UserLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationRepository(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<UserLocation> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext Result.failure(
                SecurityException("Location permission (COARSE or FINE) is not granted.")
            )
        }

        try {
            val cancellationTokenSource = CancellationTokenSource()

            val location: Location? = try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).awaitTask()
            } catch (e: Exception) {
                null
            } ?: try {
                fusedLocationClient.lastLocation.awaitTask()
            } catch (e: Exception) {
                null
            }

            if (location != null) {
                val (cityName, country) = resolveCityAndCountry(location.latitude, location.longitude)
                Result.success(
                    UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        cityName = cityName,
                        country = country,
                        isFromGps = true,
                        accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                        timestamp = location.time.takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
            } else {
                Result.failure(IllegalStateException("Unable to acquire GPS coordinates."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveCityAndCountry(latitude: Double, longitude: Double): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    return@withContext null to null
                }
                val geocoder = Geocoder(context, Locale.getDefault())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                val address = addresses.firstOrNull()
                                val city = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                                val country = address?.countryName
                                continuation.resume(city to country)
                            }

                            override fun onError(errorMessage: String?) {
                                continuation.resume(null to null)
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = addresses?.firstOrNull()
                    val city = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                    val country = address?.countryName
                    city to country
                }
            } catch (e: Exception) {
                null to null
            }
        }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
        addOnCanceledListener {
            if (continuation.isActive) {
                continuation.cancel()
            }
        }
    }
}
