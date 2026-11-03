package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.data.TranscriptionRecord
import com.example.data.TranscriptionRepository
import com.example.service.HapticAndVisualAlertManager
import com.example.service.SpeechRecognitionManager
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ErrorColor
import kotlinx.coroutines.launch

@Composable
fun LiveSpeechScreen(
    speechManager: SpeechRecognitionManager,
    alertManager: HapticAndVisualAlertManager,
    repository: TranscriptionRepository,
    preferences: AppPreferences,
    onOpenEmergencyCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isListening by speechManager.isListening.collectAsState()
    val fullTranscript by speechManager.fullTranscript.collectAsState()
    val partialTranscript by speechManager.partialTranscript.collectAsState()
    val soundLevel by speechManager.soundLevel.collectAsState()
    val statusMessage by speechManager.statusMessage.collectAsState()

    var speakerName by remember { mutableStateOf(preferences.defaultSpeakerName) }
    var showEditSpeakerDialog by remember { mutableStateOf(false) }
    var tempSpeakerName by remember { mutableStateOf(speakerName) }

    // Auto-scroll to bottom as new speech arrives
    LaunchedEffect(fullTranscript, partialTranscript) {
        if (fullTranscript.isNotBlank() || partialTranscript.isNotBlank()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Keep alert manager flags synced
    LaunchedEffect(preferences) {
        speechManager.vibrationAlertsEnabled = preferences.vibrationEnabled
        speechManager.screenFlashAlertsEnabled = preferences.screenFlashEnabled
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .testTag("live_speech_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Speaker bar & Quick emergency banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.surface)
                    .border(1.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            tempSpeakerName = speakerName
                            showEditSpeakerDialog = true
                        }
                        .padding(4.dp)
                ) {
                    Text(
                        text = "المتحدث: $speakerName",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تغيير اسم المتحدث",
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Mini Emergency Button
                Button(
                    onClick = onOpenEmergencyCard,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.primary,
                        contentColor = AppTheme.colors.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("emergency_banner_btn")
                ) {
                    Text("«أنا لا أسمع»", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sound Wave Visualizer
            SoundWaveVisualizer(
                isListening = isListening,
                soundLevel = soundLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.surface)
                    .border(1.dp, AppTheme.colors.surfaceBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Badge
            Text(
                text = statusMessage,
                color = if (isListening) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main Large Text Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(2.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(20.dp))
                    .testTag("transcript_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    if (fullTranscript.isBlank() && partialTranscript.isBlank()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = AppTheme.colors.textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "اضغط على زر الميكروفون لبدء الاستماع الفوري",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "سيظهر كلام المتحدث هنا بخط كبير مع تنبيه اهتزاز وبصري",
                                color = AppTheme.colors.textSecondary.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            val combinedText = buildAnnotatedString {
                                append(fullTranscript)
                                if (partialTranscript.isNotBlank()) {
                                    if (fullTranscript.isNotBlank()) append(" ")
                                    withStyle(
                                        SpanStyle(
                                            color = AppTheme.colors.primary,
                                            fontStyle = FontStyle.Italic
                                        )
                                    ) {
                                        append(partialTranscript)
                                    }
                                }
                            }

                            Text(
                                text = combinedText,
                                color = AppTheme.colors.textPrimary,
                                fontSize = preferences.fontSizeSp.sp,
                                fontWeight = if (preferences.isBoldFont) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = (preferences.fontSizeSp + 10).sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("live_transcript_text")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Bar (Clear, Copy, Share, Save)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.surface)
                    .border(1.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clear Button
                IconButton(
                    onClick = {
                        speechManager.clearTranscript()
                        alertManager.triggerSoftTap()
                    },
                    modifier = Modifier.testTag("clear_transcript_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "مسح النص",
                        tint = AppTheme.colors.textSecondary
                    )
                }

                // Copy Button
                IconButton(
                    onClick = {
                        val textToCopy = fullTranscript.ifBlank { partialTranscript }
                        if (textToCopy.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("النص المفرغ", textToCopy))
                            alertManager.triggerSoftTap()
                            scope.launch {
                                snackbarHostState.showSnackbar("تم نسخ النص بنجاح")
                            }
                        }
                    },
                    modifier = Modifier.testTag("copy_transcript_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ النص",
                        tint = AppTheme.colors.textPrimary
                    )
                }

                // Share Button
                IconButton(
                    onClick = {
                        val textToShare = fullTranscript.ifBlank { partialTranscript }
                        if (textToShare.isNotBlank()) {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, textToShare)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة النص"))
                            alertManager.triggerSoftTap()
                        }
                    },
                    modifier = Modifier.testTag("share_transcript_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة النص",
                        tint = AppTheme.colors.textPrimary
                    )
                }

                // Save to History Button
                IconButton(
                    onClick = {
                        val textToSave = fullTranscript.ifBlank { partialTranscript }
                        if (textToSave.isNotBlank()) {
                            scope.launch {
                                repository.save(
                                    TranscriptionRecord(
                                        title = "استماع مباشر مع $speakerName",
                                        speaker = speakerName,
                                        content = textToSave,
                                        type = "LIVE"
                                    )
                                )
                                alertManager.triggerSoftTap()
                                snackbarHostState.showSnackbar("تم حفظ النص في السجل")
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_transcript_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "حفظ في السجل",
                        tint = AppTheme.colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Huge Mic Toggle Button
            FloatingActionButton(
                onClick = {
                    if (isListening) {
                        speechManager.stopListening()
                    } else {
                        speechManager.startListening(
                            language = preferences.languageCode,
                            speaker = speakerName,
                            continuous = true
                        )
                    }
                    alertManager.triggerSoftTap()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("mic_toggle_button"),
                containerColor = if (isListening) ErrorColor else AppTheme.colors.primary,
                contentColor = if (isListening) AppTheme.colors.textPrimary else AppTheme.colors.onPrimary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening) "إيقاف الاستماع" else "بدء الاستماع",
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isListening) "إيقاف الاستماع" else "بدء الاستماع الآن",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Edit Speaker Name Dialog
        if (showEditSpeakerDialog) {
            AlertDialog(
                onDismissRequest = { showEditSpeakerDialog = false },
                title = {
                    Text(
                        text = "اسم المتحدث",
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = tempSpeakerName,
                        onValueChange = { tempSpeakerName = it },
                        label = { Text("اسم الشخص المتحدث (مثل: الطبيب، الكاشير)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary,
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.surfaceBorder
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            speakerName = tempSpeakerName.ifBlank { "المتحدث" }
                            speechManager.setSpeaker(speakerName)
                            showEditSpeakerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primary,
                            contentColor = AppTheme.colors.onPrimary
                        )
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditSpeakerDialog = false }) {
                        Text("إلغاء", color = AppTheme.colors.textSecondary)
                    }
                },
                containerColor = AppTheme.colors.surface
            )
        }
    }
}
