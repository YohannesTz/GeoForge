package com.github.yohannestz.geoforge.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock

/**
 * Thanks to
 * https://github.com/mcastillof/FakeTraveler/blob/master/app/src/main/java/cl/coders/faketraveler/MockLocationProvider.java
 */


class MockLocationProvider(name: String, ctx: Context) {
    private var providerName: String
    private var ctx: Context

    /**
     * Class constructor
     *
     * @param name provider
     * @param ctx  context
     * @return Void
     */
    init {
        providerName = name
        this.ctx = ctx
        var powerUsage = 0
        var accuracy = 5
        if (Build.VERSION.SDK_INT >= 30) {
            powerUsage = 1
            accuracy = 2
        }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startup(lm, powerUsage, accuracy,  /* maxRetryCount= */3,  /* currentRetryCount= */0)
    }

    private fun startup(
        lm: LocationManager,
        powerUsage: Int,
        accuracy: Int,
        maxRetryCount: Int,
        currentRetryCount: Int
    ) {
        if (currentRetryCount < maxRetryCount) {
            try {
                shutdown()
                lm.addTestProvider(
                    providerName,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    powerUsage,
                    accuracy
                )
                lm.setTestProviderEnabled(providerName, true)
            } catch (e: Exception) {
                startup(lm, powerUsage, accuracy, maxRetryCount, currentRetryCount + 1)
            }
        } else {
            throw SecurityException("Not allowed to perform MOCK_LOCATION")
        }
    }

    /**
     * Pushes the location in the system (mock). This is where the magic gets done.
     *
     * @param lat latitude
     * @param lon longitude
     * @return Void
     */
    fun pushLocation(lat: Double, lon: Double) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val mockLocation = Location(providerName)
        mockLocation.latitude = lat
        mockLocation.longitude = lon
        mockLocation.altitude = 3.0
        mockLocation.time = System.currentTimeMillis()
        //mockLocation.setAccuracy(16F);
        mockLocation.speed = 0.01f
        mockLocation.bearing = 1f
        mockLocation.accuracy = 3f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.bearingAccuracyDegrees = 0.1f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.verticalAccuracyMeters = 0.1f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.speedAccuracyMetersPerSecond = 0.01f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        lm.setTestProviderLocation(providerName, mockLocation)
    }

    /**
     * Removes the provider
     *
     * @return Void
     */
    fun shutdown() {
        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.removeTestProvider(providerName)
        } catch (e: Exception) {
        }
    }
}