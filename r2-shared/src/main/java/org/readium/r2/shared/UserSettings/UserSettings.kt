package org.readium.r2.shared.UserSettings

import android.content.SharedPreferences
import kotlin.reflect.KClass

const val FONT_SIZE_REF = "fontSize"
const val FONT_FAMILY_REF = "fontFamily"
const val FONT_OVERRIDE_REF = "fontOverride"
const val APPEARANCE_REF = "appearance"
const val SCROLL_REF = "scroll"
const val PUBLISHER_DEFAULT_REF = "advancedSettings"
const val TEXT_ALIGNMENT_REF = "textAlign"
const val COLUMN_COUNT_REF = "colCount"
const val WORD_SPACING_REF = "wordSpacing"
const val LETTER_SPACING_REF = "letterSpacing"
const val PAGE_MARGINS_REF = "pageMargins"

const val FONT_SIZE_NAME = "--USER__$FONT_SIZE_REF"
const val FONT_FAMILY_NAME = "--USER__$FONT_FAMILY_REF"
const val FONT_OVERRIDE_NAME = "--USER__$FONT_OVERRIDE_REF"
const val APPEARANCE_NAME = "--USER__$APPEARANCE_REF"
const val SCROLL_NAME = "--USER__$SCROLL_REF"
const val PUBLISHER_DEFAULT_NAME = "--USER__$PUBLISHER_DEFAULT_REF"
const val TEXT_ALIGNMENT_NAME = "--USER__$TEXT_ALIGNMENT_REF"
const val COLUMN_COUNT_NAME = "--USER__$COLUMN_COUNT_REF"
const val WORD_SPACING_NAME = "--USER__$WORD_SPACING_REF"
const val LETTER_SPACING_NAME = "--USER__$LETTER_SPACING_REF"
const val PAGE_MARGINS_NAME = "--USER__$PAGE_MARGINS_REF"

sealed class UserSetting(var ref: String, var name: String, private var value: String) {
    abstract override fun toString(): String
}

abstract class Enumeratable(ref: String, name: String, private var value: String) :
        UserSetting(ref, name, value) {

    abstract val values: List<String>

    fun set(index: Int) {
        this.value = values[index]
    }

    override fun toString() = value
}

abstract class Incrementable(private var nValue: Float, private val suffix: String, ref: String, name: String) :
        UserSetting(ref, name, nValue.toString() + suffix) {

    abstract val min: Float
    abstract val max: Float
    abstract val step: Float

    fun increment() {
        nValue +=  (if (nValue + step <= max) step else 0.0f)
    }

    fun decrement() {
        nValue -= (if (nValue - step >= min) step else 0.0f)
    }

    override fun toString() = nValue.toString() + suffix
}

abstract class Switchable(ref: String, name: String, value: String) :
        UserSetting(ref, name, value) {

    var on: Boolean = false
    abstract val values: Map<Boolean, String>

    fun switch() {
        on = !on
    }

    override fun toString() = values[on]!!
}

// TODO add here your new Subclasses of UserSetting. It has to be an abstract class inheriting from UserSetting.
class UserSettings(preferences: SharedPreferences) {

    var userSettings: MutableList<UserSetting> = mutableListOf()

    init {
        // TODO add here your new user settings
        // TODO add default values
        // Enumeratables
        userSettings.add(Appearance(preferences.getString(APPEARANCE_REF, "")))
        userSettings.add(ColumnCount(preferences.getString(COLUMN_COUNT_REF, "")))
        userSettings.add(FontFamily(preferences.getString(FONT_FAMILY_REF, "")))
        userSettings.add(TextAlignment(preferences.getString(TEXT_ALIGNMENT_REF, "")))

        // Switchables
        userSettings.add(FontOverride(preferences.getString(FONT_OVERRIDE_REF, FontOverrideCase.Off.toString())))
        userSettings.add(PublisherDefault(preferences.getString(PUBLISHER_DEFAULT_REF, PublisherDefaultCase.On.toString())))
        userSettings.add(Scroll(preferences.getString(SCROLL_REF, ScrollCase.Off.toString())))

        // Incrementables
        userSettings.add(FontSize(preferences.getFloat(FONT_SIZE_REF, 200.0f)))
        userSettings.add(LetterSpacing(preferences.getFloat(LETTER_SPACING_REF, 0.0f)))
        userSettings.add(PageMargins(preferences.getFloat(PAGE_MARGINS_REF, 0.0f)))
        userSettings.add(WordSpacing(preferences.getFloat(WORD_SPACING_REF, 0.0f)))
    }

    private fun getByRef(ref: String) = userSettings.filter {
        it.ref == ref
    }.firstOrNull()!!

    fun setUserSetting(ref: String, index:Int) {
        val userSetting = getByRef(ref)
        (userSetting as Enumeratable).set(index)
    }

    fun switch(ref: String) {
        val userSetting = getByRef(ref)
        (userSetting as Switchable).switch()
    }

    fun increment(ref: String) {
        val userSetting = getByRef(ref)
        (userSetting as Incrementable).increment()
    }

    fun decrement(ref: String) {
        val userSetting = getByRef(ref)
        (userSetting as Incrementable).decrement()
    }

    fun getUserSetting(ref: String) = getByRef(ref).toString()
}

