package org.readium.r2.shared.drm

import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class Event(val json: JSONObject) {

    var type: String
    var name: String
    var id: String
    var date: Date

    init {
        try {
            name = json.getString("name")
            date = DateTime(json.getString("timestamp")).toDate()
            type = json.getString("type")
            id = json.getString("id")
        } catch (e: Exception) {
            throw Exception(DrmParsingError().errorDescription(DrmParsingErrors.json))
        }
    }

}
fun parseEvents(json: JSONArray) : List<Event> {
//    val jsonEvents = json.getJSONArray(key)
    val events = mutableListOf<Event>()
    for (i in 0..json.length() - 1) {
        val event = Event(JSONObject(json[i].toString()))
        events.add(event)
    }
    return events
}
