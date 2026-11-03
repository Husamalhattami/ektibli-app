package com.example

import com.example.data.ContrastTheme
import com.example.data.TranscriptionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun transcriptionRecord_creation() {
        val record = TranscriptionRecord(
            title = "محادثة",
            speaker = "طبيب",
            content = "يرجى تناول الدواء بانتظام",
            type = "LIVE"
        )
        assertNotNull(record)
        assertEquals("محادثة", record.title)
        assertEquals("طبيب", record.speaker)
        assertEquals("يرجى تناول الدواء بانتظام", record.content)
    }

    @Test
    fun contrastTheme_values() {
        assertEquals("YELLOW_ON_BLACK", ContrastTheme.YELLOW_ON_BLACK.id)
        assertEquals("DARK_OLED", ContrastTheme.DARK_OLED.id)
        assertEquals("LIGHT_HIGH_CONTRAST", ContrastTheme.LIGHT_HIGH_CONTRAST.id)
    }
}
