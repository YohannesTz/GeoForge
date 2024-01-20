package com.github.yohannestz.geoforge.viewmodels

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yohannestz.geoforge.BuildConfig
import com.github.yohannestz.geoforge.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline

sealed class TaskStatus {
    data object Loading : TaskStatus()
    data class Success(val roadOverlay: Polyline) : TaskStatus()
    data class Error(val message: String) : TaskStatus()
}

class MainViewModel : ViewModel() {
    val geoPoints: ArrayList<GeoPoint> = ArrayList()
    private val taskStatus: MutableLiveData<TaskStatus> = MutableLiveData()
    fun getTaskStatus(): LiveData<TaskStatus> = taskStatus

    fun fetchRoadData(context: Context, waypoints: ArrayList<GeoPoint>) {
        taskStatus.value = TaskStatus.Loading
        viewModelScope.launch {
            try {
                val road = withContext(Dispatchers.IO) {
                    val roadManager: RoadManager = OSRMRoadManager(context, "OBP_Tuto/1.0")
                    roadManager.getRoad(waypoints)
                }
                if (road.mStatus == Road.STATUS_OK) {
                    val roadOverlay = RoadManager.buildRoadOverlay(
                        road,
                        ContextCompat.getColor(context, R.color.black),
                        10f
                    )
                    taskStatus.value = TaskStatus.Success(roadOverlay)
                } else {
                    taskStatus.value = TaskStatus.Error("Error when loading the road")
                    if (BuildConfig.DEBUG) {
                        Log.e("roadObj: ", road.toString())
                    }
                }
            } catch (e: Exception) {
                taskStatus.value = TaskStatus.Error("An error occurred")
                e.printStackTrace()
            }
        }
    }
}