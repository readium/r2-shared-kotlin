package org.readium.r2.shared.drm

import org.json.JSONObject
import java.net.URL

class Encryption(json: JSONObject) {
    var profile: URL
    var contentKey: ContentKey
    var userKey: UserKey

    init {
        try {
            profile = URL(json.getString("profile"))
        } catch (e: Exception){
            throw Exception(DrmParsingError().errorDescription(DrmParsingErrors.encryption))
        }
        contentKey = ContentKey(json.getJSONObject("content_key"))
        userKey = UserKey(json.getJSONObject("user_key"))
    }
}