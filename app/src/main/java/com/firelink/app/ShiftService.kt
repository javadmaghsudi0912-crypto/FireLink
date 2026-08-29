package com.firelink.app

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.database.ValueEventListener

class ShiftService : Service() {
    private var listener: ValueEventListener? = null
    private val repo = FireRepository()
    private var lastSeenId: String? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            1001,
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle("FireLink فعال است")
                .setContentText("دریافت هشدارهای گروه در شیفت فعال")
                .setOngoing(true)
                .build()
        )
        val teamId = Prefs(this).teamId
        if (teamId.isNotBlank() && FirebaseProvider.auth.currentUser != null) {
            listener = repo.observeIncidents(teamId, { list ->
                val latest = list.firstOrNull() ?: return@observeIncidents
                if (lastSeenId == null) {
                    lastSeenId = latest.id
                } else if (lastSeenId != latest.id) {
                    lastSeenId = latest.id
                    showIncident(latest)
                }
            }, {})
        }
    }

    private fun showIncident(i: Incident) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pending = PendingIntent.getActivity(
            this, 2002, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("موقعیت جدید حادثه")
            .setContentText("${i.unitName} — ${i.latitude}, ${i.longitude}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        getSystemService(NotificationManager::class.java).notify(i.id.hashCode(), n)
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "FireLink Shift", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL = "firelink_shift"
    }
}
