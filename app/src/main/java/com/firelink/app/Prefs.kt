package com.firelink.app
import android.content.Context
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("firelink_prefs", Context.MODE_PRIVATE)
    var teamId:String get()=sp.getString("team_id","")?:""; set(v){sp.edit().putString("team_id",v.trim()).apply()}
    var unitName:String get()=sp.getString("unit_name","")?:""; set(v){sp.edit().putString("unit_name",v.trim()).apply()}
    var smsNumber:String get()=sp.getString("sms_number","")?:""; set(v){sp.edit().putString("sms_number",v.trim()).apply()}
}
