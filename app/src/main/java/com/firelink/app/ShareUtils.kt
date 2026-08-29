package com.firelink.app

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtils {
    fun smsBody(i: Incident): String = buildString {
        appendLine("FIRELINK INCIDENT")
        appendLine("ID: ${i.id}")
        appendLine("Unit: ${i.unitName}")
        appendLine("Lat: ${i.latitude}")
        appendLine("Lon: ${i.longitude}")
        if (i.note.isNotBlank()) appendLine("Note: ${i.note}")
        appendLine("Map: https://maps.google.com/?q=${i.latitude},${i.longitude}")
        append("App: firelink://incident?id=${Uri.encode(i.id)}&lat=${i.latitude}&lon=${i.longitude}")
    }

    fun openSms(context: Context, number: String, i: Incident) {
        val uri = Uri.parse("smsto:${Uri.encode(number)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", smsBody(i))
        }
        context.startActivity(intent)
    }

    fun openMap(context: Context, i: Incident) {
        val geo = Uri.parse("geo:${i.latitude},${i.longitude}?q=${i.latitude},${i.longitude}")
        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
    }
}
