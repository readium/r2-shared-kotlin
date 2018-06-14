package org.readium.r2.shared.UserSettings

class Appearance(var value: String) : Enumeratable(APPEARANCE_REF, APPEARANCE_NAME, value) {

    override val values = listOf("readium-default-on", "readium-sepia-on", "readium-night-on")

}