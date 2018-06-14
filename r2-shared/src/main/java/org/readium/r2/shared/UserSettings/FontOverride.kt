package org.readium.r2.shared.UserSettings

enum class FontOverrideCase(val value: String) : CharSequence by value {

    On("readium-font-on"),
    Off("readium-font-off");

    override fun toString() = value
}

class FontOverride(var _value: Any = FontOverrideCase.Off.toString()) : Switchable(FONT_OVERRIDE_REF, FONT_OVERRIDE_NAME, _value.toString()) {

    override var values: Map<Boolean, String> = mapOf(true to FontOverrideCase.On.toString(), false to FontOverrideCase.Off.toString())

}