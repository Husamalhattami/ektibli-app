package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppTheme

@Composable
fun QuickCardDialog(
    onDismiss: () -> Unit,
    onSpeechToTextClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFlipped by remember { mutableStateOf(false) }
    var selectedPhrase by remember { mutableStateOf("أنا لا أسمع، من فضلك اكتب لي.") }
    var otherPersonReply by remember { mutableStateOf("") }
    var submittedReply by remember { mutableStateOf("") }

    val presetPhrases = listOf(
        "أنا لا أسمع، من فضلك اكتب لي.",
        "أنا أصم، أرجو التحدث بوضوح أمام الهاتف أو الكتابة لي.",
        "كم الحساب أو السعر الإجمالي؟",
        "أين يجب أن أوقع؟",
        "شكراً جزيلاً على تفهمك ومساعدتك.",
        "أحتاج مساعدة عاجلة من فضلك."
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .testTag("quick_card_dialog"),
            color = AppTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surface)
                            .border(1.dp, AppTheme.colors.surfaceBorder, CircleShape)
                            .testTag("close_card_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = AppTheme.colors.textPrimary
                        )
                    }

                    Text(
                        text = "بطاقة التواصل السريع",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // 180° Flip button to present to the person opposite
                    IconButton(
                        onClick = { isFlipped = !isFlipped },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isFlipped) AppTheme.colors.primary else AppTheme.colors.surface)
                            .border(1.dp, AppTheme.colors.surfaceBorder, CircleShape)
                            .testTag("flip_screen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "قلب الشاشة للمقابل",
                            tint = if (isFlipped) AppTheme.colors.onPrimary else AppTheme.colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The Giant Message Card (rotatable 180° for presenting across a table)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .rotate(if (isFlipped) 180f else 0f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AppTheme.colors.surface)
                        .border(3.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = selectedPhrase,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 48.sp,
                            modifier = Modifier.testTag("giant_phrase_text")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "للتواصل مع أصم • يرجى الكتابة في الخانة أدناه",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phrase Selector Chips
                Text(
                    text = "اختر عبارة جاهزة:",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetPhrases) { phrase ->
                        val isSelected = phrase == selectedPhrase
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) AppTheme.colors.primary else AppTheme.colors.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.surfaceBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedPhrase = phrase }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = phrase,
                                color = if (isSelected) AppTheme.colors.onPrimary else AppTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Area for the other person to type their answer right back!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppTheme.colors.surface)
                        .border(1.5.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "رد الطرف الآخر (يكتب هنا):",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = otherPersonReply,
                                onValueChange = { otherPersonReply = it },
                                placeholder = {
                                    Text(
                                        "اكتب ردك هنا للشخص الأصم…",
                                        color = AppTheme.colors.textSecondary.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("other_person_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = AppTheme.colors.textPrimary,
                                    unfocusedTextColor = AppTheme.colors.textPrimary,
                                    focusedBorderColor = AppTheme.colors.primary,
                                    unfocusedBorderColor = AppTheme.colors.surfaceBorder
                                ),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (otherPersonReply.isNotBlank()) {
                                        submittedReply = otherPersonReply.trim()
                                        otherPersonReply = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.primary)
                                    .testTag("submit_reply_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "عرض الرد",
                                    tint = AppTheme.colors.onPrimary
                                )
                            }
                        }

                        AnimatedVisibility(visible = submittedReply.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppTheme.colors.background)
                                    .border(2.dp, AppTheme.colors.primary, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "الرد المكتوب:",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = submittedReply,
                                    color = AppTheme.colors.textPrimary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 36.sp,
                                    modifier = Modifier.testTag("submitted_reply_text")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("close_card_footer_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.surface,
                            contentColor = AppTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.surfaceBorder)
                    ) {
                        Text("العودة للتطبيق", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    if (onSpeechToTextClick != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onSpeechToTextClick()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("open_live_speech_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.primary,
                                contentColor = AppTheme.colors.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("الاستماع المباشر", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
