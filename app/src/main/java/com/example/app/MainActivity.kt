package com.example.app

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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

    private var count: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activityMainBinding.root)

        setApiKeyForApp()

        setupMap()

        setupUI()

        runSimulation()
    }

    /**
     * Setup ArcGIS online API key
     *
     * You can get your own ArcGIS online API key from https://developers.arcgis.com/sign-up/
     */
    private fun setApiKeyForApp() {
        val ai: ApplicationInfo = applicationContext.packageManager
            .getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
        val value = ai.metaData["keyValue"]

        val key = value.toString()
        ArcGISRuntimeEnvironment.setApiKey(key)
    }

    /**
     * Sets up a map
     *
     * @author
     *
     * This function loads the feature table, decides the basemap style, initial view point,
     * fetches all the polygons, maps id of a polygon with it's graphic
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
    /**
     * defines the UI behaviour
     *
     * setups callout behaviour, defines spinner to control the speed of the animation, set-up a
     * start-pause button, initializes progress.
     */
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

        // sets up progress bar
        progressSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                count = (progress * data.size) / 100
                curProgressTextView.text = "$progress %"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * implementation for pause-resume button
     *
     * @author Rohitesh
     */
    fun toggleAnimationTimer(view: View) {
        isTimerRunning = when {
            isTimerRunning -> {
                resumePauseButton.text = getString(R.string.resume)
                // set the isTimerRunning flag to false
                false
            }
            else -> {
                runSimulation()
                resumePauseButton.text = getString(R.string.pause)
                // set the isTimerRunning flag to true
                true
            }
        }
    }

    /**
     * runs simulation from start
     *
     * @author Bruno St.Aubin
     */
    fun runSimulation() {
        val handler = Handler()

        val runnable: Runnable = object : Runnable {
            override fun run() {
                drawStep(count)
                if (!isTimerRunning) return
                if (count++ < data.size) handler.postDelayed(this, delay)
            }
        }

        // trigger first time
        handler.post(runnable)
    }

    /**
     *  interates all ids in current time-stamp, and applies color to al polygons according to the
     *  message values
     *
     *  @author Bruno St.Aubin
     *
     *  @param[j] current time stamp
     */
    private fun drawStep(j: Int) {
        var i = j
        println("drawing step $i...")
        i = i % data.size
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