package org.readium.r2.shared.drm

import org.joda.time.DateTime
import org.json.JSONObject
import java.util.*

class PotentialRights(json: JSONObject) {
    var end: Date?

    init {
        end = DateTime(json.getString("end")).toDate()
    }
}