package com.wecker.app.alarm

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.wecker.app.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground-Service, der den Klingelton spielt und vibriert, bis die Aufgabe
 * in der RingActivity gelöst wurde (ACTION_DISMISS). Danach greift ggf. die
 * Aufwachkontrolle.
 */
class AlarmService : Service() {

    companion object {
        const val ACTION_START = "com.wecker.app.service.START"
        const val ACTION_DISMISS = "com.wecker.app.service.DISMISS"
        const val ACTION_SNOOZE = "com.wecker.app.service.SNOOZE"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        /** Broadcast an die RingActivity, damit sie sich nach dem Beenden schließt. */
        const val ACTION_DISMISSED = "com.wecker.app.ALARM_DISMISSED"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        when (intent?.action) {
            ACTION_START -> if (alarmId >= 0) startRinging(alarmId)
            ACTION_DISMISS -> if (alarmId >= 0) dismiss(alarmId)
            ACTION_SNOOZE -> if (alarmId >= 0) {
                snooze(alarmId, intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10))
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRinging(alarmId: Long) {
        acquireWakeLock()
        scope.launch {
            val repository = AlarmRepository(applicationContext)
            val alarm = repository.getById(alarmId) ?: run { stopSelf(); return@launch }

            val notification = NotificationHelper.buildRingNotification(applicationContext, alarm)
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NotificationHelper.ringNotificationId(alarm.id),
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NotificationHelper.ringNotificationId(alarm.id), notification)
            }
            playSound(alarm.ringtoneUri)
            startVibration()
        }
    }

    private fun dismiss(alarmId: Long) {
        stopSoundAndVibration()
        NotificationHelper.cancelRingNotification(applicationContext, alarmId)
        // RingActivity benachrichtigen, damit sie sich schließt.
        sendBroadcast(Intent(ACTION_DISMISSED).setPackage(packageName))

        scope.launch {
            val repository = AlarmRepository(applicationContext)
            val alarm = repository.getById(alarmId)
            if (alarm != null && alarm.wakeCheckMinutes > 0) {
                // Aufwachkontrolle: erneutes Klingeln einplanen, bis der Nutzer
                // über die Benachrichtigung endgültig deaktiviert.
                val triggerAt = AlarmScheduler.scheduleWakeCheck(
                    applicationContext, alarmId, alarm.wakeCheckMinutes
                )
                NotificationHelper.showWakeCheckNotification(applicationContext, alarm, triggerAt)
            } else {
                repository.rescheduleNext(alarmId)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** Snooze: Klingeln stoppen und in [minutes] Minuten erneut wecken. */
    private fun snooze(alarmId: Long, minutes: Int) {
        stopSoundAndVibration()
        NotificationHelper.cancelRingNotification(applicationContext, alarmId)
        sendBroadcast(Intent(ACTION_DISMISSED).setPackage(packageName))
        scope.launch {
            val alarm = AlarmRepository(applicationContext).getById(alarmId)
            if (alarm != null) {
                val triggerAt = AlarmScheduler.scheduleWakeCheck(applicationContext, alarmId, minutes)
                NotificationHelper.showWakeCheckNotification(applicationContext, alarm, triggerAt)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun playSound(ringtoneUri: String?) {
        stopSound()
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // Eigener Klingelton, falls gesetzt und abspielbar – sonst Standard.
        val uri = ringtoneUri
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?.takeIf { custom ->
                runCatching {
                    applicationContext.contentResolver.openAssetFileDescriptor(custom, "r")?.close()
                }.getOrNull() != null
            }
            ?: defaultUri
        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 600, 400)
        if (Build.VERSION.SDK_INT >= 31) {
            val vm = getSystemService(VibratorManager::class.java)
            vm?.vibrate(
                android.os.CombinedVibration.createParallel(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            )
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as? Vibrator)
                ?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private fun stopSound() {
        mediaPlayer?.runCatching { if (isPlaying) stop(); release() }
        mediaPlayer = null
    }

    /** Schaltet das Display ein und hält die CPU wach, bis beendet wird (max. 10 Min). */
    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        wakeLock?.release()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "wecker:alarm"
        ).apply { acquire(10 * 60 * 1000L) }
    }

    private fun releaseWakeLock() {
        wakeLock?.runCatching { if (isHeld) release() }
        wakeLock = null
    }

    private fun stopSoundAndVibration() {
        stopSound()
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.cancel()
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.cancel()
        }
    }

    override fun onDestroy() {
        stopSoundAndVibration()
        scope.cancel()
        super.onDestroy()
    }
}
