package com.firelink.app
import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.database.ValueEventListener
class ShiftService:Service(){
    private var listener:ValueEventListener?=null
    private val repo=FireRepository(); private var lastSeenId:String?=null
    override fun onCreate(){super.onCreate(); createChannel(); startForeground(1001,NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle("FireLine فعال است").setContentText("دریافت هشدارهای گروه در شیفت فعال").setOngoing(true).build()); val team=Prefs(this).teamId; if(team.isNotBlank()&&FirebaseProvider.auth.currentUser!=null){listener=repo.observeIncidents(team,{list->val x=list.firstOrNull()?:return@observeIncidents;if(lastSeenId==null)lastSeenId=x.id else if(lastSeenId!=x.id){lastSeenId=x.id;showIncident(x)}},{})}}
    private fun showIncident(i:Incident){val launch=packageManager.getLaunchIntentForPackage(packageName);val p=PendingIntent.getActivity(this,2002,launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val n=NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("موقعیت جدید حادثه").setContentText("${i.unitName} — ${i.latitude}, ${i.longitude}").setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(p).build();getSystemService(NotificationManager::class.java).notify(i.id.hashCode(),n)}
    private fun createChannel(){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"FireLine Shift",NotificationManager.IMPORTANCE_HIGH))}
    override fun onBind(intent:Intent?):IBinder?=null
    companion object{const val CHANNEL="firelink_shift"}
}
