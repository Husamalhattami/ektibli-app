package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppPreferences
import com.example.data.TranscriptionRecord
import com.example.data.TranscriptionRepository
import com.example.service.HapticAndVisualAlertManager
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ErrorColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    repository: TranscriptionRepository,
    alertManager: HapticAndVisualAlertManager,
    preferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "LIVE", "VOICE_NOTE", "CONVERSATION"
    var showClearAllDialog by remember { mutableStateOf(false) }
    var selectedRecordForView by remember { mutableStateOf<TranscriptionRecord?>(null) }

    val allRecords by repository.allTranscriptions.collectAsState(initial = emptyList())

    val filteredRecords = remember(allRecords, searchQuery, selectedFilter) {
        allRecords.filter { record ->
            val matchesFilter = when (selectedFilter) {
                "LIVE" -> record.type == "LIVE"
                "VOICE_NOTE" -> record.type == "VOICE_NOTE"
                "CONVERSATION" -> record.type == "CONVERSATION"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                record.content.contains(searchQuery, ignoreCase = true) ||
                        record.title.contains(searchQuery, ignoreCase = true) ||
                        record.speaker.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .testTag("history_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المحادثات والتفريغ",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                if (allRecords.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearAllDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surface)
                            .border(1.dp, AppTheme.colors.surfaceBorder, CircleShape)
                            .testTag("clear_all_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "مسح كل السجل",
                            tint = ErrorColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "البحث في السجل والمحادثات…",
                        color = AppTheme.colors.textSecondary.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح البحث", tint = AppTheme.colors.textSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary,
                    focusedBorderColor = AppTheme.colors.primary,
                    unfocusedBorderColor = AppTheme.colors.surfaceBorder,
                    focusedContainerColor = AppTheme.colors.surface,
                    unfocusedContainerColor = AppTheme.colors.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "الكل",
                    "LIVE" to "استماع مباشر",
                    "VOICE_NOTE" to "صوتيات واتساب",
                    "CONVERSATION" to "محادثة ثنائية"
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AppTheme.colors.primary else AppTheme.colors.surface)
                            .border(
                                1.dp,
                                if (isSelected) AppTheme.colors.primary else AppTheme.colors.surfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedFilter = key
                                alertManager.triggerSoftTap()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) AppTheme.colors.onPrimary else AppTheme.colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List of Records
            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = AppTheme.colors.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "لا توجد نتائج تطابق بحثك" else "السجل فارغ حالياً",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        HistoryRecordCard(
                            record = record,
                            onClick = { selectedRecordForView = record },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("نص محفوظ", record.content))
                                alertManager.triggerSoftTap()
                                scope.launch { snackbarHostState.showSnackbar("تم نسخ النص") }
                            },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, record.content)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة"))
                                alertManager.triggerSoftTap()
                            },
                            onDelete = {
                                scope.launch {
                                    repository.delete(record)
                                    alertManager.triggerSoftTap()
                                    snackbarHostState.showSnackbar("تم حذف السجل")
                                }
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Clear All Dialog Confirmation
        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = { Text("مسح كل السجل", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من رغبتك في حذف جميع المحادثات والنصوص المحفوظة؟", color = AppTheme.colors.textSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch { repository.clearAll() }
                            showClearAllDialog = false
                            alertManager.triggerSoftTap()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorColor, contentColor = AppTheme.colors.textPrimary)
                    ) {
                        Text("مسح الكل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) {
                        Text("إلغاء", color = AppTheme.colors.textSecondary)
                    }
                },
                containerColor = AppTheme.colors.surface
            )
        }

        // Full Screen Viewer Dialog
        if (selectedRecordForView != null) {
            val record = selectedRecordForView!!
            Dialog(
                onDismissRequest = { selectedRecordForView = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background),
                    color = AppTheme.colors.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedRecordForView = null },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.surface)
                                    .border(1.dp, AppTheme.colors.surfaceBorder, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = AppTheme.colors.textPrimary)
                            }

                            Text(
                                text = record.title,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("نص محفوظ", record.content))
                                        alertManager.triggerSoftTap()
                                        scope.launch { snackbarHostState.showSnackbar("تم نسخ النص") }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = AppTheme.colors.primary)
                                }
                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, record.content)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة"))
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = AppTheme.colors.textPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(2.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        Text(
                                            text = record.content,
                                            color = AppTheme.colors.textPrimary,
                                            fontSize = preferences.fontSizeSp.sp,
                                            fontWeight = if (preferences.isBoldFont) FontWeight.Bold else FontWeight.Normal,
                                            lineHeight = (preferences.fontSizeSp + 10).sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { selectedRecordForView = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.primary,
                                contentColor = AppTheme.colors.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("إغلاق العرض", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: TranscriptionRecord,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(record.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }

    val typeLabel = when (record.type) {
        "VOICE_NOTE" -> "صوتيات واتساب"
        "CONVERSATION" -> "محادثة ثنائية"
        else -> "استماع مباشر"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, AppTheme.colors.surfaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("history_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppTheme.colors.primary.copy(alpha = 0.2f))
                            .border(1.dp, AppTheme.colors.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            color = AppTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.title,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = dateStr,
                    color = AppTheme.colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = record.content,
                color = AppTheme.colors.textPrimary,
                fontSize = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
