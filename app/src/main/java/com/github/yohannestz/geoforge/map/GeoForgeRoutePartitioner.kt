package com.github.yohannestz.geoforge.map

import org.osmdroid.util.GeoPoint
import kotlin.math.pow
import kotlin.math.sqrt
enum class TravelMode(val speedKmPerHour: Double) {
    FOOT(5.0), // Example speed for walking in km/h
    BIKE(20.0), // Example speed for biking in km/h
    CAR(60.0) // Example speed for driving in km/h
}

class GeoForgeRoutePartitioner(
    private val travelMode: TravelMode,
    private val distanceMetric: (GeoPoint, GeoPoint) -> Double
) {

    fun getDirection(v1: GeoPoint, v2: GeoPoint): DoubleArray {
        val dist = distanceMetric(v1, v2)
        return doubleArrayOf((v2.latitude - v1.latitude) / dist, (v2.longitude - v1.longitude) / dist)
    }

    private fun getTravelTime(v1: GeoPoint, v2: GeoPoint): Double {
        return distanceMetric(v1, v2) / (travelMode.speedKmPerHour / 3.6) // Convert speed to m/s
    }

    fun partitionRouteIntoEqualSegments(routeGeoPoints: ArrayList<GeoPoint>, segments: Int): ArrayList<GeoPoint> {
        if (segments <= 0) {
            throw IllegalArgumentException("Number of segments must be greater than 0")
        }

        val totalPoints = routeGeoPoints.size
        if (totalPoints < 2) {
            throw IllegalArgumentException("Route must have at least 2 GeoPoints")
        }

        val partitionedRoute = ArrayList<GeoPoint>()

        for (i in 0 until totalPoints - 1) {
            val startPoint = routeGeoPoints[i]
            val endPoint = routeGeoPoints[i + 1]

            val travelTime = getTravelTime(startPoint, endPoint)
            val numSubSegments = segments - 1

            partitionedRoute.add(startPoint) // Add the start point of the segment

            for (j in 1 until segments) {
                val ratio = j.toDouble() / numSubSegments.toDouble()
                val interpolatedPoint = GeoPoint(
                    startPoint.latitude + (endPoint.latitude - startPoint.latitude) * ratio,
                    startPoint.longitude + (endPoint.longitude - startPoint.longitude) * ratio
                )

                partitionedRoute.add(interpolatedPoint)
            }
        }

        partitionedRoute.add(routeGeoPoints.last()) // Add the last point of the route
        return partitionedRoute
    }
}
