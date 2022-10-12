package com.example.app

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

    private var delay: Long = 500 // Milliseconds
    private var isTimerRunning = true
    

    val mCallout: Callout by lazy {
        mapView.callout
    }

    lateinit var areasLayer: FeatureLayer

    private val fab: FloatingActionButton by lazy {
        activityMainBinding.fab
    }

    private val resumePauseButton: Button by lazy {
        activityMainBinding.resumePauseButton
    }

    private val progressSeekBar: SeekBar by lazy {
        activityMainBinding.progressSeekBar
    }

    private val curProgressTextView: TextView by lazy {
        activityMainBinding.progressTextView
    }

    private val fpsSpinner: Spinner by lazy {
        activityMainBinding.fpsSpinner
    }

    private var count : Int = 0


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

        val table =
            ServiceFeatureTable("https://services3.arcgis.com/R1QgHoeCpv6vXgCd/ArcGIS/rest/services/emergency_areas/FeatureServer/0")
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
                        mCallout.dismiss()
                    }

                    callOutPolgyon(
                        Point(e.x.toInt(), e.y.toInt()),
                        mapView,
                        areasLayer,
                        mCallout,
                        applicationContext
                    )
                    return true
                }

                override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
                    if (fab.isExpanded) {
                        fab.isExpanded = false
                    }
                    return super.onTouch(view, motionEvent)
                }
            }
            // ensure the floating action button moves to be above the attribution view
            addAttributionViewLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                val heightDelta = bottom - oldBottom
                (fab.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin += heightDelta
            }
        }

        // show the options sheet when the floating action button is clicked
        fab.setOnClickListener {
            fab.isExpanded = true
        }

        fpsSpinner.apply {
            // create an adapter with fps options
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf("1000 ms", "500 ms", "200 ms", "100 ms")
            )

            // set period based on the fps option selected
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    delay = when (position) {
                        0 -> 1000
                        1 -> 500
                        2 -> 200
                        3 -> 100
                        else -> 1000
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            // start with Delay of 500 ms
            setSelection(1)
        }
    }

    fun toggleAnimationTimer(view: View) {
        isTimerRunning = when {
            isTimerRunning -> {
                resumePauseButton.text = getString(com.example.app.R.string.resume)
                // set the isTimerRunning flag to false
                false
            }
            else -> {
                runSimulation()
                resumePauseButton.text = getString(com.example.app.R.string.pause)
                // set the isTimerRunning flag to true
                true
            }
        }
    }

    fun runSimulation() {
        val handler = Handler()

        val runnable: Runnable = object : Runnable {
            override fun run() {
                drawStep(count)
                if (!isTimerRunning) return;
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
        if (isTimerRunning) {
            toggleAnimationTimer(resumePauseButton)
        }
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