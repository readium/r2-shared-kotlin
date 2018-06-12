package org.readium.r2.shared.drm

import android.content.Context
import nl.komponents.kovenant.Promise
import java.io.Serializable

interface DrmHttpService : Serializable {
    fun statusDocument(url: String): Promise<StatusDocument, Exception>
    fun fetchUpdatedLicense(url: String): Promise<LicenseDocument, Exception>

    fun publicationUrl(context: Context, url: String, parameters: List<Pair<String, Any?>>? = null): Promise<String, Exception>
    fun certificateRevocationList(url: String): Promise<String, Exception>
    fun register(registerUrl: String, params: List<Pair<String, Any?>>): Promise<String?, Exception>
    fun renewLicense(url: String): Promise<String?, Exception>
    fun returnLicense(url: String): Promise<String?, Exception>
}