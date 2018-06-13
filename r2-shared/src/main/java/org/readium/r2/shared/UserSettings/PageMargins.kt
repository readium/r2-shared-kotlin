package org.readium.r2.shared.UserSettings

class PageMargins(override var _value: Any = min) : UserSetting(PAGE_MARGINS_REF, PAGE_MARGINS_NAME, _value.toString()){

    companion object {
        val max = 2.0f
        val min = 0.5f
        val step = 0.25f
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
        return floatValue.toString()
    }
}