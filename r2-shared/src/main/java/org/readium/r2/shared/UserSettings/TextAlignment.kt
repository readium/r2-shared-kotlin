package org.readium.r2.shared.UserSettings

enum class TextAlignmentCase(val value : String) : CharSequence by value {

    Justify("justify"),
    Left("start");

    override fun toString() = value

}

class TextAlignment(override var _value: Any = TextAlignmentCase.Justify.toString()) : UserSetting(TEXT_ALIGNMENT_REF, TEXT_ALIGNMENT_NAME, _value.toString()) {
    override fun toString() = _value.toString()
}