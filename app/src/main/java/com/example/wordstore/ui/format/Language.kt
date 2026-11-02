package com.example.wordstore.ui.format

import java.util.Locale

/** The language's name in its own language, for example "Türkçe" for "tr". */
fun languageName(code: String): String {
    val locale = Locale(code)
    val name = locale.getDisplayLanguage(locale)
    if (name.isBlank() || name == code) return code.uppercase(Locale.ROOT)
    return name.replaceFirstChar { it.titlecase(locale) }
}
