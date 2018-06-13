package org.readium.r2.shared.UserSettings

enum class AppearanceCase(val value: String) : CharSequence by value {
    Default("readium-default-on"),
    Sepia("readium-sepia-on"),
    Night("readium-night-on");

    override fun toString() = this.value
}

class Appearance(override var _value: Any = AppearanceCase.Default.toString()) : UserSetting(APPEARANCE_REF, APPEARANCE_NAME, _value.toString()) {
    override fun toString() = (_value as AppearanceCase).toString()
}