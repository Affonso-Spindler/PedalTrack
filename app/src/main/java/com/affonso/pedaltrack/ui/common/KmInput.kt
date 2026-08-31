package com.affonso.pedaltrack.ui.common

import kotlin.math.roundToInt

/**
 * Digit-only km entry, like a price field: each typed digit shifts in from the right,
 * the last digit is always the tenths place. "125" -> "12,5".
 */
fun formatKmDigits(digits: String): String {
    if (digits.isEmpty()) return ""
    val padded = digits.padStart(2, '0')
    val whole = padded.dropLast(1).trimStart('0').ifEmpty { "0" }
    val decimal = padded.last()
    return "$whole,$decimal"
}

/** Parses the digit-only representation back into a km value, or null if there's nothing entered. */
fun kmDigitsToDouble(digits: String): Double? = digits.toIntOrNull()?.let { it / 10.0 }

/** Converts an existing km value into the digit-only representation, for pre-filling an edit field. */
fun kmToDigits(km: Double): String = (km * 10).roundToInt().coerceAtLeast(0).toString()

/**
 * Applies a raw text-field edit to [current] as if the field only ever edits from the right —
 * regardless of where the cursor was tapped. Typing always appends to the end; deleting always
 * removes from the end, ignoring wherever the raw edit's cursor position actually was.
 *
 * The raw field text is rendered from `formatKmDigits(current)`, which left-pads with a zero
 * once there's a whole-number place (e.g. "3" displays as "0,3", i.e. digits "03") — so the
 * edit's digit count must be compared against that padded/displayed length, not [current]'s
 * raw length, or a single edit gets miscounted as multiple.
 */
fun applyDigitEdit(current: String, rawNewValue: String, maxLength: Int = 6): String {
    val displayDigits = if (current.isEmpty()) "" else current.padStart(2, '0')
    val newDigits = rawNewValue.filter { it.isDigit() }
    if (newDigits.length > displayDigits.length) {
        val addedCount = newDigits.length - displayDigits.length
        return (current + newDigits.takeLast(addedCount)).take(maxLength)
    }
    val removedCount = displayDigits.length - newDigits.length
    return current.dropLast(removedCount)
}
