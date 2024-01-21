package com.github.yohannestz.geoforge.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.github.yohannestz.geoforge.BuildConfig
import com.github.yohannestz.geoforge.R
import com.github.yohannestz.geoforge.utils.Constants
import io.supercharge.shimmerlayout.ShimmerLayout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.IOException

class GeoPointsAdapter(private val geoPoints: List<GeoPoint>, private val context: Context) :
    RecyclerView.Adapter<GeoPointsAdapter.GeoPointViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeoPointViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_geo_point, parent, false)
        return GeoPointViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: GeoPointViewHolder, position: Int) {
        val geoPoint = geoPoints[position]
        holder.bind(geoPoint)
    }

    override fun getItemCount(): Int {
        return geoPoints.size
    }

    class GeoPointViewHolder(itemView: View, private val context: Context) :
        RecyclerView.ViewHolder(itemView) {
        private val latLangTextView: TextView = itemView.findViewById(R.id.latLangTextView)
        private val placeNameTextView: TextView = itemView.findViewById(R.id.placeNameTextView)
        private val shimmerLayout: ShimmerLayout = itemView.findViewById(R.id.shimmerLayout)

        @SuppressLint("SetTextI18n")
        fun bind(geoPoint: GeoPoint) {
            latLangTextView.text = "Latitude: ${geoPoint.latitude} Longitude: ${geoPoint.longitude}"
            fetchPlaceName(geoPoint, context)
        }

        private fun fetchPlaceName(geoPoint: GeoPoint, context: Context) {
            val client = OkHttpClient()
            val url =
                "https://geocode.maps.co/reverse?lat=${geoPoint.latitude}&lon=${geoPoint.longitude}&api_key=${BuildConfig.GEOCODE_API_KEY}"

            if (BuildConfig.DEBUG) {
                Log.e("url: ", url)
            }

            val request = Request.Builder()
                .url(url)
                .build()

            shimmerLayout.startShimmerAnimation()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    //Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }

                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    val placeName = parsePlaceName(responseBody)
                    placeNameTextView.post {
                        shimmerLayout.stopShimmerAnimation()
                        shimmerLayout.visibility = View.GONE
                        placeNameTextView.visibility = View.VISIBLE
                        placeNameTextView.text = placeName
                    }
                }
            })
        }

        private fun parsePlaceName(responseBody: String?): String {
            try {
                val json = responseBody?.let { JSONObject(it) }
                if (json != null) {
                    return json.getString(Constants.GEOCODING_DISPLAY_NAME_KEY)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return "Unknown"
        }
    }
}