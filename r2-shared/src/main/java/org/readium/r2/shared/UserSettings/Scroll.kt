package org.readium.r2.shared.UserSettings

enum class ScrollCase(val value: String) : CharSequence by value {

    Off("readium-scroll-off"),
    On("readium-scroll-on");

    override fun toString() = value
}

class Scroll(override var _value: Any = ScrollCase.Off.toString()) : UserSetting(SCROLL_REF, SCROLL_NAME, _value.toString()) {
    override fun toString() = _value.toString()
}