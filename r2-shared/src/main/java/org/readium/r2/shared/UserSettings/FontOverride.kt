package org.readium.r2.shared.UserSettings

enum class FontOverrideCase(val value: String) : CharSequence by value {

    On("readium-font-on"),
    Off("readium-font-off");

    override fun toString() = value
}

class FontOverride(var _value: String = FontOverrideCase.Off.toString()) : UserSetting(FONT_OVERRIDE_REF, FONT_OVERRIDE_NAME, _value)