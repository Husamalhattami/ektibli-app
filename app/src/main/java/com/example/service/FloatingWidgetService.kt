package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingWidgetService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null
    private var collapsedView: FrameLayout? = null
    private var expandedView: LinearLayout? = null
    private var liveTranscriptTextView: TextView? = null
    private var micButton: Button? = null
    private var alertBannerTextView: TextView? = null

    private lateinit var alertManager: HapticAndVisualAlertManager
    private lateinit var speechManager: SpeechRecognitionManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private var transcriptJob: Job? = null
    private var isListening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        alertManager = HapticAndVisualAlertManager(this)
        speechManager = SpeechRecognitionManager(this, alertManager)

        startForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        initFloatingView()
        observeSpeech()
    }

    private fun startForegroundNotification() {
        val channelId = "floating_widget_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "الزر العائم فوق واتساب",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "خدمة المساعد العائم للتفريغ الفوري فوق التطبيقات"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("تطبيق اكتب لي • المساعد العائم نشط")
            .setContentText("الزر العائم يعمل الآن فوق واتساب والتطبيقات الأخرى")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun initFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        floatingView = FrameLayout(this)

        // 1. Collapsed Bubble View
        collapsedView = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#141414"))
                setStroke(4, Color.parseColor("#FACC15"))
            }
            background = bg

            val icon = ImageView(this@FloatingWidgetService).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                setColorFilter(Color.parseColor("#FACC15"))
                val p = 24
                setPadding(p, p, p, p)
            }
            addView(icon, FrameLayout.LayoutParams(140, 140, Gravity.CENTER))
        }

        // 2. Expanded Floating Window
        expandedView = createExpandedView()
        expandedView?.visibility = View.GONE

        floatingView?.addView(collapsedView)
        floatingView?.addView(expandedView)

        // Drag and click handling on collapsed bubble
        setupTouchListener(params)

        try {
            windowManager?.addView(floatingView, params)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun createExpandedView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(Color.parseColor("#09090B"))
                setStroke(4, Color.parseColor("#FACC15"))
            }
            background = bg
            setPadding(28, 24, 28, 28)
            layoutParams = LinearLayout.LayoutParams(700, LinearLayout.LayoutParams.WRAP_CONTENT)

            // Top Header: Title & Close buttons
            val header = LinearLayout(this@FloatingWidgetService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val titleView = TextView(this@FloatingWidgetService).apply {
                text = "اكتب لي • مساعد واتساب"
                setTextColor(Color.parseColor("#FDE047"))
                textSize = 16f
                paint.isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val minimizeBtn = TextView(this@FloatingWidgetService).apply {
                text = "—"
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(16, 8, 16, 8)
                setOnClickListener {
                    collapseView()
                }
            }

            val closeBtn = TextView(this@FloatingWidgetService).apply {
                text = "✕"
                setTextColor(Color.parseColor("#EF4444"))
                textSize = 18f
                setPadding(16, 8, 16, 8)
                setOnClickListener {
                    stopSelf()
                }
            }

            header.addView(titleView)
            header.addView(minimizeBtn)
            header.addView(closeBtn)
            addView(header)

            // Incoming Voice Alert Banner (shown when WhatsApp voice arrives)
            alertBannerTextView = TextView(this@FloatingWidgetService).apply {
                text = "وصلتك رسالة صوتية جديدة في واتساب 🎤"
                setTextColor(Color.parseColor("#000000"))
                textSize = 13f
                paint.isFakeBoldText = true
                gravity = Gravity.CENTER
                val bannerBg = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(Color.parseColor("#FACC15"))
                }
                background = bannerBg
                setPadding(16, 10, 16, 10)
                visibility = View.GONE
            }
            addView(alertBannerTextView)

            // Spacer
            val spacer1 = View(this@FloatingWidgetService).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16)
            }
            addView(spacer1)

            // Live speech display box
            val scrollContainer = ScrollView(this@FloatingWidgetService).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 260)
                val boxBg = GradientDrawable().apply {
                    cornerRadius = 18f
                    setColor(Color.parseColor("#18181B"))
                    setStroke(2, Color.parseColor("#3F3F46"))
                }
                background = boxBg
                setPadding(16, 16, 16, 16)
            }

            liveTranscriptTextView = TextView(this@FloatingWidgetService).apply {
                text = "اضغط «بدء الاستماع» لتحويل الصوت أو كلام الشخص أمامك مباشرة إلى نص هنا."
                setTextColor(Color.parseColor("#FAFAFA"))
                textSize = 20f
                paint.isFakeBoldText = true
                setLineSpacing(0f, 1.3f)
                gravity = Gravity.RIGHT
            }
            scrollContainer.addView(liveTranscriptTextView)
            addView(scrollContainer)

            // Spacer
            val spacer2 = View(this@FloatingWidgetService).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16)
            }
            addView(spacer2)

            // Mic Toggle Button
            micButton = Button(this@FloatingWidgetService).apply {
                text = "بدء الاستماع الآن 🎤"
                setTextColor(Color.BLACK)
                textSize = 15f
                paint.isFakeBoldText = true
                val btnBg = GradientDrawable().apply {
                    cornerRadius = 16f
                    setColor(Color.parseColor("#FACC15"))
                }
                background = btnBg
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    110
                )
                setOnClickListener {
                    toggleSpeechListening()
                }
            }
            addView(micButton)

            // Emergency card button
            val emergencyCardBtn = Button(this@FloatingWidgetService).apply {
                text = "«أنا لا أسمع، من فضلك اكتب لي»"
                setTextColor(Color.WHITE)
                textSize = 13f
                paint.isFakeBoldText = true
                val btnBg = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(Color.parseColor("#27272A"))
                    setStroke(2, Color.parseColor("#FACC15"))
                }
                background = btnBg
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    100
                ).apply {
                    topMargin = 12
                }
                layoutParams = lp
                setOnClickListener {
                    liveTranscriptTextView?.text = "«أنا لا أسمع، من فضلك اكتب لي.»\n\n(يُرجى إظهار هذه الشاشة للشخص المقابل)"
                    liveTranscriptTextView?.textSize = 26f
                    alertManager.triggerSoftTap()
                }
            }
            addView(emergencyCardBtn)

            // Open full app button
            val openAppBtn = TextView(this@FloatingWidgetService).apply {
                text = "فتح تطبيق اكتب لي الكامل ←"
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 14f
                paint.isFakeBoldText = true
                gravity = Gravity.CENTER
                setPadding(12, 16, 12, 8)
                setOnClickListener {
                    val intent = Intent(this@FloatingWidgetService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    collapseView()
                }
            }
            addView(openAppBtn)
        }
    }

    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        collapsedView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                            isDragging = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            windowManager?.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            expandView()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun expandView() {
        collapsedView?.visibility = View.GONE
        expandedView?.visibility = View.VISIBLE
        alertManager.triggerSoftTap()
    }

    private fun collapseView() {
        if (isListening) {
            speechManager.stopListening()
            isListening = false
            updateMicButtonState()
        }
        expandedView?.visibility = View.GONE
        collapsedView?.visibility = View.VISIBLE
        alertManager.triggerSoftTap()
    }

    private fun toggleSpeechListening() {
        if (isListening) {
            speechManager.stopListening()
            isListening = false
        } else {
            speechManager.startListening(language = "ar-SA", speaker = "المتحدث", continuous = true)
            isListening = true
            alertManager.triggerSpeechStartAlert(true, true)
        }
        updateMicButtonState()
    }

    private fun updateMicButtonState() {
        if (isListening) {
            micButton?.text = "إيقاف الاستماع 🛑"
            val bg = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(Color.parseColor("#EF4444"))
            }
            micButton?.background = bg
            micButton?.setTextColor(Color.WHITE)
        } else {
            micButton?.text = "بدء الاستماع الآن 🎤"
            val bg = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(Color.parseColor("#FACC15"))
            }
            micButton?.background = bg
            micButton?.setTextColor(Color.BLACK)
        }
    }

    private fun observeSpeech() {
        transcriptJob = scope.launch {
            launch {
                speechManager.fullTranscript.collectLatest { full ->
                    if (full.isNotBlank()) {
                        liveTranscriptTextView?.text = full
                        liveTranscriptTextView?.textSize = 22f
                    }
                }
            }
            launch {
                speechManager.partialTranscript.collectLatest { partial ->
                    if (partial.isNotBlank()) {
                        val current = speechManager.fullTranscript.value
                        val display = if (current.isBlank()) partial else "$current\n$partial"
                        liveTranscriptTextView?.text = display
                        liveTranscriptTextView?.textSize = 22f
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_VOICE_ALERT) {
            // Trigger visual and haptic alert for incoming WhatsApp audio!
            alertManager.triggerTranscriptionCompleteAlert(true, true)
            alertBannerTextView?.visibility = View.VISIBLE
            expandView()
            liveTranscriptTextView?.text = "وصلتك رسالة صوتية جديدة في واتساب 🎤\n\nاضغط على «بدء الاستماع» لتشغيل الميكروفون وتحويل الصوت فوراً، أو اختر مشاركة المقطع لتطبيق اكتب لي."
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        transcriptJob?.cancel()
        speechManager.destroy()
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (_: Exception) {}
        }
    }

    companion object {
        const val ACTION_VOICE_ALERT = "com.example.ektibli.VOICE_ALERT"

        fun notifyVoiceMessageReceived(context: Context) {
            val intent = Intent(context, FloatingWidgetService::class.java).apply {
                action = ACTION_VOICE_ALERT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
