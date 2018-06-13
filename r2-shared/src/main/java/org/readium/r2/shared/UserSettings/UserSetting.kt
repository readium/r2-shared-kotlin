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


open class UserSetting(var ref: String, var name: String, var value: String)

class UserSettings() {

    var userSettings: MutableList<UserSetting> = mutableListOf()
/*
    var appearance: Appearance = Appearance()
    var columnCount: ColumnCount = ColumnCount()
    var fontFamily: FontFamily = FontFamily()
    var fontOverride: FontOverride = FontOverride()
    var fontSize: FontSize = FontSize()
    var letterSpacing: LetterSpacing = LetterSpacing()
    var pageMargins: PageMargins = PageMargins()
    var publisherDefault: PublisherDefault = PublisherDefault()
    var scroll: Scroll = Scroll()
    var textAlignment: TextAlignment = TextAlignment()
    var wordSpacing: WordSpacing = WordSpacing()
*/
    constructor(preferences: SharedPreferences) : this(){
        userSettings.add(Appearance(preferences.getString(APPEARANCE_REF, AppearanceCase.Default.toString())))
        userSettings.add(ColumnCount(preferences.getString(COLUMN_COUNT_REF, ColumnCountCase.Auto.toString())))
        userSettings.add(FontFamily(preferences.getString(FONT_FAMILY_REF, FontFamilyCase.Publisher.toString())))
        userSettings.add(FontOverride(preferences.getString(FONT_OVERRIDE_REF, FontOverrideCase.Off.toString())))
        userSettings.add(FontSize(preferences.getInt(FONT_SIZE_REF, 0)))
        userSettings.add(LetterSpacing(preferences.getFloat(LETTER_SPACING_REF, 0.0f)))
        userSettings.add(PageMargins(preferences.getFloat(PAGE_MARGINS_REF, 0.0f)))
        userSettings.add(PublisherDefault(preferences.getString(PUBLISHER_DEFAULT_REF, PublisherDefaultCase.On.toString())))
        userSettings.add(Scroll(preferences.getString(SCROLL_REF, ScrollCase.Off.toString())))
        userSettings.add(TextAlignment(preferences.getString(TEXT_ALIGNMENT_REF, TextAlignmentCase.Justify.toString())))
        userSettings.add(WordSpacing(preferences.getFloat(WORD_SPACING_REF, 0.0f)))
    }

    private inline fun <reified UserSettingType : UserSetting>getByType(type: KClass<UserSettingType>) = userSettings.filterIsInstance<UserSettingType>().firstOrNull()

}

