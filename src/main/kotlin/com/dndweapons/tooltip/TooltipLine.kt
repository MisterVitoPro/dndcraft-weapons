package com.dndweapons.tooltip

/**
 * Pure value type representing one tooltip line. The injector converts each line
 * to a Component via Component.translatable(translationKey, *args).
 *
 * args are passed straight to Component.translatable's varargs; supported types
 * are String, Int, Float (anything Component.translatable accepts as an arg).
 *
 * P1-003: Constructor validates translation keys to prevent injection attacks.
 */
data class TooltipLine(
    val translationKey: String,
    val args: List<Any> = emptyList(),
) {
    init {
        validateTranslationKey(translationKey)
    }

    companion object {
        // P1-003: Translation keys must match this safe pattern: lowercase letters, digits, dots, underscores, colons.
        // Pattern: word characters and dots only, with optional :suffix for parameterized keys.
        private val SAFE_TRANSLATION_KEY_PATTERN = Regex("^[a-z0-9._:]+$")

        private fun validateTranslationKey(key: String) {
            require(key.isNotEmpty()) { "Translation key must not be empty" }
            require(SAFE_TRANSLATION_KEY_PATTERN.matches(key)) {
                "Translation key '$key' contains invalid characters. Only lowercase letters, digits, dots, underscores, and colons are allowed."
            }
            // P1-003: Prevent path traversal attempts
            require(!key.contains("..") && !key.contains("//") && !key.contains("\\")) {
                "Translation key '$key' contains path traversal patterns (.., //, or backslashes)"
            }
        }
    }
}

