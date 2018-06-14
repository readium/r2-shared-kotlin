package org.readium.r2.shared.UserSettings

/*
    Justify("justify"),
    Left("start");
*/

class TextAlignment(var value: String) : Enumeratable(TEXT_ALIGNMENT_REF, TEXT_ALIGNMENT_NAME, value) {

    override val values = listOf("justify", "start") // TODO add your new text alignment logic here)

}