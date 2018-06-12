package org.readium.r2.shared.drm

enum class DrmParsingErrors {
    json,
    date,
    link,
    updated,
    updatedDate,
    encryption,
    signature
}

class DrmParsingError {

    fun errorDescription(error: DrmParsingErrors) = when(error){
        DrmParsingErrors.json -> "The JSON is no representing a valid Status Document."
        DrmParsingErrors.date -> "Invalid ISO8601 dates found."
        DrmParsingErrors.link -> "Invalid Link found in the JSON."
        DrmParsingErrors.encryption -> "Invalid Encryption object."
        DrmParsingErrors.signature -> "Invalid License Document Signature."
        DrmParsingErrors.updated -> "Invalid Updated object."
        DrmParsingErrors.updatedDate -> "Invalid Updated object date."
    }
}