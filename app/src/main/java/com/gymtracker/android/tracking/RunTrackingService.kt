package com.gymtracker.android.tracking

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.gymtracker.android.GymTrackerApplication
import com.gymtracker.android.MainActivity
import com.gymtracker.android.R
import com.gymtracker.android.domain.model.DistanceUnit
import com.gymtracker.android.domain.service.RunCalculator
import kotlinx.coroutines.*
import java.util.Locale

class RunTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller get() = (application as GymTrackerApplication).runController
    private var tts: TextToSpeech? = null
    private var speechReady = false
    private var recording: Job? = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("runs", "Run tracking", NotificationManager.IMPORTANCE_LOW))
        tts = TextToSpeech(this) { result ->
            speechReady = result == TextToSpeech.SUCCESS
            if (speechReady) {
                tts?.language = Locale.getDefault()
                tts?.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(this, 2001, notification("Preparing run…"),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
        } catch (e: Exception) {
            controller.report("Couldn't start background tracking: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
        if (recording?.isActive != true) recording = scope.launch {
            val unit = DistanceUnit.entries.firstOrNull { it.name == intent?.getStringExtra("unit") } ?: DistanceUnit.MILES
            val started = controller.activate(unit, intent?.getDoubleExtra("goal", 0.0)?.takeIf { it > 0 },
                intent?.getBooleanExtra("audio", false) == true) { text ->
                if (speechReady) tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "run-${System.nanoTime()}")
            }
            if (!started) { stopSelf(); return@launch }
            launch {
                controller.state.collect { state ->
                    state.run?.let { run ->
                        getSystemService(NotificationManager::class.java).notify(2001, notification(
                            "${RunCalculator.duration(run.elapsedMillis)} • ${RunCalculator.distance(run.distanceMeters, run.unit)}"))
                    }
                }
            }
            try {
                AndroidLocationSource(this@RunTrackingService).updates().collect { update ->
                    when (update) {
                        is LocationUpdate.Fix -> update.location.let { location ->
                            controller.onLocation(LocationSample(location.latitude, location.longitude,
                                location.elapsedRealtimeNanos / 1_000_000,
                                if (location.hasAccuracy()) location.accuracy else Float.NaN))
                        }
                        is LocationUpdate.Status -> controller.status(update.message)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                controller.report("Location tracking interrupted: ${e.message}")
                stopSelf()
            }
        }
        // Recovery is initiated from the visible app, avoiding background-start permission violations.
        return START_NOT_STICKY
    }
    private fun notification(text: String): Notification {
        val open = Intent(this, MainActivity::class.java).putExtra("openRun", true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return NotificationCompat.Builder(this, "runs")
            .setSmallIcon(R.drawable.ic_run_notification).setContentTitle("Run in progress")
            .setContentText(text).setOngoing(true).setOnlyAlertOnce(true)
            .setContentIntent(PendingIntent.getActivity(this, 2001, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        scope.cancel()
        tts?.stop(); tts?.shutdown()
        controller.detached()
        super.onDestroy()
    }
}
