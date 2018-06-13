package org.readium.r2.shared.UserSettings

class FontSize(override var _value: Any = min) : UserSetting(FONT_SIZE_REF, FONT_SIZE_NAME, _value.toString()){

    companion object {
        val max = 300
        val min = 100
        val step = 25
        var intValue = 0
    }

    init {
        intValue = _value as Int
    }
    fun increment(){
        intValue += (if (intValue + step <= max) step else 0)
    }

    fun decrement(){
        intValue -= (if (intValue - step >= min) step else 0)
    }

    override fun toString() : String {
        return intValue.toString() + "%"
    }

}