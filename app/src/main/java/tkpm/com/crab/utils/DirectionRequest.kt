package tkpm.com.crab.utils

import android.graphics.Color
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONException
import org.json.JSONObject
import tkpm.com.crab.BuildConfig
import java.net.URL

public fun addressOverview(address: String): String {
    return address.substring(0, address.indexOf(","))
}
private fun drawPath(result: String?, mMap: GoogleMap, polylines: MutableList<Any>) {
    try {
        // Tranform the string into a json object
        val json = JSONObject(result!!)
        val routeArray = json.getJSONArray("routes")
        val routes = routeArray.getJSONObject(0)
        val overviewPolylines = routes.getJSONObject("overview_polyline")
        val encodedString = overviewPolylines.getString("points")
        val list = decodePoly(encodedString)
        // clearLines()
        val line = mMap.addPolyline(
            PolylineOptions().addAll(list).width(12f)
                .color(Color.parseColor("#05b1fb")) // Google maps blue color
                .geodesic(true)
        )
        polylines.add(line)
    } catch (e: JSONException) {
    }
}

private fun decodePoly(encoded: String): List<LatLng> {
    val poly: MutableList<LatLng> = ArrayList()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0
    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        val p = LatLng(
            lat.toDouble() / 1E5, lng.toDouble() / 1E5
        )
        poly.add(p)
    }
    return poly
}

public class DirectionRequest : AsyncTask<Void, Void, String> {
    private val mMap: GoogleMap
    private val url: String
    private val polylines: MutableList<Any>

    constructor(
        mMap: GoogleMap,
        pickup: LatLng,
        destination: LatLng,
        polylines: MutableList<Any> = mutableListOf()
    ) {
        this.mMap = mMap
        this.url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=" + pickup.latitude + "," + pickup.longitude + "&destination=" + destination.latitude + "," + destination.longitude + "&key=" + BuildConfig.MAPS_API_KEY
        this.polylines = mutableListOf()
    }

    constructor(mMap: GoogleMap, url: String, polylines: MutableList<Any> = mutableListOf()) {
        this.mMap = mMap
        this.url = url
        this.polylines = mutableListOf()
    }

    override fun doInBackground(vararg params: Void?): String? {
        val result: String
        val handler = Handler(Looper.getMainLooper())
        try {
            val data = url.let { URL(it).readText() }
            result = data
        } catch (e: Exception) {
            handler.post {
            }
            return null
        }
        return result
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        drawPath(result, mMap, polylines)
    }
}