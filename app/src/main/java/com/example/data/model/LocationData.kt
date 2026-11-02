package com.example.data.model

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String? = null,
    val country: String? = null,
    val isFromGps: Boolean = false,
    val accuracyMeters: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)
