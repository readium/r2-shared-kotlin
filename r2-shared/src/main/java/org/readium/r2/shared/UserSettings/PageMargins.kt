package org.readium.r2.shared.UserSettings

class PageMargins(var _value: Float = min) : UserSetting(PAGE_MARGINS_REF, PAGE_MARGINS_NAME, _value.toString()){

    companion object {
        val max = 2.0f
        val min = 0.5f
        val step = 0.25f
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

}