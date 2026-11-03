package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppPreferences
import com.example.data.TranscriptionRecord
import com.example.data.TranscriptionRepository
import com.example.service.HapticAndVisualAlertManager
import com.example.service.SpeechRecognitionManager
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ErrorColor
import kotlinx.coroutines.launch

data class DialogueMessage(
    val sender: String, // "THEM" or "ME"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ConversationScreen(
    speechManager: SpeechRecognitionManager,
    alertManager: HapticAndVisualAlertManager,
    repository: TranscriptionRepository,
    preferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isListening by speechManager.isListening.collectAsState()
    val fullTranscript by speechManager.fullTranscript.collectAsState()
    val partialTranscript by speechManager.partialTranscript.collectAsState()
    val soundLevel by speechManager.soundLevel.collectAsState()

    val messages = remember { mutableStateListOf<DialogueMessage>() }
    var userReplyInput by remember { mutableStateOf("") }
    var isTopFlipped by remember { mutableStateOf(false) }
    var fullScreenReplyText by remember { mutableStateOf<String?>(null) }
    var isFullScreenFlipped by remember { mutableStateOf(false) }

    val quickChips = listOf(
        "نعم، بالتأكيد",
        "لا، شكراً لك",
        "لحظة واحدة من فضلك",
        "كم الحساب أو التكلفة؟",
        "أين يجب أن أوقع؟",
        "أنا موافق تماماً",
        "هل يمكنك تكرار ذلك؟",
        "شكراً جزيلاً لك"
    )

    // When new speech result arrives from the speech recognizer, append to dialogue messages
    LaunchedEffect(fullTranscript) {
        if (fullTranscript.isNotBlank()) {
            val lines = fullTranscript.split("\n").filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                val latest = lines.last()
                val cleaned = latest.replace(Regex("^\\[.*?\\]:\\s*"), "")
                if (cleaned.isNotBlank() && (messages.isEmpty() || messages.last().text != cleaned)) {
                    messages.add(DialogueMessage(sender = "THEM", text = cleaned))
                    speechManager.clearTranscript()
                    alertManager.triggerSpeechEndAlert(preferences.vibrationEnabled)
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .testTag("conversation_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with Save and Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وضع المحادثة الثنائية",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            if (messages.isNotEmpty()) {
                                scope.launch {
                                    val dialogueText = messages.joinToString("\n") {
                                        val role = if (it.sender == "THEM") "الطرف الآخر" else "أنا"
                                        "[$role]: ${it.text}"
                                    }
                                    repository.save(
                                        TranscriptionRecord(
                                            title = "محادثة ثنائية (${messages.size} ردود)",
                                            speaker = "محادثة ثنائية",
                                            content = dialogueText,
                                            type = "CONVERSATION"
                                        )
                                    )
                                    alertManager.triggerSoftTap()
                                    snackbarHostState.showSnackbar("تم حفظ المحادثة في السجل")
                                }
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surface)
                            .border(1.dp, AppTheme.colors.surfaceBorder, CircleShape)
                            .testTag("save_conversation_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "حفظ المحادثة",
                            tint = AppTheme.colors.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            messages.clear()
                            speechManager.clearTranscript()
                            alertManager.triggerSoftTap()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surface)
                            .border(1.dp, AppTheme.colors.surfaceBorder, CircleShape)
                            .testTag("clear_conversation_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "مسح المحادثة",
                            tint = AppTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Upper Panel: The Hearing Speaker's Speech-to-Text section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .rotate(if (isTopFlipped) 180f else 0f)
                    .border(2.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(18.dp))
                    .testTag("speaker_top_panel"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الطرف الآخر (يتحدث):",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 180° Flip toggle for top panel
                            IconButton(
                                onClick = { isTopFlipped = !isTopFlipped },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "قلب للطرف المقابل",
                                    tint = if (isTopFlipped) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Mic toggle button
                            IconButton(
                                onClick = {
                                    if (isListening) {
                                        speechManager.stopListening()
                                    } else {
                                        speechManager.startListening(
                                            language = preferences.languageCode,
                                            speaker = "الطرف الآخر",
                                            continuous = true
                                        )
                                    }
                                    alertManager.triggerSoftTap()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) ErrorColor else AppTheme.colors.primary)
                                    .testTag("conversation_mic_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "ميكروفون الطرف الآخر",
                                    tint = if (isListening) AppTheme.colors.textPrimary else AppTheme.colors.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sound wave
                    SoundWaveVisualizer(
                        isListening = isListening,
                        soundLevel = soundLevel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Current spoken text
                    val currentLiveText = partialTranscript.ifBlank {
                        if (isListening) "يستمع الآن… تحدّث أمامي" else "اضغط على أيقونة الميكروفون لبدء الاستماع"
                    }

                    Text(
                        text = currentLiveText,
                        color = if (partialTranscript.isNotBlank()) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                        fontSize = (preferences.fontSizeSp - 4).coerceAtLeast(18).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (preferences.fontSizeSp + 4).sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: Dialogue History List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.surface)
                    .border(1.dp, AppTheme.colors.surfaceBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "سجل المحادثة الثنائية سيظهر هنا…\nتحدّث الطرف الآخر أو اكتب ردك في الأسفل.",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages) { msg ->
                            val isMe = msg.sender == "ME"
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.Start else Alignment.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isMe) AppTheme.colors.primary.copy(alpha = 0.2f) else AppTheme.colors.cardBackground)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isMe) AppTheme.colors.primary else AppTheme.colors.surfaceBorder,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isMe) "أنا (رد مكتوب)" else "الطرف الآخر (كلام منطوق)",
                                                color = if (isMe) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            // Full screen magnifier for individual message
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = "تكبير",
                                                tint = AppTheme.colors.textSecondary,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clickable { fullScreenReplyText = msg.text }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = msg.text,
                                            color = AppTheme.colors.textPrimary,
                                            fontSize = (preferences.fontSizeSp - 4).coerceAtLeast(18).sp,
                                            fontWeight = if (preferences.isBoldFont) FontWeight.Bold else FontWeight.Normal,
                                            lineHeight = (preferences.fontSizeSp + 4).sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Reply Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickChips) { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.colors.surface)
                            .border(1.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                userReplyInput = chip
                                alertManager.triggerSoftTap()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = chip,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User Typing & Send / Fullscreen Preview row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userReplyInput,
                    onValueChange = { userReplyInput = it },
                    placeholder = {
                        Text(
                            "اكتب ردك ليظهر بخط كبير…",
                            color = AppTheme.colors.textSecondary.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("conversation_reply_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary,
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.surfaceBorder
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Giant preview button (تكبير الرد)
                IconButton(
                    onClick = {
                        if (userReplyInput.isNotBlank()) {
                            fullScreenReplyText = userReplyInput.trim()
                            alertManager.triggerSoftTap()
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.colors.surface)
                        .border(1.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(14.dp))
                        .testTag("magnify_reply_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "تكبير الرد للشخص المقابل",
                        tint = AppTheme.colors.primary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send button
                IconButton(
                    onClick = {
                        if (userReplyInput.isNotBlank()) {
                            val text = userReplyInput.trim()
                            messages.add(DialogueMessage(sender = "ME", text = text))
                            userReplyInput = ""
                            alertManager.triggerSoftTap()
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.colors.primary)
                        .testTag("send_reply_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال الرد للمحادثة",
                        tint = AppTheme.colors.onPrimary
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Full Screen Reply Display Dialog (for showing to hearing person)
        if (fullScreenReplyText != null) {
            Dialog(
                onDismissRequest = { fullScreenReplyText = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background)
                        .testTag("fullscreen_reply_dialog"),
                    color = AppTheme.colors.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { fullScreenReplyText = null },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.surface)
                                    .border(1.dp, AppTheme.colors.surfaceBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = AppTheme.colors.textPrimary
                                )
                            }

                            Text(
                                text = "عرض الرد بحجم كبير",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Flip 180° for the person in front
                            IconButton(
                                onClick = { isFullScreenFlipped = !isFullScreenFlipped },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isFullScreenFlipped) AppTheme.colors.primary else AppTheme.colors.surface)
                                    .border(1.dp, AppTheme.colors.surfaceBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "قلب الشاشة للمقابل",
                                    tint = if (isFullScreenFlipped) AppTheme.colors.onPrimary else AppTheme.colors.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .rotate(if (isFullScreenFlipped) 180f else 0f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(AppTheme.colors.surface)
                                .border(3.dp, AppTheme.colors.primary, RoundedCornerShape(24.dp))
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fullScreenReplyText ?: "",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                lineHeight = 58.sp,
                                modifier = Modifier.testTag("giant_reply_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { fullScreenReplyText = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.primary,
                                contentColor = AppTheme.colors.onPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("تم، العودة للمحادثة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
