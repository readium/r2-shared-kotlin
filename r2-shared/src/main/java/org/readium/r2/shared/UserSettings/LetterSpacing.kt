package org.readium.r2.shared.UserSettings

class LetterSpacing(override var _value: Any = min) : UserSetting(LETTER_SPACING_REF, LETTER_SPACING_NAME, _value.toString()){

    companion object {
        val max = 0.5f
        val min = 0.0f
        val step = 0.0625f
        var floatValue = 0.0f
    }

    init {
        floatValue = this._value as Float
    }

    fun increment(){
        floatValue += (if (floatValue + step <= max) step else 0.0f)
    }

    fun decrement(){
        floatValue -= (if (floatValue - step >= min) step else 0.0f)
    }

    override fun toString() : String {
        return floatValue.toString() + "em"
    }

}