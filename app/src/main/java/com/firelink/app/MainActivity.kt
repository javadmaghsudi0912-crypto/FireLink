package com.firelink.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { FireLinkScreen() } }
    }

    @Composable
    private fun FireLinkScreen() {
        val configured = remember { FirebaseProvider.configured() }
        if (!configured) {
            SetupMissing()
            return
        }

        var signedIn by remember { mutableStateOf(FirebaseProvider.auth.currentUser != null) }
        if (!signedIn) {
            Login { signedIn = true }
            return
        }

        Home(onSignOut = {
            FirebaseProvider.auth.signOut()
            signedIn = false
        })
    }

    @Composable
    private fun SetupMissing() {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("FireLink", style = MaterialTheme.typography.headlineLarge)
                Text("تنظیمات Firebase هنوز وارد نشده است.")
                Text("فایل local.properties.example و docs/SETUP_FA.md داخل پروژه را ببینید.")
            }
        }
    }

    @Composable
    private fun Login(onSuccess: () -> Unit) {
        val scope = rememberCoroutineScope()
        var email by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ورود به FireLink", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(email, { email = it }, label = { Text("ایمیل سازمانی") })
            OutlinedTextField(pass, { pass = it }, label = { Text("رمز عبور") })
            Button(onClick = {
                scope.launch {
                    runCatching { FireRepository().signIn(email, pass) }
                        .onSuccess { onSuccess() }
                        .onFailure { message = it.message ?: "خطا در ورود" }
                }
            }) { Text("ورود") }
            if (message.isNotBlank()) Text(message)
        }
    }

    @Composable
    private fun Home(onSignOut: () -> Unit) {
        val prefs = remember { Prefs(this) }
        val repo = remember { FireRepository() }
        val loc = remember { LocationHelper(this) }
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
            if (result[Manifest.permission.ACCESS_FINE_LOCATION] != true &&
                result[Manifest.permission.ACCESS_COARSE_LOCATION] != true) {
                status = "مجوز موقعیت لازم است."
            }
        }

        LaunchedEffect(teamId) {
            if (teamId.isNotBlank()) {
                repo.observeIncidents(teamId, { incidents = it }, { status = it })
            }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("FireLink — اشتراک موقعیت عملیات", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(teamId, {
                    teamId = it
                    prefs.teamId = it
                }, label = { Text("شناسه گروه") })
                OutlinedTextField(unitName, {
                    unitName = it
                    prefs.unitName = it
                }, label = { Text("نام واحد / خودرو") })
                OutlinedTextField(smsNumber, {
                    smsNumber = it
                    prefs.smsNumber = it
                }, label = { Text("شماره مقصد SMS اضطراری") })
                OutlinedTextField(note, { note = it }, label = { Text("توضیح حادثه") })

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val perms = buildList {
                            add(Manifest.permission.ACCESS_FINE_LOCATION)
                            add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                        }.toTypedArray()
                        permissionLauncher.launch(perms)
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
                    }) { Text(if (shiftActive) "پایان شیفت" else "شروع شیفت") }
                }

                Button(
                    enabled = teamId.isNotBlank() && unitName.isNotBlank(),
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(
                                this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                        if (!granted) {
                            status = "ابتدا مجوز موقعیت را بدهید."
                        } else {
                            scope.launch {
                                status = "در حال دریافت GPS..."
                                runCatching {
                                    val l = loc.current()
                                    repo.sendIncident(
                                        teamId,
                                        Incident(
                                            latitude = l.latitude,
                                            longitude = l.longitude,
                                            accuracyM = l.accuracy.toDouble(),
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
                ) { Text("ارسال موقعیت فعلی حادثه") }

                if (status.isNotBlank()) Text(status)
                HorizontalDivider()
                Text("آخرین موقعیت‌ها", style = MaterialTheme.typography.titleMedium)
            }

            items(incidents, key = { it.id }) { i ->
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${i.unitName} — ${i.status}")
                        Text("${i.latitude}, ${i.longitude}")
                        Text("دقت GPS: ${i.accuracyM.toInt()} متر")
                        if (i.note.isNotBlank()) Text(i.note)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { ShareUtils.openMap(this@MainActivity, i) }) {
                                Text("نقشه")
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { repo.acknowledge(teamId, i.id, unitName) }
                                        .onSuccess { status = "دریافت تأیید شد." }
                                        .onFailure { status = it.message ?: "خطا" }
                                }
                            }) { Text("دریافت شد") }
                            TextButton(onClick = {
                                ShareUtils.openSms(this@MainActivity, smsNumber, i)
                            }) { Text("SMS پشتیبان") }
                        }
                    }
                }
            }

            item {
                TextButton(onClick = onSignOut) { Text("خروج از حساب") }
            }
        }
    }
}
