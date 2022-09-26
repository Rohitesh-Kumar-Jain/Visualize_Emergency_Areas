package com.example.app

import android.R
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.data.*
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.SimpleFillSymbol
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.example.app.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {

    private val activityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

    private val graphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

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
        mapView.graphicsOverlays.add(graphicsOverlay)

        createFeatureLayer(map)
    }

    // Create the parcels feature layer. When it is loaded, add a listener on the spinner.
    // Add the layer to the map, and set the map view's selection properties to red.
    private fun createFeatureLayer(map: ArcGISMap) {

        val serviceFeatureTable =
            ServiceFeatureTable("https://services3.arcgis.com/R1QgHoeCpv6vXgCd/ArcGIS/rest/services/emergency_areas/FeatureServer/0")

        val featureLayer = FeatureLayer(serviceFeatureTable)
        // give the layer an ID so we can easily find it later, then add it to the map
        featureLayer.id = "farcels"

        val messagesLog = assets.open("messages1.log")
        val structureJSON = assets.open("structure1.json")

        val timeStampedData = getTimeStampedDataFromLogFile(messagesLog, structureJSON)

        /**
         * Under Development Code
         */


        featureLayer.clearSelection()
        val featureTableToQuery = featureLayer.featureTable
        val lineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f)
        val fillSymbol = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.YELLOW, lineSymbol)

        val query = QueryParameters()
        query.whereClause = ("1 = 1")

//        val identifyLayerResultListenableFuture: ListenableFuture<IdentifyLayerResult> =
//            serviceFeatureTable.queryFeaturesAsync(query)
//        identifyLayerResultListenableFuture.addDoneListener {
//            try {
//                val identifyLayerResult =
//                    identifyLayerResultListenableFuture.get()
//                // create a textview to display field values
//                for (element in identifyLayerResult.elements) {
//                    val feature = element as Feature
//                    // create a map of all available attributes as name value pairs
//                    val attr =
//                        feature.attributes
//                    val keys: Set<String> = attr.keys
//                    for (key in keys) {
//                        var value = attr[key]
//
//                        println("$key | $value\n")
//                    }
//                }
//            } catch (e1: Exception) {
//
//            }
//        }

        val future: ListenableFuture<FeatureQueryResult> =
            serviceFeatureTable.queryFeaturesAsync(query)

        future.addDoneListener {
            try {
                // call get on the future to get the result
                val result = future.get()
                // check there are some results
                val resultIterator = result.iterator()

                while (resultIterator.hasNext()) {
                    val feature: Feature = resultIterator.next()
                    val attr: MutableMap<String, Any> = feature.attributes
                    val keys = attr.keys

                    for (key in keys) {
                        val value: Any? = attr.get(key)

                        println("${key} : ${value}")
                    }
                }
            } catch (e: Exception) {
                "That didn't work!".also {
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                    Log.e(TAG, it)
                }
            }
        }

        /**
         * Development ends here
         */

        map.operationalLayers.add(featureLayer)
    }

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
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
}