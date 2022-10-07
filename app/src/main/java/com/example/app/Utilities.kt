package com.example.app

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.data.Feature
import com.esri.arcgisruntime.geometry.Envelope
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.view.Callout
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.SimpleFillSymbol
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import org.json.JSONObject
import org.json.JSONTokener
import java.io.InputStream


/**
 * reads strucutre JSON file and retrieves the components array from the file
 */
fun getComponentsJSONArray(structureJSON : InputStream) : MutableList<Components> {
    val jsonString: String = structureJSON.bufferedReader().use { it.readText() }
    val componentsList = mutableListOf<Components>()

    val jsonObject = JSONTokener(jsonString).nextValue() as JSONObject
    val componentsArray = jsonObject.getJSONArray("components")

    for (i in 0 until componentsArray.length()) {
        componentsList.add(Components(componentsArray.getJSONObject(i).getString("id"), componentsArray.getJSONObject(i).getInt("model_type")))
    }

    return componentsList
}

/**
 * reads and stores the log file data according to the time stamps
 *
 * @param fileName  location of the log file to be parsed
 */
fun getTimeStampedDataFromLogFile(messagesLog : InputStream, structureJSON : InputStream) : MutableList<MutableList<LogFileData>> {
    val timeStampedData = mutableListOf<MutableList<LogFileData>>()
    val currentTimeStamp = mutableListOf<LogFileData>()

    val componentsList = getComponentsJSONArray(structureJSON)
    // convert list of components

    messagesLog.bufferedReader().forEachLine {
        val list = it.split(";")

        // if it's a time stamp, the list will have only one element
        if (list.size == 1) {
            addClonedCurrentTimeStampToTimeStampedData(timeStampedData, currentTimeStamp)

        } else if (!list.get(1).contains(",")) {
            val message_emitter = Integer.parseInt(list.get(0))
            val message_data = Integer.parseInt(list.get(1))
            val logFileData = LogFileData(message_data, componentsList.get(message_emitter))
            currentTimeStamp.add(logFileData)
        }
    }

    addClonedCurrentTimeStampToTimeStampedData(timeStampedData, currentTimeStamp)
    return timeStampedData
}

// two parallel structures, one to hold models, and one to hold

/**
 * helper function for {@link #readFileLineByLineUsingForEachLine
 *
 * @param timeStampedData
 * @param currentTimeStamp
 */
fun addClonedCurrentTimeStampToTimeStampedData(
    timeStampedData: MutableList<MutableList<LogFileData>>, currentTimeStamp: MutableList<LogFileData>
) {
    val clone = mutableListOf<LogFileData>()
    for (timeStamp in currentTimeStamp) {
        clone.add(timeStamp)
    }
    timeStampedData.add(clone)
    currentTimeStamp.clear()
}

fun getSimpleFillSymbol(message_data : Int) : SimpleFillSymbol {
    val lineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f)
    val firstFillColor = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(255,245,240), lineSymbol)
    val secondFillColor = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(254,224,210), lineSymbol)
    val thirdFillColor = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(252,187,161), lineSymbol)
    val fourthFillColor = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(252,146,114), lineSymbol)
    val fifthFillColor = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(251,106,74), lineSymbol)
    val sixthFillColor = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(239,59,44), lineSymbol)
    val seventhFillColor = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(203,24,29), lineSymbol)
    val eighthFillColor = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(165,15,21), lineSymbol)
    val ninthFillColor = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(103,0,13), lineSymbol)

    if (message_data in 0..1) return firstFillColor
    else if (message_data in 2..4) return secondFillColor
    else if (message_data in 4..6) return thirdFillColor
    else if (message_data in 6..10) return fourthFillColor
    else if (message_data in 11..15) return fifthFillColor
    else if (message_data in 16.. 25) return sixthFillColor
    else if (message_data in 26 .. 35) return seventhFillColor
    else if (message_data in 36 .. 50) return eighthFillColor
    else return ninthFillColor
}

fun callOutPolgyon(screenPoint : Point, mapView : MapView, areasLayer : FeatureLayer, mCallout : Callout, applicationContext : Context) {
    val identifyLayerResultListenableFuture: ListenableFuture<IdentifyLayerResult> = mapView
        .identifyLayerAsync(areasLayer, screenPoint, 10.0, false, 1)

    identifyLayerResultListenableFuture.addDoneListener {
        try {
            val identifyLayerResult =
                identifyLayerResultListenableFuture.get()
            val calloutContent = TextView(applicationContext)
            calloutContent.setTextColor(Color.BLACK)
            calloutContent.isSingleLine = false
            calloutContent.isVerticalScrollBarEnabled = true
            calloutContent.scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            calloutContent.movementMethod = ScrollingMovementMethod()
            calloutContent.setLines(6)
            for (element in identifyLayerResult.elements) {
                val feature =
                    element as Feature

                val attr =
                    feature.attributes
                val keys: Set<String> = attr.keys
                for (key in keys) {
                    var value = attr[key]
                    calloutContent.append("$key | $value\n")
                }

                val envelope: Envelope = feature.geometry.extent
                // uncomment this line, if you want to focus on the touched polygon
//                    mapView.setViewpointGeometryAsync(envelope, 200.0)

                mCallout.location = envelope.center
                mCallout.content = calloutContent
                mCallout.show()
            }
        } catch (e1: Exception) {
            Log.e(MainActivity.TAG, "Select feature failed: " + e1.message)
        }
    }
}