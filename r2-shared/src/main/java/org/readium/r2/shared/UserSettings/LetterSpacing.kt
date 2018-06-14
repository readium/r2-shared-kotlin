package org.readium.r2.shared.UserSettings

class LetterSpacing(value: Float = 0.0f) : Incrementable(value, "em", LETTER_SPACING_REF, LETTER_SPACING_NAME){

    override val min = 0.0f
    override val max = 0.5f
    override val step = 0.0625f

}