package org.readium.r2.shared

sealed class UserSetting(var ref: String, var name: String) {

    private var value: String = ""
    get() = this.toString()
    abstract override fun toString(): String

}

// TODO add here your new Subclasses of UserSetting. It has to be an abstract class inheriting from UserSetting.

class Enumeratable(private var value: String, private val values: List<String>, ref: String, name: String) :
        UserSetting(ref, name) {

    fun set(index: Int) {
        this.value = values[index]
    }

    override fun toString() = value
}

class Incrementable(private var nValue: Float,
                    val min: Float,
                    val max: Float,
                    val step: Float,
                    private val suffix: String,
                    ref: String,
                    name: String) :
        UserSetting(ref, name) {

    fun increment() {
        nValue +=  (if (nValue + step <= max) step else 0.0f)
    }

    fun decrement() {
        nValue -= (if (nValue - step >= min) step else 0.0f)
    }

    override fun toString() = nValue.toString() + suffix
}

class Switchable(onValue: String, offValue: String, var on: Boolean,
        ref: String, name: String) :
        UserSetting(ref, name) {

    private val values = mapOf(true to onValue, false to offValue)

    fun switch() {
        on = !on
    }

    override fun toString() = values[on]!!
}

class UserSettings {

    val userSettings: MutableList<UserSetting> = mutableListOf()

    fun addIncrementable(value: Float, min: Float, max: Float, step: Float, suffix: String, ref: String, name: String) {
        userSettings.add(Incrementable(value, min, max, step, suffix, ref, name))
    }

    fun addSwitchable(onValue: String, offValue: String, on: Boolean, ref: String, name: String) {
        userSettings.add(Switchable(onValue, offValue, on, ref, name))
    }

    fun addEnumeratable(value: String, values: List<String>, ref: String, name: String) {
        userSettings.add(Enumeratable(value, values, ref, name))
    }

    private fun getByRef(ref: String) = userSettings.filter {
        it.ref == ref
    }.firstOrNull()!!


    fun getUserSetting(ref: String) = getByRef(ref).toString()
}

