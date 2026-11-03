package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ContrastTheme
import com.example.data.PreferencesManager
import com.example.data.TranscriptionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("اكتب لي", appName)
  }

  @Test
  fun `preferences manager updates font size and theme`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsManager = PreferencesManager(context)

    prefsManager.setFontSize(40)
    assertEquals(40, prefsManager.preferences.value.fontSizeSp)

    prefsManager.setTheme(ContrastTheme.DARK_OLED)
    assertEquals(ContrastTheme.DARK_OLED, prefsManager.preferences.value.theme)

    prefsManager.setVibrationEnabled(true)
    assertTrue(prefsManager.preferences.value.vibrationEnabled)
  }

  @Test
  fun `transcription record instantiation`() {
    val record = TranscriptionRecord(
      title = "اختبار",
      speaker = "طبيب",
      content = "يرجى تناول الدواء بانتظام",
      type = "LIVE"
    )
    assertNotNull(record)
    assertEquals("اختبار", record.title)
    assertEquals("طبيب", record.speaker)
    assertEquals("يرجى تناول الدواء بانتظام", record.content)
  }
}
