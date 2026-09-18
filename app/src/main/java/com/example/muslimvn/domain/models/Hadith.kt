package com.example.muslimvn.domain.models

/** A presentation-safe HadeethEnc item. The UI never consumes API DTOs directly. */
data class Hadith(
    val id: String,
    val title: String,
    val text: String,
    val attribution: String?,
    val grade: String?,
    val explanation: String?,
    val reference: String?,
    val category: String?,
    val language: String = "vi"
)

/**
 * Removes curly braces `{}` and normalizes resulting spaces in Hadith strings
 * to ensure natural Vietnamese sentence formatting.
 */
fun String.cleanCurlyBraces(): String {
    if (isEmpty()) return this
    return this
        .replace("{", "")
        .replace("}", "")
        .lines()
        .joinToString("\n") { line ->
            line.replace(Regex("[ \\t]+"), " ").trim()
        }
        .trim()
}
