package org.readium.r2.shared.UserSettings

class WordSpacing(var _value: Float = min) : UserSetting(WORD_SPACING_REF, WORD_SPACING_NAME, _value.toString()){

    companion object  {
        val max = 0.5f
        val min = 0.0f
        val step = 0.25f
    }

    fun increment(){
        this._value -= (if (this._value + step <= max) step else 0.0f)
    }

    fun decrement(){
        this._value -= (if (this._value - step >= min) step else 0.0f)
    }

    override fun toString() : String {
        return this._value.toString() + "rem"
    }

}