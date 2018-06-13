package org.readium.r2.shared.UserSettings

enum class PublisherDefaultCase(val value: String) : CharSequence by value {

    On("readium-advanced-off"),
    Off("readium-advanced-on");

    override fun toString() = value
}

class PublisherDefault(var _value: String = PublisherDefaultCase.On.toString()) : UserSetting(PUBLISHER_DEFAULT_REF, PUBLISHER_DEFAULT_NAME, _value)