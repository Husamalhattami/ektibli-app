package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.data.ContrastTheme
import com.example.data.PreferencesManager
import com.example.service.HapticAndVisualAlertManager
import com.example.ui.theme.AppTheme

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    preferences: AppPreferences,
    alertManager: HapticAndVisualAlertManager,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var speakerNameInput by remember { mutableStateOf(preferences.defaultSpeakerName) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "المظهر والخط والتنبيهات",
                color = AppTheme.colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Font Size Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatSize, contentDescription = null, tint = AppTheme.colors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حجم الخط: ${preferences.fontSizeSp} نقطة",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = preferences.fontSizeSp.toFloat(),
                        onValueChange = { preferencesManager.setFontSize(it.toInt()) },
                        valueRange = 22f..54f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = AppTheme.colors.primary,
                            activeTrackColor = AppTheme.colors.primary,
                            inactiveTrackColor = AppTheme.colors.surfaceBorder
                        ),
                        modifier = Modifier.testTag("font_size_slider")
                    )

                    // Preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(24 to "عادي", 32 to "كبير", 42 to "ضخم", 52 to "عملاق").forEach { (size, label) ->
                            val isSelected = preferences.fontSizeSp == size
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AppTheme.colors.primary else AppTheme.colors.surface)
                                    .border(1.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(8.dp))
                                    .clickable { preferencesManager.setFontSize(size) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) AppTheme.colors.onPrimary else AppTheme.colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bold Font Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "خط عريض جداً (لقراءة أسهل)",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = preferences.isBoldFont,
                            onCheckedChange = { preferencesManager.setBoldFont(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppTheme.colors.onPrimary,
                                checkedTrackColor = AppTheme.colors.primary
                            ),
                            modifier = Modifier.testTag("bold_font_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.colors.background)
                            .border(1.5.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "معاينة: مرحباً بك في «اكتب لي»",
                            color = AppTheme.colors.textPrimary,
                            fontSize = preferences.fontSizeSp.sp,
                            fontWeight = if (preferences.isBoldFont) FontWeight.ExtraBold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Contrast Theme Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, tint = AppTheme.colors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نمط الألوان والتباين البصري",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ContrastTheme.entries.forEach { theme ->
                        val isSelected = preferences.theme == theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AppTheme.colors.primary.copy(alpha = 0.15f) else AppTheme.colors.surface)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.surfaceBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { preferencesManager.setTheme(theme) }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = theme.titleAr,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )

                            // Color swatches indicator
                            val (bgPreview, fgPreview) = when (theme) {
                                ContrastTheme.YELLOW_ON_BLACK -> Color(0xFF000000) to Color(0xFFFDE047)
                                ContrastTheme.DARK_OLED -> Color(0xFF09090B) to Color(0xFFFFFFFF)
                                ContrastTheme.LIGHT_HIGH_CONTRAST -> Color(0xFFFFFFFF) to Color(0xFF09090B)
                                ContrastTheme.NAVY_CYAN -> Color(0xFF0B132B) to Color(0xFF38BDF8)
                                ContrastTheme.EMERALD_DARK -> Color(0xFF022C22) to Color(0xFF34D399)
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(bgPreview)
                                    .border(2.dp, fgPreview, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = fgPreview, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Tactile and Visual Alerts Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التنبيهات اللمسية والبصرية",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Vibration Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = AppTheme.colors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "تنبيه الهاتف بالاهتزاز",
                                    color = AppTheme.colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "يهتز عند بدء الكلام، توقفه، أو وصول رسالة",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = preferences.vibrationEnabled,
                            onCheckedChange = { preferencesManager.setVibrationEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppTheme.colors.onPrimary,
                                checkedTrackColor = AppTheme.colors.primary
                            ),
                            modifier = Modifier.testTag("vibration_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { alertManager.triggerTranscriptionCompleteAlert(true, false) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.surface,
                            contentColor = AppTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.surfaceBorder)
                    ) {
                        Text("تجربة نمط الاهتزاز", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Screen Flash Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = AppTheme.colors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "وميض الشاشة البصري",
                                    color = AppTheme.colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "وميض لطيف عند رصد صوت أو اكتمال التفريغ",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = preferences.screenFlashEnabled,
                            onCheckedChange = { preferencesManager.setScreenFlashEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppTheme.colors.onPrimary,
                                checkedTrackColor = AppTheme.colors.primary
                            ),
                            modifier = Modifier.testTag("screen_flash_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { alertManager.triggerSpeechStartAlert(false, true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.surface,
                            contentColor = AppTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.surfaceBorder)
                    ) {
                        Text("تجربة وميض الشاشة", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Default Speaker Name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AppTheme.colors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اسم المتحدث الافتراضي",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = speakerNameInput,
                        onValueChange = {
                            speakerNameInput = it
                            preferencesManager.setSpeakerName(it.ifBlank { "المتحدث" })
                        },
                        placeholder = { Text("المتحدث") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary,
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.surfaceBorder
                        ),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. App Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AppTheme.colors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عن تطبيق «اكتب لي»",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "صُمم تطبيق «اكتب لي» خصيصاً لمساعدة مجتمع الصم وضعاف السمع في التواصل مع الآخرين، وتحويل الحديث والرسائل الصوتية في واتساب إلى نص كبير فوري دون أي اعتماد على الصوت أو السماعات.",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
