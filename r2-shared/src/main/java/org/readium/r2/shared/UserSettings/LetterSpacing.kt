package org.readium.r2.shared.UserSettings

class LetterSpacing(var _value: Float = min) : UserSetting(FONT_SIZE_REF, FONT_SIZE_NAME, _value.toString()){

    companion object {
        val max = 0.5f
        val min = 0.0f
        val step = 0.0625f
    }

    fun increment(){
        if (this._value + step <= max){
            this._value += step
        }
    }

    fun decrement(){
        if (this._value - step >= min){
            this._value -= step
        }
    }

    override fun toString() : String {
        return this._value.toString() + "em"
    }

}