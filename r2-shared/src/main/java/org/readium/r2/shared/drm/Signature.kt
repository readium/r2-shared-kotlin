package org.readium.r2.shared.drm

import org.json.JSONObject
import java.net.URL

class Signature (json: JSONObject) {

    var algorithm: URL
    var certificate: String
    var value: String

    init {
        try {
            algorithm = URL(json.getString("algorithm"))
            certificate = json.getString("certificate")
            value = json.getString("value")
        } catch (e: Exception){
            throw Exception(DrmParsingError().errorDescription(DrmParsingErrors.signature))
        }
    }

}