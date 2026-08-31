package com.affonso.pedaltrack.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KmInputTest {

    @Test
    fun `formatKmDigits shifts digits in from the right with one decimal place`() {
        assertEquals("", formatKmDigits(""))
        assertEquals("0,1", formatKmDigits("1"))
        assertEquals("1,2", formatKmDigits("12"))
        assertEquals("12,5", formatKmDigits("125"))
        assertEquals("0,0", formatKmDigits("0"))
    }

    @Test
    fun `kmDigitsToDouble parses the digit sequence as tenths`() {
        assertEquals(12.5, kmDigitsToDouble("125")!!, 0.001)
        assertEquals(0.1, kmDigitsToDouble("1")!!, 0.001)
        assertNull(kmDigitsToDouble(""))
    }

    @Test
    fun `kmToDigits and kmDigitsToDouble round-trip a km value`() {
        val original = 12.5
        val digits = kmToDigits(original)
        assertEquals(original, kmDigitsToDouble(digits)!!, 0.001)
    }

    @Test
    fun `applyDigitEdit appends newly typed digits to the end`() {
        // Typing at the end, the normal case: "12" + "5" -> "125"
        assertEquals("125", applyDigitEdit("12", "125"))
        // Pasting/typing multiple digits at once still appends all of them to the end.
        assertEquals("12345", applyDigitEdit("12", "12345"))
    }

    @Test
    fun `applyDigitEdit always removes from the end, regardless of raw cursor position`() {
        // Normal backspace at the end: "125" -> "12"
        assertEquals("12", applyDigitEdit("125", "12"))
        // Backspace as if it happened in the middle of the displayed text (raw diff removed
        // the middle digit "2" from "125" leaving "15") must still drop from the *end* of our
        // own model, i.e. "12", not keep the positionally-edited "15".
        assertEquals("12", applyDigitEdit("125", "15"))
    }

    @Test
    fun `applyDigitEdit clears everything on select-all delete`() {
        assertEquals("", applyDigitEdit("125", ""))
    }

    @Test
    fun `applyDigitEdit does not duplicate digits because of the display's padded leading zero`() {
        // "" -> type 3 -> displayed "0,3", internal state is just "3"
        val afterFirst = applyDigitEdit("", "3")
        assertEquals("3", afterFirst)
        // "3" (displayed "0,3") -> type 3 again -> raw field text is "0,33"
        val afterSecond = applyDigitEdit(afterFirst, "033")
        assertEquals("33", afterSecond)
        // "33" (displayed "3,3") -> type 3 again -> raw field text is "3,33"
        val afterThird = applyDigitEdit(afterSecond, "333")
        assertEquals("333", afterThird)
    }

    @Test
    fun `applyDigitEdit backspace can fully clear a single-digit value`() {
        // "3" displays padded as "0,3"; one backspace should clear the digit entirely,
        // not get stuck because the raw digit count coincidentally matched current's length.
        assertEquals("", applyDigitEdit("3", "0"))
    }

    @Test
    fun `applyDigitEdit respects the max length`() {
        assertEquals("123456", applyDigitEdit("12345", "123456789", maxLength = 6))
    }
}
