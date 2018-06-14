package org.readium.r2.shared.UserSettings

class PageMargins(value: Float) : Incrementable(value, "", PAGE_MARGINS_REF, PAGE_MARGINS_NAME){

    override val max = 2.0f
    override val min = 0.5f
    override val step = 0.25f

}