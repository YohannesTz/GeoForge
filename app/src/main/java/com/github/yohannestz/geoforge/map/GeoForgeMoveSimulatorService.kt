package com.github.yohannestz.geoforge.map

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.github.yohannestz.geoforge.R
import com.github.yohannestz.geoforge.activity.MainActivity
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.*

class GeoForgeMoveSimulatorService : Service() {

    private lateinit var mapView: MapView
    private lateinit var routeGeoPoints: ArrayList<GeoPoint>
    private lateinit var markerIcon: Drawable
    private var speedKmPerHour: Double = 0.0

    private lateinit var geoForgeMoveSimulator: GeoForgeMoveSimulator
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)

            // Retrieve parameters from the intent
            mapView = intent.getParcelableExtra(EXTRA_MAP_VIEW)!!
            routeGeoPoints = intent.getParcelableArrayListExtra(EXTRA_ROUTE_GEO_POINTS)!!
            markerIcon = ContextCompat.getDrawable(
                this,
                intent.getParcelableExtra(EXTRA_MARKER_ICON)!!
            )!!
            speedKmPerHour = intent.getDoubleExtra(EXTRA_SPEED_KM_PER_HOUR, 0.0)

            // Initialize and start the GeoForgeMoveSimulator
            geoForgeMoveSimulator =
                GeoForgeMoveSimulator(mapView, routeGeoPoints, markerIcon, speedKmPerHour)
            geoForgeMoveSimulator.startSimulation(
                onSimulationFinish = {
                    stopSelf()
                    showFinishedDialog()
                }, context = this
            )
        }
        return START_STICKY
    }

    private fun showFinishedDialog() {
        val helpMessage = resources.getString(R.string.finished)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Help")
            .setMessage(helpMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            geoForgeMoveSimulator.stopSimulation()
        }
    }

    private fun createNotification(): Notification {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("geo_forge_service", "GeoForgeMoveSimulator Service")
            } else {
                ""
            }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("GeoForgeMoveSimulator is running")
            .setContentText("Tap to return to the app")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentIntent(pendingIntent)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_MAP_VIEW = "map_view"
        const val EXTRA_ROUTE_GEO_POINTS = "route_geo_points"
        const val EXTRA_MARKER_ICON = "marker_icon"
        const val EXTRA_SPEED_KM_PER_HOUR = "speed_km_per_hour"
    }
}
