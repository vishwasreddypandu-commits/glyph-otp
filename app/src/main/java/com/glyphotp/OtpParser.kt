package com.glyphotp

object OtpParser {
    private val keywords = Regex("(?i)\\b(otp|code|passcode|password|pin|verification|verify|one[- ]time|security|authenticate|login|sign[- ]?in)\\b")
    private val codeRegex = Regex("(?<![\\d])(\\d{3}[- ]?\\d{3}|\\d{4,8})(?![\\d])")

    /** Returns a 4-6 digit code, or null. Needs a keyword in the message so random numbers are ignored. */
    fun extract(text: String): String? {
        if (!keywords.containsMatchIn(text)) return null
        for (m in codeRegex.findAll(text)) {
            val digits = m.value.filter { it.isDigit() }
            if (digits.length in 4..6) return digits
        }
        return null
    }
}
