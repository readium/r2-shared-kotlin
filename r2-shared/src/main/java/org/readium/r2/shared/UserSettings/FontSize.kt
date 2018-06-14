package org.readium.r2.shared.UserSettings

class FontSize(value: Float = 100.0f) : Incrementable(value, "", FONT_SIZE_REF, FONT_SIZE_NAME){

    override val min = 100.0f
    override val max = 300.0f
    override val step = 25.0f

}