package com.dndweapons.tooltip

/**
 * Pure value type representing one tooltip line. The injector converts each line
 * to a Component via Component.translatable(translationKey, *args).
 *
 * args are passed straight to Component.translatable's varargs; supported types
 * are String, Int, Float (anything Component.translatable accepts as an arg).
 */
data class TooltipLine(
    val translationKey: String,
    val args: List<Any> = emptyList(),
)
