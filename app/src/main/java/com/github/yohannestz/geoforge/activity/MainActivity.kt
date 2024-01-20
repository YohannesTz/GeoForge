package com.github.yohannestz.geoforge.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.yohannestz.geoforge.BuildConfig
import com.github.yohannestz.geoforge.R
import com.github.yohannestz.geoforge.adapters.GeoPointsAdapter
import com.github.yohannestz.geoforge.map.CacheTypeSize
import com.github.yohannestz.geoforge.map.GeoForgeMapFactory
import com.github.yohannestz.geoforge.map.MapType
import com.github.yohannestz.geoforge.viewmodels.MainViewModel
import com.github.yohannestz.geoforge.viewmodels.TaskStatus
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : AppCompatActivity(), MapListener {

    private lateinit var mapView: MapView
    private lateinit var settingsButton: CardView
    private lateinit var currentLocationButton: CardView
    private lateinit var helpButton: CardView
    private lateinit var geoPointsRv: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: Button
    private lateinit var geoPointsLinearLayout: ConstraintLayout

    private lateinit var geoPointsAdapter: GeoPointsAdapter
    private lateinit var viewModel: MainViewModel
    private val startPoint = GeoPoint(9.0192, 38.7525)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance()
            .load(ctx, getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE))
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        mapView = findViewById(R.id.osmmap)
        settingsButton = findViewById(R.id.settingsButton)
        currentLocationButton = findViewById(R.id.currentLocation)
        helpButton = findViewById(R.id.helpButton)
        geoPointsRv = findViewById(R.id.geoPointsRv)
        startButton = findViewById(R.id.startButton)
        progressBar = findViewById(R.id.progressBar)
        geoPointsLinearLayout = findViewById(R.id.geoPointsLinearLayout)

        mapView.setMultiTouchControls(true)
        setCacheTypeSize(CacheTypeSize.FIFTY_MB)
        mapView.setTileSource(GeoForgeMapFactory.createMap(MapType.MAPTILER_OSM_URL))

        val mMyLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        val controller: IMapController = mapView.controller
        controller.animateTo(startPoint)
        controller.setZoom(18.0)
        mapView.overlays.add(mMyLocationOverlay)
        mapView.addMapListener(this)

        settingsButton.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        //just for now
        currentLocationButton.visibility = View.GONE
        helpButton.setOnClickListener {
            showHelpDialog()
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        geoPointsAdapter = GeoPointsAdapter(viewModel.geoPoints, this)
        geoPointsRv.layoutManager = LinearLayoutManager(this)
        geoPointsRv.adapter = geoPointsAdapter
        startButton.isEnabled = false

        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return true
            }

            override fun longPressHelper(longPressLocation: GeoPoint): Boolean {
                if (BuildConfig.DEBUG) {
                    Log.e("longPress: ", "new Location")
                }
                if (viewModel.geoPoints.size < 4) {
                    val newGeoPoint =
                        GeoPoint(longPressLocation.latitude, longPressLocation.longitude)
                    viewModel.geoPoints.add(newGeoPoint)
                    startButton.isEnabled = true
                    geoPointsAdapter.notifyItemInserted(viewModel.geoPoints.size - 1)
                }

                return true
            }
        })

        startButton.setOnClickListener {
            viewModel.fetchRoadData(applicationContext, viewModel.geoPoints)
        }

        viewModel.getTaskStatus().observe(this) { status ->
            when (status) {
                is TaskStatus.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    startButton.visibility = View.INVISIBLE
                }

                is TaskStatus.Success -> {
                    geoPointsLinearLayout.visibility = View.GONE
                    val startPoint = viewModel.geoPoints[0]
                    val endPoint = viewModel.geoPoints[viewModel.geoPoints.size - 1]

                    val startMarker = Marker(mapView)
                    startMarker.position = startPoint
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    startMarker.icon = ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.ic_location_baseline_blue
                    )
                    startMarker.title = "Start point"
                    mapView.overlays.add(startMarker)

                    val endMarker = Marker(mapView)
                    endMarker.position = endPoint
                    endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    endMarker.icon = ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.ic_location_baseline_red
                    )
                    endMarker.title = "End point"
                    mapView.overlays.add(endMarker)

                    mapView.overlays.add(status.roadOverlay)
                }

                is TaskStatus.Error -> {
                    Toast.makeText(applicationContext, status.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        mapView.overlays.add(tapOverlay)
    }

    private fun setCacheTypeSize(cacheTypeSize: CacheTypeSize) {
        val cacheSizeBytes: Long = when (cacheTypeSize) {
            CacheTypeSize.TEN_MB -> 1024L * 1024L * 10L
            CacheTypeSize.HUNDRED_MB -> 1024L * 1024L * 100L
            CacheTypeSize.FIVE_MB -> 1024L * 1024L * 5L
            CacheTypeSize.FIFTY_MB -> 1024L * 1024
        }

        Configuration.getInstance().tileFileSystemCacheTrimBytes = cacheSizeBytes
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        return true
    }

    private fun showHelpDialog() {
        val helpMessage = resources.getString(R.string.help_text)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Help")
            .setMessage(helpMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog when "OK" is clicked
            }
            .create()

        alertDialog.show()
    }

}