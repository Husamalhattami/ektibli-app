package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.data.PreferencesManager
import com.example.data.TranscriptionRepository
import com.example.service.GeminiAudioTranscriber
import com.example.service.HapticAndVisualAlertManager
import com.example.service.SpeechRecognitionManager
import com.example.ui.components.QuickCardDialog
import com.example.ui.components.ScreenFlashOverlay
import com.example.ui.screens.AudioTranscribeScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LiveSpeechScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppTheme

enum class AppTab(val title: String) {
    LIVE("استماع مباشر"),
    CONVERSATION("محادثة ثنائية"),
    TRANSCRIBE("صوتيات واتساب"),
    HISTORY("السجل"),
    SETTINGS("المظهر")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    speechManager: SpeechRecognitionManager,
    alertManager: HapticAndVisualAlertManager,
    transcriber: GeminiAudioTranscriber,
    repository: TranscriptionRepository,
    preferencesManager: PreferencesManager,
    incomingAudioUri: Uri?,
    onIncomingUriConsumed: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val preferences by preferencesManager.preferences.collectAsState()
    var currentTab by remember(incomingAudioUri) {
        mutableStateOf(if (incomingAudioUri != null) AppTab.TRANSCRIBE else AppTab.LIVE)
    }
    var showEmergencyCard by remember { mutableStateOf(false) }

    // If on a secondary tab and user presses back, return to LIVE tab
    BackHandler(enabled = currentTab != AppTab.LIVE) {
        currentTab = AppTab.LIVE
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اكتب لي",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    actions = {
                        // Quick Emergency Card Trigger Button
                        Button(
                            onClick = {
                                alertManager.triggerQuickCardAlert(
                                    preferences.vibrationEnabled,
                                    preferences.screenFlashEnabled
                                )
                                showEmergencyCard = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.primary,
                                contentColor = AppTheme.colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .height(38.dp)
                                .testTag("top_emergency_btn")
                        ) {
                            Text(
                                text = "أنا لا أسمع",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = AppTheme.colors.surface
                    ),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = AppTheme.colors.surfaceBorder.copy(alpha = 0.3f)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = AppTheme.colors.surface,
                    modifier = Modifier
                        .border(1.dp, AppTheme.colors.surfaceBorder.copy(alpha = 0.3f))
                        .testTag("main_bottom_nav")
                ) {
                    val navItems = listOf(
                        Triple(AppTab.LIVE, Icons.Default.Mic, "استماع"),
                        Triple(AppTab.CONVERSATION, Icons.Default.QuestionAnswer, "محادثة"),
                        Triple(AppTab.TRANSCRIBE, Icons.Default.Audiotrack, "واتساب"),
                        Triple(AppTab.HISTORY, Icons.Default.History, "السجل"),
                        Triple(AppTab.SETTINGS, Icons.Default.Settings, "المظهر")
                    )

                    navItems.forEach { (tab, icon, label) ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                currentTab = tab
                                alertManager.triggerSoftTap()
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) AppTheme.colors.onPrimary else AppTheme.colors.textSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = AppTheme.colors.primary
                            ),
                            modifier = Modifier.testTag("nav_item_${tab.name}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AppTab.LIVE -> LiveSpeechScreen(
                        speechManager = speechManager,
                        alertManager = alertManager,
                        repository = repository,
                        preferences = preferences,
                        onOpenEmergencyCard = { showEmergencyCard = true }
                    )
                    AppTab.CONVERSATION -> ConversationScreen(
                        speechManager = speechManager,
                        alertManager = alertManager,
                        repository = repository,
                        preferences = preferences
                    )
                    AppTab.TRANSCRIBE -> AudioTranscribeScreen(
                        incomingAudioUri = incomingAudioUri,
                        transcriber = transcriber,
                        alertManager = alertManager,
                        repository = repository,
                        preferences = preferences,
                        onIncomingUriConsumed = onIncomingUriConsumed,
                        onNavigateToSettings = { currentTab = AppTab.SETTINGS }
                    )
                    AppTab.HISTORY -> HistoryScreen(
                        repository = repository,
                        alertManager = alertManager,
                        preferences = preferences
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        preferencesManager = preferencesManager,
                        preferences = preferences,
                        alertManager = alertManager,
                        onRequestOverlayPermission = onRequestOverlayPermission
                    )
                }
            }
        }

        // Screen Flash Alert Overlay
        ScreenFlashOverlay(flashEvents = alertManager.flashEvents)

        // Emergency Card Dialog
        if (showEmergencyCard) {
            QuickCardDialog(
                onDismiss = { showEmergencyCard = false },
                onSpeechToTextClick = { currentTab = AppTab.LIVE }
            )
        }
    }
}
