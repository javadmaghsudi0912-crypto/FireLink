package com.firelink.app
import android.content.Context
import android.content.Intent
import android.net.Uri
object ShareUtils {
    fun smsBody(i:Incident)= "FIRELINE INCIDENT\nID: ${i.id}\nUnit: ${i.unitName}\nLat: ${i.latitude}\nLon: ${i.longitude}\nMap: https://maps.google.com/?q=${i.latitude},${i.longitude}"
    fun openSms(context:Context,number:String,i:Incident){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:${Uri.encode(number)}")).putExtra("sms_body",smsBody(i)))}
    fun openMap(context:Context,i:Incident){context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("geo:${i.latitude},${i.longitude}?q=${i.latitude},${i.longitude}")))}
}
