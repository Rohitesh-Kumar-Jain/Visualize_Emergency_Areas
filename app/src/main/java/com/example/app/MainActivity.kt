package com.example.app

import android.graphics.Color
import android.os.Bundle
import android.util.Log
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

    private val geos: HashMap<String, Feature> = HashMap<String, Feature>()

    private val timeStampedData by lazy {
        val messagesLog = assets.open("messages1.log")
        val structureJSON = assets.open("structure1.json")
        getTimeStampedDataFromLogFile(messagesLog, structureJSON)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activityMainBinding.root)

        setApiKeyForApp()

        setupMap()
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
        mapView.graphicsOverlays.add(graphicsOverlay)

        val serviceFeatureTable =
            ServiceFeatureTable("https://services3.arcgis.com/R1QgHoeCpv6vXgCd/ArcGIS/rest/services/emergency_areas/FeatureServer/0")

        val featureLayer = FeatureLayer(serviceFeatureTable)

        map.operationalLayers.add(featureLayer)
        loadGeographies(featureLayer, serviceFeatureTable)
    }

    /**
     * Loads geographies
     */
    private fun loadGeographies(featureLayer : FeatureLayer, serviceFeatureTable : ServiceFeatureTable) {
//        featureLayer.clearSelection()
        val featureTableToQuery = featureLayer.featureTable
        val lineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f)
        val fillSymbol = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.YELLOW, lineSymbol)

        val query = QueryParameters()
        query.whereClause = ("1 = 1")

        val queryFields: ServiceFeatureTable.QueryFeatureFields = ServiceFeatureTable.QueryFeatureFields.LOAD_ALL
        val future: ListenableFuture<FeatureQueryResult> = serviceFeatureTable.queryFeaturesAsync(query, queryFields)

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

        launchSimulation()

        println("EXITOS")
        Log.e(TAG, "EXITOS")
    }

    private fun launchSimulation() {
        // set up timer with a certain interval.
        // at every interval, draw the map according to the results.

        println(timeStampedData)
        println("HEMLOZS")
    }

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
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