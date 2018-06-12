package org.readium.r2.shared.drm

import java.io.Serializable

enum class DrmErrorCase {
    unknown,
    invalidPath,
    invalidLcpl,
    statusLinkNotFound,
    licenseNotFound,
    licenseLinkNotFound,
    publicationLinkNotFound,
    hintLinkNotFound,
    registerLinkNotFound,
    LinkNotFound,
    renewLinkNotFound,
    noStatusDocument,
    licenseDocumentData,
    publicationData,
    registrationFailure,
    Failure,
    alreadyed,
    alreadyExpired,
    renewFailure,
    renewPeriod,
    deviceId,
    unexpectedServerError,
    invalidHintData,
    archive,
    fileNotInArchive,
    noPassphraseFound,
    emptyPassphrase,
    invalidJson,
    invalidContext,
    crlFetching,
    missingLicenseStatus,
    licenseStatusCancelled,
    licenseStatusReturned,
    licenseStatusRevoked,
    licenseStatusExpired,
    invalidRights,
    invalidPassphrase
}

class DrmError {

    fun errorDescription(drmError: DrmErrorCase) = when(drmError){
        DrmErrorCase.unknown -> "Unknown error"
        DrmErrorCase.invalidPath -> "The provided license file path is incorrect."
        DrmErrorCase.invalidLcpl -> "The provided license isn't a correctly formatted LCPL file. "
        DrmErrorCase.licenseNotFound -> "No license found in base for the given identifier."
        DrmErrorCase.statusLinkNotFound -> "The status link is missing from the license document."
        DrmErrorCase.licenseLinkNotFound -> "The license link is missing from the status document."
        DrmErrorCase.publicationLinkNotFound -> "The publication link is missing from the license document."
        DrmErrorCase.hintLinkNotFound -> "The hint link is missing from the license document."
        DrmErrorCase.registerLinkNotFound -> "The register link is missing from the status document."
        DrmErrorCase.LinkNotFound -> "The  link is missing from the status document."
        DrmErrorCase.renewLinkNotFound -> "The renew link is missing from the status document."
        DrmErrorCase.noStatusDocument -> "Updating the license failed, there is no status document."
        DrmErrorCase.licenseDocumentData -> "Updating license failed, the fetche data is invalid."
        DrmErrorCase.publicationData -> "The publication data is invalid."
        DrmErrorCase.missingLicenseStatus -> "The license status couldn't be defined."
        DrmErrorCase.licenseStatusReturned -> "This license has been ed."
        DrmErrorCase.licenseStatusRevoked -> "This license has been revoked by its PROVIDER."
        DrmErrorCase.licenseStatusCancelled -> "You have cancelled this license."
        DrmErrorCase.licenseStatusExpired -> "The license status is expired, if your PROVIDER allow it, you may be able to renew it."
        DrmErrorCase.invalidRights -> "The rights of this license aren't valid."
        DrmErrorCase.registrationFailure -> "The device could not be registered properly."
        DrmErrorCase.Failure -> "Your publication could not be ed properly."
        DrmErrorCase.alreadyed -> "Your publication has already been ed before."
        DrmErrorCase.alreadyExpired -> "Your publication has already expired."
        DrmErrorCase.renewFailure -> "Your publication could not be renewed properly."
        DrmErrorCase.deviceId -> "Couldn't retrieve/generate a proper deviceId."
        DrmErrorCase.unexpectedServerError -> "An unexpected error has occured."
        DrmErrorCase.invalidHintData -> "The data ed by the server for the hint is not valid."
        DrmErrorCase.archive -> "Coudn't instantiate the archive object."
        DrmErrorCase.fileNotInArchive -> "The file you requested couldn't be found in the archive."
        DrmErrorCase.noPassphraseFound -> "Couldn't find a valide passphrase in the database, please provide a passphrase."
        DrmErrorCase.emptyPassphrase -> "The passphrase provided is empty."
        DrmErrorCase.invalidJson -> "The JSON license is not valid."
        DrmErrorCase.invalidContext -> "The context provided is invalid."
        DrmErrorCase.crlFetching -> "Error while fetching the certificate revocation list."
        DrmErrorCase.invalidPassphrase -> "The passphrase entered is not valid."
        DrmErrorCase.renewPeriod -> "Incorrect renewal period, your publication could not be renewed."
    }

}