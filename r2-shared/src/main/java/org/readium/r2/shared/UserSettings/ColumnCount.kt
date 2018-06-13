package org.readium.r2.shared.UserSettings

enum class ColumnCountCase(val value: String)  : CharSequence by value{

    Auto("auto"),
    One("1"),
    Two("2");

    override fun toString() = value

}

class ColumnCount(override var _value: Any = ColumnCountCase.Auto.toString()) : UserSetting(COLUMN_COUNT_REF, COLUMN_COUNT_NAME, _value.toString()) {
    override fun toString() = (_value as ColumnCountCase).toString()

}