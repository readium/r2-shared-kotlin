package org.readium.r2.shared.UserSettings

class FontSize(var _value: Int = min) : UserSetting(FONT_SIZE_REF, FONT_SIZE_NAME, _value.toString()){

    companion object {
        val max = 300
        val min = 100
        val step = 25
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
        return this._value.toString() + "%"
    }

}