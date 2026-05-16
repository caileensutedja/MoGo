package com.fit3161.fit3162.mogo.com.fit3161.fit3162.mogo.data.model

import com.google.android.gms.maps.model.LatLng

data class DriverLocation(
    val driverId: String,
    val name: String,
    val latLng: LatLng
)

