package com.firelink.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationHelper(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun current(): Location {
        val token = CancellationTokenSource()
        return client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
            ?: error("موقعیت دریافت نشد. GPS را بررسی کنید.")
    }
}
