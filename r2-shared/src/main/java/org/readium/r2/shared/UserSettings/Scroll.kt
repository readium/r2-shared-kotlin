package org.readium.r2.shared.UserSettings

enum class ScrollCase(val value: String) : CharSequence by value {

    Off("readium-scroll-off"),
    On("readium-scroll-on");

    override fun toString() = value
}

class Scroll(value: Any = ScrollCase.Off.toString()) : Switchable(SCROLL_REF, SCROLL_NAME, value.toString()) {

    override val values: Map<Boolean, String> = mapOf(true to ScrollCase.On.toString(), false to ScrollCase.Off.toString())

}