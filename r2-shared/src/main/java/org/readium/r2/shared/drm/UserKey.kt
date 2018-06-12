package org.readium.r2.shared.drm

import org.json.JSONObject
import java.net.URL

class UserKey (json: JSONObject){

    var hint: String
    var algorithm: URL
    var keyCheck: String

    init {
        try {
            hint = json.getString("text_hint")
            algorithm = URL(json.getString("algorithm"))
            keyCheck = json.getString("key_check")
        } catch (e: Exception) {
            throw Exception(DrmParsingError().errorDescription(DrmParsingErrors.json))
        }
    }
}