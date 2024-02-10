package com.github.yohannestz.geoforge.activity

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.utils.Utils
import com.github.yohannestz.geoforge.BuildConfig
import com.github.yohannestz.geoforge.R
import com.github.yohannestz.geoforge.adapters.GeoPointsAdapter
import com.github.yohannestz.geoforge.map.CacheSizeType
import com.github.yohannestz.geoforge.map.GeoForgeMapFactory
import com.github.yohannestz.geoforge.map.GeoForgeMoveSimulator
import com.github.yohannestz.geoforge.map.GeoForgeMoveSimulatorService
import com.github.yohannestz.geoforge.map.GeoForgeRoutePartitioner
import com.github.yohannestz.geoforge.map.MapType
import com.github.yohannestz.geoforge.map.TravelMode
import com.github.yohannestz.geoforge.utils.Constants
import com.github.yohannestz.geoforge.viewmodels.MainViewModel
import com.github.yohannestz.geoforge.viewmodels.TaskStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.*


class MainActivity : AppCompatActivity(), MapListener {

    private lateinit var mapView: MapView
    private lateinit var settingsButton: CardView
    private lateinit var currentLocationButton: CardView
    private lateinit var helpButton: CardView
    private lateinit var geoPointsRv: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var geoPointsLinearLayout: ConstraintLayout
    private lateinit var geoForgeLayout: ConstraintLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var geoPointsAdapter: GeoPointsAdapter
    private lateinit var simulator: GeoForgeMoveSimulator
    private lateinit var viewModel: MainViewModel
    private var startPoint = GeoPoint(9.0192, 38.7525)

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                checkMockLocationPermission()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

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
        stopButton = findViewById(R.id.stopButton)
        geoPointsLinearLayout = findViewById(R.id.geoPointsLinearLayout)
        geoForgeLayout = findViewById(R.id.geoForgeLinearLayout)

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        if (!isMockSettingsON()) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }

        val mapType = when (prefs.getString("mapType", "1")) {
            "1" -> MapType.MAPTILER_BASIC_V2
            "2" -> MapType.MAPTILER_DATAVIS
            "3" -> MapType.MAPTILER_OSM_URL
            "4" -> MapType.MAPTILER_DATAVIS_LIGHT
            "5" -> MapType.MAPTILER_OUTDOOR_V2
            else -> MapType.MAPTILER_DATAVIS_LIGHT
        }

        val cacheSizeType = when (prefs.getString("cacheSize", "1")) {
            "1" -> CacheSizeType.FIVE_MB
            "2" -> CacheSizeType.TEN_MB
            "3" -> CacheSizeType.FIFTY_MB
            "4" -> CacheSizeType.HUNDRED_MB
            else -> CacheSizeType.TEN_MB
        }

        val multiStopEnabled = prefs.getBoolean("multiStopEnabled", true)

        mapView.setMultiTouchControls(multiStopEnabled)
        setCacheTypeSize(cacheSizeType)
        mapView.setTileSource(GeoForgeMapFactory.createMap(mapType))

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
                    Log.e("longPress: ", "new Location detected thru long press")
                }
                if (viewModel.geoPoints.size < 4) {
                    val newGeoPoint =
                        GeoPoint(longPressLocation.latitude, longPressLocation.longitude)

                    val newGeoPointMarker = Marker(mapView)
                    newGeoPointMarker.position = newGeoPoint
                    newGeoPointMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    newGeoPointMarker.icon = ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.ic_location_baseline_blue
                    )
                    mapView.overlays.add(newGeoPointMarker)

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
                    geoForgeLayout.visibility = View.VISIBLE

                    val startPoint = viewModel.geoPoints[0]
                    val endPoint = viewModel.geoPoints[viewModel.geoPoints.size - 1]

                    val startMarker = Marker(mapView)
                    startMarker.position = startPoint
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    startMarker.icon = ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.ic_location_baseline_green
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

                    val moving = ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.ic_car
                    )

                    val customDistanceMetric: (GeoPoint, GeoPoint) -> Double = { p1, p2 ->
                        sqrt(
                            (p1.latitude - p2.latitude).pow(2) + (p1.longitude - p2.longitude).pow(2)
                        )
                    }

                    val partitioner = GeoForgeRoutePartitioner(TravelMode.CAR, customDistanceMetric)
                    val result = partitioner.partitionRouteIntoEqualSegments(
                        status.roadOverlay.actualPoints as ArrayList,
                        25
                    )
                    simulator = GeoForgeMoveSimulator(mapView, result, moving!!, 1025.0)
                    /*simulator.startSimulation(
                        onSimulationFinish = {
                            showFinishedDialog()
                            this.viewModelStore.clear() //reset state
                            geoPointsLinearLayout.visibility = View.GONE
                            geoForgeLayout.visibility = View.GONE
                        },
                        this@MainActivity,
                    )*/

                    val intent = Intent(this, GeoForgeMoveSimulatorService::class.java).apply {
                        putParcelableArrayListExtra(GeoForgeMoveSimulatorService.EXTRA_ROUTE_GEO_POINTS, result)
                        putExtra(GeoForgeMoveSimulatorService.EXTRA_MARKER_ICON, R.drawable.ic_car)
                        putExtra(GeoForgeMoveSimulatorService.EXTRA_SPEED_KM_PER_HOUR, 1025.0)
                    }
                    startService(intent)

                    val line = Polyline(mapView, true, false)
                    line.setPoints(result)
                    line.infoWindow = null
                    line.paint.style = Paint.Style.STROKE
                    line.paint.pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
                    line.color = Color.rgb(0, 191, 255)
                    line.paint.pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
                    mapView.overlays.add(0, line)
                    mapView.postInvalidate()
                }

                is TaskStatus.Error -> {
                    Toast.makeText(applicationContext, status.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        stopButton.setOnClickListener {
            if (simulator.isRunning) {
                simulator.stopSimulation()
                geoForgeLayout.visibility = View.GONE
                mapView.overlays.removeAt(mapView.overlays.size - 2)
                mapView.postInvalidate()
            }
        }

        mapView.overlays.add(tapOverlay)
    }

    private fun checkLocationPermission() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            requestLocationPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            //checkMockLocationPermission()
            initializeStartingPoint()
        }
    }

    private fun alertUser() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Permission Required")
        builder.setMessage("This app requires access to your device's location to provide relevant functionality.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setOnDismissListener {
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun initializeStartingPoint() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            alertUser()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location ->
                startPoint = GeoPoint(location.latitude, location.longitude)
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to get location. defaulting to Addis Ababa instead",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun isMockSettingsON(): Boolean {
        var isMockLocation = false
        isMockLocation = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val opsManager = this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                opsManager.checkOp(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    android.os.Process.myUid(),
                    BuildConfig.APPLICATION_ID
                ) == AppOpsManager.MODE_ALLOWED
            } else {
                Settings.Secure.getString(this.contentResolver, "mock_location") != "0"
            }
        } catch (e: Exception) {
            return isMockLocation
        }
        return isMockLocation

    }

    private fun checkMockLocationPermission() {
        initializeStartingPoint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
            Toast.makeText(this, "Please grant mock location permission", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setCacheTypeSize(cacheTypeSize: CacheSizeType) {
        val cacheSizeBytes: Long = when (cacheTypeSize) {
            CacheSizeType.TEN_MB -> 1024L * 1024L * 10L
            CacheSizeType.HUNDRED_MB -> 1024L * 1024L * 100L
            CacheSizeType.FIVE_MB -> 1024L * 1024L * 5L
            CacheSizeType.FIFTY_MB -> 1024L * 1024
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
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
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

    override fun onStart() {
        super.onStart()
        onStartOrResume()
    }

    override fun onResume() {
        super.onResume()
        onStartOrResume()
    }

    private fun onStartOrResume() {

    }
}