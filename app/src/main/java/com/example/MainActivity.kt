package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.example.service.FloatingWidgetService
import com.example.service.GeminiAudioTranscriber
import com.example.service.HapticAndVisualAlertManager
import com.example.service.SpeechRecognitionManager
import com.example.ui.MainScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.EktibliTheme
import java.io.File

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

            // Request Audio & Notification Permissions
            var hasAudioPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                hasAudioPermission = permissionsMap[Manifest.permission.RECORD_AUDIO] == true
            }

            LaunchedEffect(Unit) {
                val neededPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(neededPermissions.toTypedArray())
            }

            // Sync Floating widget service state with user preference
            LaunchedEffect(preferences.floatingWidgetEnabled) {
                if (preferences.floatingWidgetEnabled) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this@MainActivity)) {
                        startFloatingService()
                    }
                } else {
                    stopFloatingService()
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
                        onIncomingUriConsumed = { incomingSharedAudioUri = null },
                        onRequestOverlayPermission = { requestOverlayPermission() }
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

        if (Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action || Intent.ACTION_VIEW == action) {
            val rawUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            } ?: intent.data ?: if (intent.clipData != null && intent.clipData!!.itemCount > 0) {
                intent.clipData!!.getItemAt(0).uri
            } else null

            if (rawUri != null) {
                // Immediately cache the stream to private app storage to avoid permission expiration
                val cachedUri = cacheSharedAudioUri(rawUri)
                incomingSharedAudioUri = cachedUri
                alertManager.triggerTranscriptionCompleteAlert(true, true)
            }
        }
    }

    private fun cacheSharedAudioUri(uri: Uri): Uri {
        return try {
            val cacheFile = File(cacheDir, "whatsapp_shared_${System.currentTimeMillis()}.ogg")
            contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error caching shared audio uri", e)
            uri
        }
    }

    private fun startFloatingService() {
        try {
            val intent = Intent(this, FloatingWidgetService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start floating service", e)
        }
    }

    private fun stopFloatingService() {
        try {
            val intent = Intent(this, FloatingWidgetService::class.java)
            stopService(intent)
        } catch (_: Exception) {}
    }

    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
