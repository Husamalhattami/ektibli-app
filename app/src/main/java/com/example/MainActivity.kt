package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.TranscriptionRepository
import com.example.service.GeminiAudioTranscriber
import com.example.service.HapticAndVisualAlertManager
import com.example.service.SpeechRecognitionManager
import com.example.ui.MainScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.EktibliTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: TranscriptionRepository
    private lateinit var alertManager: HapticAndVisualAlertManager
    private lateinit var speechManager: SpeechRecognitionManager
    private lateinit var transcriber: GeminiAudioTranscriber
    private lateinit var preferencesManager: PreferencesManager

    private var incomingSharedAudioUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(this)
        repository = TranscriptionRepository(database.transcriptionDao())
        alertManager = HapticAndVisualAlertManager(this)
        speechManager = SpeechRecognitionManager(this, alertManager)
        transcriber = GeminiAudioTranscriber(this)
        preferencesManager = PreferencesManager(this)

        handleIntent(intent)

        setContent {
            val preferences by preferencesManager.preferences.collectAsState()

            // Request Audio Permission
            var hasAudioPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasAudioPermission = isGranted
            }

            LaunchedEffect(Unit) {
                if (!hasAudioPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    speechManager.destroy()
                }
            }

            EktibliTheme(contrastTheme = preferences.theme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background)
                ) {
                    MainScreen(
                        speechManager = speechManager,
                        alertManager = alertManager,
                        transcriber = transcriber,
                        repository = repository,
                        preferencesManager = preferencesManager,
                        incomingAudioUri = incomingSharedAudioUri,
                        onIncomingUriConsumed = { incomingSharedAudioUri = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if (type.startsWith("audio/") || type.contains("ogg") || type.contains("octet-stream")) {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                } ?: intent.data ?: intent.clipData?.getItemAt(0)?.uri

                if (uri != null) {
                    incomingSharedAudioUri = uri
                }
            }
        }
    }
}
