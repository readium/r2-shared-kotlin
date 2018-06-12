package org.readium.r2.shared.drm

import org.json.JSONObject
import java.net.URL

class ContentKey(json: JSONObject) {

    val encryptedValue: String
    var algorithm: URL

    init {
        try {
            encryptedValue = json.getString("encrypted_value")
            algorithm = URL(json.getString("algorithm"))
        } catch (e: Exception) {
            throw Exception(DrmParsingError().errorDescription(DrmParsingErrors.json))
        }
    }

}