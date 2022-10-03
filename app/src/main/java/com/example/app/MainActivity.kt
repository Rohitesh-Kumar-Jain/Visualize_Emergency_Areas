package com.example.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.data.Feature
import com.esri.arcgisruntime.data.FeatureQueryResult
import com.esri.arcgisruntime.data.QueryParameters
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.SimpleRenderer
import com.example.app.databinding.ActivityMainBinding
import java.util.*



class MainActivity : AppCompatActivity() {

    private val activityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

//    private val graphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

    private var geos: HashMap<String, Feature> = HashMap<String, Feature>()

    private val timeStampedData by lazy {
        val messagesLog = assets.open("messages.log")
        val structureJSON = assets.open("structure.json")
        getTimeStampedDataFromLogFile(messagesLog, structureJSON)
    }

    private var timer = Timer()
    private val DELAY: Long = 500 // Milliseconds

    private lateinit var featureLayer : FeatureLayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activityMainBinding.root)

        setApiKeyForApp()

        setupMap()

        launchSimulation()
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
//        mapView.graphicsOverlays.add(graphicsOverlay)

        val serviceFeatureTable =
            ServiceFeatureTable("https://services3.arcgis.com/R1QgHoeCpv6vXgCd/ArcGIS/rest/services/emergency_areas/FeatureServer/0")

        featureLayer = FeatureLayer(serviceFeatureTable)

        map.operationalLayers.add(featureLayer)
        loadGeographies(featureLayer, serviceFeatureTable)
    }

    /**
     * Loads geographies
     */
    private fun loadGeographies(featureLayer : FeatureLayer, serviceFeatureTable : ServiceFeatureTable) {
        val query = QueryParameters()
        query.whereClause = ("1 = 1")

        val queryFields: ServiceFeatureTable.QueryFeatureFields = ServiceFeatureTable.QueryFeatureFields.LOAD_ALL
        val future: ListenableFuture<FeatureQueryResult> = serviceFeatureTable.queryFeaturesAsync(query, queryFields)

        future.let {
            try {
                val result = future.get()
                val resultIterator = result.iterator()

                while (resultIterator.hasNext()) {
                    val feature: Feature = resultIterator.next()
                    val id  = feature.attributes.get("id")
                    geos[id.toString()] = feature
                }
            } catch (e: Exception) {
                "That didn't work!".also {
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                    Log.e(TAG, it)
                }
            }
        }
    }

    private fun launchSimulation() {
        // set up timer with a certain interval.
        // at every interval, draw the map according to the results.

//        val curStampedData1: MutableList<LogFileData> = timeStampedData.get(17)
//        val curStampedData2: MutableList<LogFileData> = timeStampedData.get(117)

//        System.out.println(timeStampedData)
//        System.out.println(geos)

        Log.e(TAG, "Launch Simulation")

//        for (curStampedData in timeStampedData) {
//
//            timer.cancel()
//            timer = Timer()
//
//            Log.e(TAG, "Next Iteration")
//
//            val stopWatch = Stopwatch.createStarted()
//            val timer = Timer()
//            timer.schedule(object : TimerTask() {
//                override fun run() {
//                    stopWatch.stop()
//
//                    for (logFileData in curStampedData) {
//                        println("id ${logFileData.components.id}   message_data  ${logFileData.message_data}")
//
//                        val simpleFillSymbol = getSimpleFillSymbol(logFileData.message_data)
//                        val feature = geos.get(logFileData.components.id)
//                        val graphic = Graphic(feature?.geometry)
//
//                        println(feature)
//
//                        val simpleRenderer = SimpleRenderer(simpleFillSymbol)
//
//                        val go = GraphicsOverlay().apply {
//                            graphics.add(graphic)
//                            renderer = simpleRenderer
//                        }
//
//                        mapView.graphicsOverlays.add(go)
//                    }
//                    timer.cancel()
//                }
//            }, 10000)
//        }

        for (curStampedData in timeStampedData) {

            Log.e(TAG, "Next Iteration")

            val map = ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC)
            mapView.map = map

            val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()
            mapView.graphicsOverlays.add(graphicsOverlay)

            for (logFileData in curStampedData) {
                println("id ${logFileData.components.id}   message_data  ${logFileData.message_data}")

                val simpleFillSymbol = getSimpleFillSymbol(logFileData.message_data)
                val feature = geos.get(logFileData.components.id)
                val graphic = Graphic(feature?.geometry)

                println(feature)

                val simpleRenderer = SimpleRenderer(simpleFillSymbol)

                val go = GraphicsOverlay().apply {
                    graphics.add(graphic)
                    renderer = simpleRenderer
                }

                mapView.graphicsOverlays.add(go)
            }
            mapView.map
            Thread.sleep(5000)
        }

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