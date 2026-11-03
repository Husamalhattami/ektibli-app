package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.data.TranscriptionRecord
import com.example.data.TranscriptionRepository
import com.example.service.GeminiAudioTranscriber
import com.example.service.HapticAndVisualAlertManager
import com.example.service.TranscriptionResult
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ErrorColor
import kotlinx.coroutines.launch

@Composable
fun AudioTranscribeScreen(
    incomingAudioUri: Uri?,
    transcriber: GeminiAudioTranscriber,
    alertManager: HapticAndVisualAlertManager,
    repository: TranscriptionRepository,
    preferences: AppPreferences,
    onIncomingUriConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedUri by remember { mutableStateOf<Uri?>(incomingAudioUri) }
    var isTranscribing by remember { mutableStateOf(false) }
    var transcriptionText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var audioFileName by remember { mutableStateOf<String?>(null) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            audioFileName = uri.lastPathSegment ?: "ملف صوتي محدد"
            errorMessage = null
            transcriptionText = ""
        }
    }

    fun startTranscription(uri: Uri) {
        scope.launch {
            isTranscribing = true
            errorMessage = null
            transcriptionText = ""

            when (val result = transcriber.transcribeAudioUri(uri)) {
                is TranscriptionResult.Success -> {
                    transcriptionText = result.text
                    isTranscribing = false
                    alertManager.triggerTranscriptionCompleteAlert(
                        preferences.vibrationEnabled,
                        preferences.screenFlashEnabled
                    )
                }
                is TranscriptionResult.Error -> {
                    errorMessage = result.message
                    isTranscribing = false
                }
            }
        }
    }

    // Auto-transcribe if incoming uri from WhatsApp share
    LaunchedEffect(incomingAudioUri) {
        if (incomingAudioUri != null) {
            selectedUri = incomingAudioUri
            audioFileName = "رسالة صوتية مستلمة من واتساب"
            onIncomingUriConsumed()
            startTranscription(incomingAudioUri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .testTag("audio_transcribe_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Text(
                text = "تحويل الرسائل الصوتية (واتساب)",
                color = AppTheme.colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // WhatsApp How-To Guide Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier
                            .size(26.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "طريقة تحويل صوتيات واتساب:",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. افتح تطبيق واتساب وحدد الرسالة الصوتية.\n2. اضغط زر «مشاركة» في واتساب.\n3. اختر تطبيق «اكتب لي» من القائمة.\nسيبدأ التحويل فوراً دون أي صوت وبخط كبير وواضح.",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Choose file manually button
            Button(
                onClick = { audioPicker.launch("audio/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("pick_audio_file_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.surface,
                    contentColor = AppTheme.colors.textPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AppTheme.colors.surfaceBorder)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أو اختر ملف صوتي من ذاكرة الهاتف",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Audio Info & Transcribe Trigger
            if (selectedUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = audioFileName ?: "ملف صوتي جاهز للتحويل",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { selectedUri?.let { startTranscription(it) } },
                            enabled = !isTranscribing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_transcription_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.primary,
                                contentColor = AppTheme.colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isTranscribing) {
                                CircularProgressIndicator(
                                    color = AppTheme.colors.onPrimary,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("جارٍ التحويل بالذكاء الاصطناعي…", fontSize = 16.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تحويل إلى نص الآن", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error display
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ErrorColor, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = ErrorColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Right
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Result Display Card
            AnimatedVisibility(visible = transcriptionText.isNotBlank()) {
                Column {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, AppTheme.colors.primary, RoundedCornerShape(20.dp))
                            .testTag("audio_result_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "النص المفرغ من الرسالة الصوتية:",
                                    color = AppTheme.colors.primary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "تم بنجاح ✓",
                                    color = AppTheme.colors.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = transcriptionText,
                                color = AppTheme.colors.textPrimary,
                                fontSize = preferences.fontSizeSp.sp,
                                fontWeight = if (preferences.isBoldFont) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = (preferences.fontSizeSp + 10).sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("transcription_result_text")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Copy, Share, Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("النص المفرغ", transcriptionText))
                                alertManager.triggerSoftTap()
                                scope.launch { snackbarHostState.showSnackbar("تم نسخ النص بنجاح") }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("copy_audio_result_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.surface,
                                contentColor = AppTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.surfaceBorder)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ", fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, transcriptionText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة النص"))
                                alertManager.triggerSoftTap()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("share_audio_result_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.surface,
                                contentColor = AppTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.surfaceBorder)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة", fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    repository.save(
                                        TranscriptionRecord(
                                            title = "رسالة صوتية (واتساب)",
                                            speaker = "تسجيل صوتي",
                                            content = transcriptionText,
                                            type = "VOICE_NOTE"
                                        )
                                    )
                                    alertManager.triggerSoftTap()
                                    snackbarHostState.showSnackbar("تم حفظ النص في السجل")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("save_audio_result_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.primary,
                                contentColor = AppTheme.colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ", fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}
