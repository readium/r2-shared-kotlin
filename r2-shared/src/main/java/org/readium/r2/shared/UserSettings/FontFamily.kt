package org.readium.r2.shared.UserSettings

enum class FontFamilyCase(val value: String) : CharSequence by value {

    Publisher("Publisher's default"),
    Helvetica("sans-serif"),
    Iowan("Roboto"),
    Athelas("serif"),
    Seravek("Seravek");

    override fun toString() = value

}

class FontFamily(override var _value: Any = FontFamilyCase.Publisher.toString()) : UserSetting(FONT_FAMILY_REF, FONT_FAMILY_NAME, _value.toString()) {
    override fun toString() = (_value as FontFamilyCase).toString()

}