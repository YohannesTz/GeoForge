package com.github.yohannestz.geoforge.map

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.*
import kotlin.concurrent.timerTask

class GeoForgeMoveSimulator(
    private val mapView: MapView,
    private val routeGeoPoints: ArrayList<GeoPoint>,
    private val markerIcon: Drawable,
    private val speedKmPerHour: Double
) {
    var isRunning: Boolean = false
    private val currentPositionMarker: Marker = Marker(mapView)
    private var currentPositionIndex = 0
    private var timer: Timer? = null

    fun startSimulation(onSimulationFinish: () -> Unit) {
        currentPositionMarker.icon = markerIcon
        mapView.overlays.add(currentPositionMarker)
        isRunning = true
        val distanceBetweenPoints = calculateDistanceBetweenPoints(routeGeoPoints)
        val timeIntervalMs = calculateTimeInterval(distanceBetweenPoints, speedKmPerHour)

        val handler = Handler(Looper.getMainLooper())
        timer = Timer()
        timer?.scheduleAtFixedRate(timerTask {
            handler.post {
                currentPositionMarker.position = routeGeoPoints[currentPositionIndex]
                mapView.invalidate()
                currentPositionIndex++

                if (currentPositionIndex >= routeGeoPoints.size) {
                    stopSimulation()
                    onSimulationFinish()
                    isRunning = false
                }
            }
        }, 0, timeIntervalMs.toLong())
    }

    fun stopSimulation() {
        timer?.cancel()
        timer = null
        isRunning = false
        mapView.overlays.remove(currentPositionMarker)
        mapView.postInvalidate()
    }

    private fun calculateDistanceBetweenPoints(routeGeoPoints: ArrayList<GeoPoint>): Double {
        var distance = 0.0
        for (i in 0 until routeGeoPoints.size - 1) {
            val startPoint = routeGeoPoints[i]
            val endPoint = routeGeoPoints[i + 1]
            distance += startPoint.distanceToAsDouble(endPoint)
        }
        return distance
    }

    private fun calculateTimeInterval(distance: Double, speedKmPerHour: Double): Double {
        val speedMetersPerSecond = speedKmPerHour * 10_000 /* was 1000 but I needed the speed... don't copy from GPT */ / 3600
        val timeInSeconds = distance / speedMetersPerSecond
        return timeInSeconds * 1000 // Convert to milliseconds
    }
}
