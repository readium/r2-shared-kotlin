package org.readium.r2.shared.UserSettings

enum class PublisherDefaultCase(val value: String) : CharSequence by value {

    On("readium-advanced-off"),
    Off("readium-advanced-on");

    override fun toString() = value
}

class PublisherDefault(value: Any = PublisherDefaultCase.On.toString()) : Switchable(PUBLISHER_DEFAULT_REF, PUBLISHER_DEFAULT_NAME, value.toString()) {

    override val values: Map<Boolean, String> = mapOf(true to PublisherDefaultCase.On.toString(), false to PublisherDefaultCase.Off.toString())

}