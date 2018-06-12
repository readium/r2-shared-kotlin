package org.readium.r2.shared.drm

import org.joda.time.DateTime
import org.json.JSONObject
import java.util.*

class Updated (json: JSONObject){

    var license: Date
    var status: Date

    init {
        try {
            license = DateTime(json.getString("license")).toDate()
            status = DateTime(json.getString("status")).toDate()
        } catch (e: Exception){
            throw Exception(DrmParsingError().errorDescription(DrmParsingErrors.updated))
        }
    }
}