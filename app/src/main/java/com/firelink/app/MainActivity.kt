package com.firelink.app

import android.Manifest
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { FireLineScreen() } }
    }

    @Composable
    private fun FireLineScreen() {
        val configured = remember { FirebaseProvider.configured() }
        if (!configured) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("FireLine", style = MaterialTheme.typography.headlineLarge)
                Text("تنظیمات Firebase هنوز وارد نشده است.")
            }
            return
        }

        var signedIn by remember { mutableStateOf(FirebaseProvider.auth.currentUser != null) }
        if (!signedIn) {
            LoginScreen { signedIn = true }
        } else {
            HomeScreen {
                FirebaseProvider.auth.signOut()
                signedIn = false
            }
        }
    }

    @Composable
    private fun LoginScreen(onSuccess: () -> Unit) {
        val scope = rememberCoroutineScope()
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ورود به FireLine", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(email, { email = it }, label = { Text("ایمیل") })
            OutlinedTextField(password, { password = it }, label = { Text("رمز عبور") })
            Button(onClick = {
                scope.launch {
                    runCatching { FireRepository().signIn(email, password) }
                        .onSuccess { onSuccess() }
                        .onFailure { message = it.message ?: "خطا در ورود" }
                }
            }) { Text("ورود") }
            if (message.isNotBlank()) Text(message)
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

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (!fine && !coarse) status = "مجوز موقعیت لازم است."
        }

        LaunchedEffect(teamId) {
            if (teamId.isNotBlank()) {
                repo.observeIncidents(
                    teamId,
                    { incidents = it },
                    { status = it }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FireLine — اشتراک موقعیت عملیات", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(teamId, { teamId = it; prefs.teamId = it }, label = { Text("شناسه گروه") })
                    OutlinedTextField(unitName, { unitName = it; prefs.unitName = it }, label = { Text("نام واحد / خودرو") })
                    OutlinedTextField(smsNumber, { smsNumber = it; prefs.smsNumber = it }, label = { Text("شماره مقصد SMS") })
                    OutlinedTextField(note, { note = it }, label = { Text("توضیح حادثه") })

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val permissions = buildList {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                            }.toTypedArray()
                            permissionLauncher.launch(permissions)
                        }) { Text("مجوزها") }

                        Button(onClick = {
                            if (!shiftActive) {
                                ContextCompat.startForegroundService(
                                    this@MainActivity,
                                    Intent(this@MainActivity, ShiftService::class.java)
                                )
                                shiftActive = true
                            } else {
                                stopService(Intent(this@MainActivity, ShiftService::class.java))
                                shiftActive = false
                            }
                        }) {
                            Text(if (shiftActive) "پایان شیفت" else "شروع شیفت")
                        }
                    }

                    Button(
                        enabled = teamId.isNotBlank() && unitName.isNotBlank(),
                        onClick = {
                            val hasLocation =
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                            if (!hasLocation) {
                                status = "ابتدا مجوز موقعیت را بدهید."
                            } else {
                                scope.launch {
                                    status = "در حال دریافت GPS..."
                                    runCatching {
                                        val location = locationHelper.current()
                                        repo.sendIncident(
                                            teamId,
                                            Incident(
                                                latitude = location.latitude,
                                                longitude = location.longitude,
                                                accuracyM = location.accuracy.toDouble(),
                                                note = note,
                                                unitName = unitName
                                            )
                                        )
                                    }.onSuccess {
                                        status = "موقعیت ارسال شد."
                                        note = ""
                                    }.onFailure {
                                        status = it.message ?: "ارسال ناموفق"
                                    }
                                }
                            }
                        }
                    ) {
                        Text("ارسال موقعیت فعلی حادثه")
                    }

                    if (status.isNotBlank()) Text(status)
                    HorizontalDivider()
                    Text("آخرین موقعیت‌ها", style = MaterialTheme.typography.titleMedium)
                }
            }

            items(incidents, key = { it.id }) { incident ->
                Card {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("${incident.unitName} — ${incident.status}")
                        Text("${incident.latitude}, ${incident.longitude}")
                        Text("دقت GPS: ${incident.accuracyM.toInt()} متر")
                        if (incident.note.isNotBlank()) Text(incident.note)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { ShareUtils.openMap(this@MainActivity, incident) }) {
                                Text("نقشه")
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { repo.acknowledge(teamId, incident.id, unitName) }
                                        .onSuccess { status = "دریافت تأیید شد." }
                                        .onFailure { status = it.message ?: "خطا" }
                                }
                            }) { Text("دریافت شد") }
                            TextButton(onClick = { ShareUtils.openSms(this@MainActivity, smsNumber, incident) }) {
                                Text("SMS")
                            }
                        }
                    }
                }
            }

            item {
                TextButton(onClick = onSignOut) { Text("خروج") }
            }
        }
    }
}
