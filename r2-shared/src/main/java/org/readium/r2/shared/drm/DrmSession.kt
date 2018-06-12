package org.readium.r2.shared.drm

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import java.io.Serializable
import java.net.URL
import java.util.*

interface DrmSession:Serializable {
    fun resolve(passphrase: String, pemCrl: String) : Promise<DrmLicense, Exception>

    fun getLcpContext(jsonLicense: String, passphrase: String, pemCrl: String) : Promise<DrmLicense, Exception>
    fun getHint() : String
    fun getProfile() : String
    fun checkPassphrases(passphrases: List<String>) : String
    fun passphraseFromDb() : String?
    fun storePassphrase(passphraseHash: String)
    fun validateLicense() :Promise<Unit, Exception>
}