package org.readium.r2.shared.UserSettings

class WordSpacing(value: Float = 0.0f) : Incrementable(value, "", WORD_SPACING_REF, WORD_SPACING_NAME){

    override val max = 0.5f
    override val min = 0.0f
    override val step = 0.25f

}