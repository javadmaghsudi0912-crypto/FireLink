package com.firelink.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

private val FireRed = Color(0xFFD21F26)
private val SoftRed = Color(0xFFFFF3F3)
private val Ink = Color(0xFF202124)
private val Muted = Color(0xFF6F7378)
private val FireLinkColors = lightColorScheme(
    primary = FireRed,
    onPrimary = Color.White,
    primaryContainer = SoftRed,
    onPrimaryContainer = Ink,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF7F7F8),
    outline = Color(0xFFD6D7DA)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = FireLinkColors) { FireLineScreen() }
        }
    }

    @Composable
    private fun FireLineScreen() {
        val configured = remember { FirebaseProvider.configured() }
        if (!configured) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrandHeader()
                Text("تنظیمات Firebase هنوز وارد نشده است.")
            }
            return
        }
        var signedIn by remember { mutableStateOf(FirebaseProvider.auth.currentUser != null) }
        if (!signedIn) LoginScreen { signedIn = true }
        else HomeScreen {
            FirebaseProvider.auth.signOut()
            signedIn = false
        }
    }

    @Composable
    private fun BrandHeader() {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("FireLink", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("GM", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = FireRed)
            }
            Text("اشتراک موقعیت عملیات", color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }

    @Composable
    private fun LoginScreen(onSuccess: () -> Unit) {
        val scope = rememberCoroutineScope()
        val repo = remember { FireRepository() }
        var registerMode by remember { mutableStateOf(false) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var displayName by remember { mutableStateOf("") }
        var teamId by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BrandHeader()
            Text(if (registerMode) "ثبت‌نام عضو جدید" else "ورود اعضای تیم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            if (registerMode) {
                OutlinedTextField(displayName, { displayName = it }, label = { Text("نام و نام خانوادگی") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(teamId, { teamId = it }, label = { Text("شناسه گروه موردنظر") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            }

            OutlinedTextField(email, { email = it }, label = { Text("ایمیل") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            OutlinedTextField(password, { password = it }, label = { Text("رمز عبور") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp))
            if (registerMode) {
                OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text("تکرار رمز عبور") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp))
                Text("برای امنیت، ثبت‌نام به معنی عضویت خودکار در تیم نیست. مدیر باید UID شما را تأیید کند.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    scope.launch {
                        message = ""
                        if (!registerMode) {
                            runCatching { repo.signIn(email, password) }
                                .onSuccess { onSuccess() }
                                .onFailure { message = it.message ?: "خطا در ورود" }
                        } else {
                            when {
                                displayName.trim().length < 2 -> message = "نام را کامل وارد کنید."
                                teamId.trim().isBlank() -> message = "شناسه گروه را وارد کنید."
                                !email.contains("@") -> message = "ایمیل معتبر وارد کنید."
                                password.length < 8 -> message = "رمز عبور باید حداقل ۸ کاراکتر باشد."
                                password != confirmPassword -> message = "تکرار رمز عبور یکسان نیست."
                                else -> runCatching { repo.registerUser(email, password) }
                                    .onSuccess { uid ->
                                        message = "حساب ساخته شد. برای عضویت در ${teamId.trim()} این UID را برای مدیر بفرستید: ${uid}"
                                        password = ""
                                        confirmPassword = ""
                                    }
                                    .onFailure { message = it.message ?: "ثبت‌نام ناموفق بود" }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (registerMode) "ساخت حساب" else "ورود") }

            OutlinedButton(
                onClick = { registerMode = !registerMode; message = "" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (registerMode) "حساب دارم — ورود" else "عضو جدید هستم — ثبت‌نام") }

            if (message.isNotBlank()) Text(message, color = if (message.startsWith("حساب ساخته شد")) Color(0xFF268A45) else FireRed)
        }
    }

    @Composable
    private fun HomeScreen(onSignOut: () -> Unit) {
        val prefs = remember { Prefs(this) }
        val repo = remember { FireRepository() }
        val locationHelper = remember { LocationHelper(this) }
        val scope = rememberCoroutineScope()
        var teamId by remember { mutableStateOf(prefs.teamId) }
        var unitName by remember { mutableStateOf(prefs.unitName) }
        var smsNumber by remember { mutableStateOf(prefs.smsNumber) }
        var note by remember { mutableStateOf("") }
        var incidents by remember { mutableStateOf(emptyList<Incident>()) }
        var status by remember { mutableStateOf("") }
        var shiftActive by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (!fine && !coarse) status = "مجوز موقعیت لازم است."
        }

        val mapPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val lat = result.data?.getDoubleExtra("latitude", Double.NaN) ?: Double.NaN
                val lon = result.data?.getDoubleExtra("longitude", Double.NaN) ?: Double.NaN
                if (!lat.isNaN() && !lon.isNaN()) {
                    scope.launch {
                        status = "در حال ارسال نقطه انتخابی..."
                        runCatching { repo.sendIncident(teamId, Incident(latitude = lat, longitude = lon, accuracyM = 0.0, note = note, unitName = unitName)) }
                            .onSuccess { status = "نقطه انتخابی برای تیم ارسال شد."; note = "" }
                            .onFailure { status = it.message ?: "ارسال نقطه ناموفق" }
                    }
                }
            }
        }

        LaunchedEffect(teamId) {
            if (teamId.isNotBlank()) repo.observeIncidents(teamId, { incidents = it }, { status = it })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrandHeader()
                    OutlinedTextField(teamId, { teamId = it; prefs.teamId = it }, label = { Text("شناسه گروه") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(unitName, { unitName = it; prefs.unitName = it }, label = { Text("نام واحد / خودرو") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(smsNumber, { smsNumber = it; prefs.smsNumber = it }, label = { Text("شماره مقصد SMS (اختیاری)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(note, { note = it }, label = { Text("توضیح حادثه (اختیاری)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val permissions = buildList {
                                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                                }.toTypedArray()
                                permissionLauncher.launch(permissions)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("مجوزها") }
                        OutlinedButton(
                            onClick = {
                                if (!shiftActive) {
                                    ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, ShiftService::class.java))
                                    shiftActive = true
                                } else {
                                    stopService(Intent(this@MainActivity, ShiftService::class.java))
                                    shiftActive = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (shiftActive) "پایان شیفت" else "شروع شیفت") }
                    }

                    OutlinedButton(
                        enabled = teamId.isNotBlank() && unitName.isNotBlank(),
                        onClick = {
                            scope.launch {
                                val intent = Intent(this@MainActivity, MapPickerActivity::class.java)
                                val hasLocation = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasLocation) runCatching { locationHelper.current() }.getOrNull()?.let { loc ->
                                    intent.putExtra("startLat", loc.latitude)
                                    intent.putExtra("startLon", loc.longitude)
                                }
                                mapPickerLauncher.launch(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FireRed)
                    ) { Text("انتخاب نقطه روی نقشه و ارسال") }

                    Button(
                        enabled = teamId.isNotBlank() && unitName.isNotBlank(),
                        onClick = {
                            val hasLocation = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (!hasLocation) status = "ابتدا مجوز موقعیت را بدهید."
                            else scope.launch {
                                status = "در حال دریافت GPS..."
                                runCatching {
                                    val location = locationHelper.current()
                                    repo.sendIncident(teamId, Incident(latitude = location.latitude, longitude = location.longitude, accuracyM = location.accuracy.toDouble(), note = note, unitName = unitName))
                                }.onSuccess { status = "موقعیت ارسال شد."; note = "" }
                                    .onFailure { status = it.message ?: "ارسال ناموفق" }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("ارسال موقعیت فعلی") }

                    if (status.isNotBlank()) {
                        Text(status, color = if (status.contains("خطا") || status.contains("ناموفق")) FireRed else Color(0xFF268A45), modifier = Modifier.fillMaxWidth())
                    }
                    HorizontalDivider(color = Color(0xFFE5E5E7))
                    Text("آخرین موقعیت‌ها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            items(incidents, key = { it.id }) { incident ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F9))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(incident.unitName, fontWeight = FontWeight.Bold)
                            Text(incident.status, color = FireRed, fontWeight = FontWeight.Bold)
                        }
                        Text("%.6f, %.6f".format(incident.latitude, incident.longitude), color = Ink)
                        Text("دقت GPS: ${incident.accuracyM.toInt()} متر", color = Muted)
                        if (incident.note.isNotBlank()) Text(incident.note)
                        HorizontalDivider(color = Color(0xFFE4E4E6))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { ShareUtils.openMap(this@MainActivity, incident) }) { Text("نقشه", color = FireRed) }
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { repo.acknowledge(teamId, incident.id, unitName) }
                                        .onSuccess { status = "دریافت تأیید شد." }
                                        .onFailure { status = it.message ?: "خطا" }
                                }
                            }) { Text("دریافت شد") }
                            TextButton(onClick = { ShareUtils.openSms(this@MainActivity, smsNumber, incident) }) { Text("SMS", color = FireRed) }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onSignOut) { Text("خروج") }
                    Text("Designed by  GM", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, color = Muted, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
