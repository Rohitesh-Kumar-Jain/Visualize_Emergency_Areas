package com.example.app

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
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
import com.esri.arcgisruntime.mapping.view.*
import com.example.app.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {

    val activityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

    private var geos: HashMap<String, Graphic> = HashMap<String, Graphic>()
    private var overlay: GraphicsOverlay = GraphicsOverlay()

    private val data by lazy {
        val messagesLog = assets.open("messages.log")
        val structureJSON = assets.open("structure.json")
        getTimeStampedDataFromLogFile(messagesLog, structureJSON)
    }

    private val delay: Long = 1000 // Milliseconds

    val mCallout : Callout by lazy {
        mapView.callout
    }

    lateinit var areasLayer: FeatureLayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activityMainBinding.root)

        setApiKeyForApp()

        setupMap()

        setupUI()

        runSimulation()
    }

    private fun setApiKeyForApp() {
        ArcGISRuntimeEnvironment.setApiKey("AAPK5ea618c24b1d43ca9672b8329c88adc1EKFW0i1WBQ6pD9DAHnOCR7zqJXuvk2UobY9YsrsAcu63hUutb4MaMpY51iszA7bP")
    }

    /**
     * Sets up a map
     */
    private fun setupMap() {
        mapView.map = ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC)
        mapView.setViewpoint(Viewpoint(43.8971, -78.8658, 72000.0))
        mapView.graphicsOverlays.add(overlay)

        val qp = QueryParameters()
        val qf = ServiceFeatureTable.QueryFeatureFields.LOAD_ALL

        qp.whereClause = ("1 = 1")

        val table = ServiceFeatureTable("https://services3.arcgis.com/R1QgHoeCpv6vXgCd/ArcGIS/rest/services/emergency_areas/FeatureServer/0")
        areasLayer = FeatureLayer(table)
        mapView.map.operationalLayers.add(areasLayer)
        val future: ListenableFuture<FeatureQueryResult> = table.queryFeaturesAsync(qp, qf)

        future.let {
            val iterator = future.get().iterator()

            while (iterator.hasNext()) {
                val f: Feature = iterator.next()
                val id = f.attributes["id"].toString()

                geos[id] = Graphic(f.geometry, f.attributes)
                overlay.graphics.add(geos[id])
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        mapView.apply {
            onTouchListener = object : DefaultMapViewOnTouchListener(this@MainActivity, mapView) {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (mCallout.isShowing) {
                        mCallout.dismiss();
                    }

                    callOutPolgyon(Point(e.x.toInt(), e.y.toInt()), mapView, areasLayer, mCallout, applicationContext)
                    return true
                }
            }
        }
    }

    private fun runSimulation() {
        val handler = Handler()
        var count = 0

        val runnable: Runnable = object : Runnable {
            override fun run() {
                drawStep(count)

                if (count++ < data.size) handler.postDelayed(this, delay)
            }
        }

        // trigger first time
        handler.post(runnable)
    }

    private fun drawStep(i: Int) {
        println("drawing step $i...")
        for (d in data[i]) {
            val g = geos[d.components.id]

            if (g != null) {
                g.symbol = getSimpleFillSymbol(d.message_data)
            }
        }
    }

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
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