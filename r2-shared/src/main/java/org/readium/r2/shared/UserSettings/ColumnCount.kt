package org.readium.r2.shared.UserSettings

class ColumnCount(value: String) : Enumeratable(COLUMN_COUNT_REF, COLUMN_COUNT_NAME, value) {

    override val values = listOf("auto", "1", "2")

}