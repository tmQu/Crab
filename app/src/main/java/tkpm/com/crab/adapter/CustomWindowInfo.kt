package tkpm.com.crab.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker
import tkpm.com.crab.R

class CustomWindowInfo(val context: Context): InfoWindowAdapter {
    private var mLayout = LayoutInflater.from(context).inflate(R.layout.custom_windowinfo, null)

    fun renderWindow(marker: Marker): View {
        val title = marker.title
        val snippet = marker.snippet

        val titleView = mLayout.findViewById<TextView>(R.id.title)
        val snippetView = mLayout.findViewById<TextView>(R.id.snippet)

        titleView.text = title
        snippetView.text = snippet
        return mLayout
    }
    override fun getInfoContents(p0: Marker): View? {
        return renderWindow(p0)
    }

    override fun getInfoWindow(p0: Marker): View? {
        return renderWindow(p0)
    }
}