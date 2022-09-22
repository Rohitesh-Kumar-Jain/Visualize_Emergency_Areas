package com.example.app

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