package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WhatsAppNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName ?: ""
        // Check if notification is from WhatsApp or WhatsApp Business
        if (pkgName == "com.whatsapp" || pkgName == "com.whatsapp.w4b") {
            val extras = sbn.notification?.extras ?: return
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            val combined = "$title $text $subText $bigText".lowercase()

            val isVoiceMessage = combined.contains("رسالة صوتية") ||
                    combined.contains("صوتية") ||
                    combined.contains("مقطع صوتي") ||
                    combined.contains("تسجيل صوتي") ||
                    combined.contains("voice message") ||
                    combined.contains("audio") ||
                    combined.contains("🎤") ||
                    combined.contains("ptt")

            if (isVoiceMessage) {
                // Notify the floating widget to pop up and alert the user!
                try {
                    FloatingWidgetService.notifyVoiceMessageReceived(applicationContext)
                } catch (_: Exception) {}
            }
        }
    }
}
