package com.example.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.MapView

import com.example.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val activityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("Start of the program")

        setContentView(activityMainBinding.root)

        setApiKeyForApp()

        setupMap()
    }

    override fun onPause() {
        mapView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onDestroy() {
        mapView.dispose()
        super.onDestroy()
    }

    private fun setApiKeyForApp() {
        ArcGISRuntimeEnvironment.setApiKey("AAPK5ea618c24b1d43ca9672b8329c88adc1EKFW0i1WBQ6pD9DAHnOCR7zqJXuvk2UobY9YsrsAcu63hUutb4MaMpY51iszA7bP")
    }

    /**
     * Sets up a map
     */
    private fun setupMap() {
        // create a map with a topographic basemap and set the map on the mapview
        val map = ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC)
        mapView.map = map

        // set the viewpoint, Viewpoint(latitude, longitude, scale)
        mapView.setViewpoint(Viewpoint(43.8971, -78.8658, 72000.0))

        createFeatureLayer(map)
    }

    // Create the parcels feature layer. When it is loaded, add a listener on the spinner.
    // Add the layer to the map, and set the map view's selection properties to red.
    private fun createFeatureLayer(map: ArcGISMap) {

        val serviceFeatureTable =
            ServiceFeatureTable("https://services3.arcgis.com/R1QgHoeCpv6vXgCd/ArcGIS/rest/services/emergency_areas/FeatureServer/0")

        val parcelsFeatureLayer = FeatureLayer(serviceFeatureTable)

        // give the layer an ID so we can easily find it later, then add it to the map
        parcelsFeatureLayer.id = "Parcels"

        val messagesLog = assets.open("messages1.log")
        val structureJSON = assets.open("structure1.json")

        val timeStampedData = getTimeStampedDataFromLogFile(messagesLog, structureJSON)

        for (data in timeStampedData) {
            for (hehe in data) {
                println("${hehe.message_data}  ${hehe.components}")
            }
        }

        map.operationalLayers.add(parcelsFeatureLayer)
    }

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }
}