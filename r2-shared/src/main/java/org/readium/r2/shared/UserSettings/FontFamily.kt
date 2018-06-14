package org.readium.r2.shared.UserSettings



/*
Publisher("Publisher's default"),
Helvetica("sans-serif"),
Iowan("Roboto"),
Athelas("serif"),
Seravek("Seravek");
*/

class FontFamily(var value: String = "Publisher's default") : Enumeratable(FONT_FAMILY_REF, FONT_FAMILY_NAME, value) {

    override val values = listOf("Publisher's default",
            "sans-serif",
            "Roboto",
            "serif",
            "Seravek"
            // TODO add here your new polices
    )
    
    override fun toString() = value

}