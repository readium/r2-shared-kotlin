package org.readium.r2.shared.UserSettings

class WordSpacing(override var _value: Any = min) : UserSetting(WORD_SPACING_REF, WORD_SPACING_NAME, _value.toString()){

    companion object  {
        val max = 0.5f
        val min = 0.0f
        val step = 0.25f
        var floatValue = 0.0f
    }

    fun increment(){
        floatValue += (if (floatValue + step <= max) step else 0.0f)
    }

    fun decrement(){
        floatValue -= (if (floatValue - step >= min) step else 0.0f)
    }

    override fun toString() = this._value.toString() + "rem"
}