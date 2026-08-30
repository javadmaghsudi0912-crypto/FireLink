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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
private val Success = Color(0xFF268A45)

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
        setContent { MaterialTheme(colorScheme = FireLinkColors) { FireLineScreen() } }
    }

    @Composable
    private fun FireLineScreen() {
        if (!remember { FirebaseProvider.configured() }) {
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
    private fun BrandHeader() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SoftRed)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(FireRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("GM", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("FireLink", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("GM", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = FireRed)
                    }
                    Text("سامانه اشتراک موقعیت عملیات", color = Muted)
                }
            }
        }
    }

    @Composable
    private fun LoginScreen(onSuccess: () -> Unit) {
        val scope = rememberCoroutineScope()
        val repo = remember { FireRepository() }
        var mode by remember { mutableStateOf("login") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var displayName by remember { mutableStateOf("") }
        var teamCode by remember { mutableStateOf("") }
        var teamName by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var success by remember { mutableStateOf(false) }

        fun clearMessage() { message = ""; success = false }

        LazyColumn(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { BrandHeader() }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Text(
                            when (mode) {
                                "join" -> "عضویت در تیم موجود"
                                "create" -> "ساخت تیم جدید"
                                else -> "ورود به FireLink GM"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            when (mode) {
                                "join" -> "کد تیم را وارد کنید؛ درخواست تا زمان تصمیم مدیر باقی می‌ماند."
                                "create" -> "یک تیم مستقل با کد یکتا ساخته می‌شود و شما مدیر آن خواهید بود."
                                else -> "با حساب خود وارد شوید."
                            },
                            color = Muted
                        )

                        if (mode != "login") {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text(if (mode == "create") "نام مدیر" else "نام و نام خانوادگی") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        if (mode == "join") {
                            OutlinedTextField(
                                value = teamCode,
                                onValueChange = { teamCode = it },
                                label = { Text("کد تیم") },
                                supportingText = { Text("مثال: FL-8K4P2Q یا کد تیم قدیمی") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        if (mode == "create") {
                            OutlinedTextField(
                                value = teamName,
                                onValueChange = { teamName = it },
                                label = { Text("نام تیم / ایستگاه") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("ایمیل") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("رمز عبور") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        if (mode != "login") {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("تکرار رمز عبور") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    clearMessage()
                                    when (mode) {
                                        "login" -> runCatching { repo.signIn(email, password) }
                                            .onSuccess { onSuccess() }
                                            .onFailure { message = it.message ?: "خطا در ورود" }

                                        "join" -> when {
                                            displayName.trim().length < 2 -> message = "نام را کامل وارد کنید."
                                            teamCode.trim().isBlank() -> message = "کد تیم را وارد کنید."
                                            !email.contains("@") -> message = "ایمیل معتبر وارد کنید."
                                            password.length < 8 -> message = "رمز عبور باید حداقل ۸ کاراکتر باشد."
                                            password != confirmPassword -> message = "تکرار رمز عبور یکسان نیست."
                                            else -> runCatching {
                                                repo.registerUser(email, password, displayName, teamCode)
                                            }.onSuccess {
                                                message = "حساب ساخته شد و درخواست عضویت برای مدیر ارسال شد."
                                                success = true
                                                password = ""
                                                confirmPassword = ""
                                            }.onFailure { message = it.message ?: "ثبت‌نام ناموفق بود" }
                                        }

                                        "create" -> when {
                                            displayName.trim().length < 2 -> message = "نام مدیر را کامل وارد کنید."
                                            teamName.trim().length < 2 -> message = "نام تیم را کامل وارد کنید."
                                            !email.contains("@") -> message = "ایمیل معتبر وارد کنید."
                                            password.length < 8 -> message = "رمز عبور باید حداقل ۸ کاراکتر باشد."
                                            password != confirmPassword -> message = "تکرار رمز عبور یکسان نیست."
                                            else -> runCatching {
                                                repo.registerAdminAndCreateTeam(email, password, displayName, teamName)
                                            }.onSuccess { onSuccess() }
                                                .onFailure { message = it.message ?: "ساخت تیم ناموفق بود" }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                when (mode) {
                                    "join" -> "ثبت‌نام و ارسال درخواست"
                                    "create" -> "ساخت تیم و ورود مدیر"
                                    else -> "ورود"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (mode == "join") {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        clearMessage()
                                        when {
                                            displayName.trim().length < 2 -> message = "نام را کامل وارد کنید."
                                            teamCode.trim().isBlank() -> message = "کد تیم را وارد کنید."
                                            email.isBlank() || password.isBlank() -> message = "ایمیل و رمز را وارد کنید."
                                            else -> runCatching {
                                                repo.requestJoinExisting(email, password, displayName, teamCode)
                                            }.onSuccess {
                                                message = "درخواست عضویت برای مدیر ارسال شد."
                                                success = true
                                            }.onFailure { message = it.message ?: "ارسال درخواست ناموفق بود" }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("حسابم از قبل ساخته شده — فقط درخواست بفرست") }
                        }

                        HorizontalDivider()
                        OutlinedButton(
                            onClick = { mode = "login"; clearMessage() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("ورود به حساب") }
                        OutlinedButton(
                            onClick = { mode = "join"; clearMessage() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("عضویت در تیم موجود") }
                        OutlinedButton(
                            onClick = { mode = "create"; clearMessage() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("ساخت تیم جدید برای مدیر") }

                        if (message.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (success) Color(0xFFF0FAF3) else SoftRed)
                            ) {
                                Text(
                                    message,
                                    modifier = Modifier.padding(12.dp),
                                    color = if (success) Success else FireRed
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "GM • Fire & Rescue Technology",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    textAlign = TextAlign.Center,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    @Composable
    private fun HomeScreen(onSignOut: () -> Unit) {
        val prefs = remember { Prefs(this) }
        val repo = remember { FireRepository() }
        val locationHelper = remember { LocationHelper(this) }
        val scope = rememberCoroutineScope()

        var teams by remember { mutableStateOf(emptyList<TeamSummary>()) }
        var selectedTeamId by remember { mutableStateOf(prefs.teamId) }
        val selectedTeam = teams.firstOrNull { it.teamId == selectedTeamId }
        val isAdmin = selectedTeam?.role == "admin"

        var unitName by remember { mutableStateOf(prefs.unitName) }
        var smsNumber by remember { mutableStateOf(prefs.smsNumber) }
        var note by remember { mutableStateOf("") }
        var incidents by remember { mutableStateOf(emptyList<Incident>()) }
        var joinRequests by remember { mutableStateOf(emptyList<JoinRequest>()) }
        var status by remember { mutableStateOf("") }
        var shiftActive by remember { mutableStateOf(false) }
        var loadingTeams by remember { mutableStateOf(true) }
        var showCreateTeam by remember { mutableStateOf(false) }
        var newTeamName by remember { mutableStateOf("") }
        var managerName by remember { mutableStateOf("") }

        suspend fun refreshTeams() {
            loadingTeams = true
            runCatching {
                repo.ensureLegacyMembershipIndex(listOf(prefs.teamId, "team-001"))
                repo.loadMyTeams()
            }.onSuccess { loaded ->
                teams = loaded
                val preferred = loaded.firstOrNull { it.teamId == selectedTeamId }
                    ?: loaded.firstOrNull { it.teamId == prefs.teamId }
                    ?: loaded.firstOrNull()
                selectedTeamId = preferred?.teamId ?: ""
                prefs.teamId = selectedTeamId
            }.onFailure { status = it.message ?: "دریافت تیم‌ها ناموفق بود." }
            loadingTeams = false
        }

        LaunchedEffect(Unit) { refreshTeams() }

        DisposableEffect(selectedTeamId, isAdmin) {
            if (selectedTeamId.isBlank()) {
                incidents = emptyList()
                joinRequests = emptyList()
                onDispose { }
            } else {
                val incidentListener = repo.observeIncidents(selectedTeamId, { incidents = it }, { status = it })
                val joinListener = if (isAdmin) {
                    repo.observeJoinRequests(
                        selectedTeamId,
                        { joinRequests = it },
                        { status = "خطا در دریافت درخواست‌ها: $it" }
                    )
                } else null
                if (!isAdmin) joinRequests = emptyList()

                onDispose {
                    repo.removeIncidentsListener(selectedTeamId, incidentListener)
                    if (joinListener != null) repo.removeJoinRequestsListener(selectedTeamId, joinListener)
                }
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (!fine && !coarse) status = "مجوز موقعیت لازم است."
        }

        val mapPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val lat = result.data?.getDoubleExtra("latitude", Double.NaN) ?: Double.NaN
                val lon = result.data?.getDoubleExtra("longitude", Double.NaN) ?: Double.NaN
                if (!lat.isNaN() && !lon.isNaN() && selectedTeamId.isNotBlank()) {
                    scope.launch {
                        status = "در حال ارسال نقطه انتخابی..."
                        runCatching {
                            repo.sendIncident(
                                selectedTeamId,
                                Incident(latitude = lat, longitude = lon, accuracyM = 0.0, note = note, unitName = unitName)
                            )
                        }.onSuccess {
                            status = "نقطه انتخابی برای تیم ارسال شد."
                            note = ""
                        }.onFailure { status = it.message ?: "ارسال نقطه ناموفق" }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BrandHeader() }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F9))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("تیم‌های من", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        when {
                            loadingTeams -> Text("در حال دریافت تیم‌ها...", color = Muted)
                            teams.isEmpty() -> {
                                Text("هنوز عضویت فعالی برای این حساب پیدا نشد.", color = Muted)
                                Text("اگر درخواست فرستاده‌اید، پس از تأیید مدیر دوباره وارد شوید.", color = Muted)
                            }
                            else -> teams.forEach { team ->
                                OutlinedButton(
                                    onClick = {
                                        selectedTeamId = team.teamId
                                        prefs.teamId = team.teamId
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            if (team.teamId == selectedTeamId) "✓ ${team.name}" else team.name,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "کد: ${team.code} • ${if (team.role == "admin") "مدیر" else "عضو"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Muted
                                        )
                                    }
                                }
                            }
                        }

                        TextButton(
                            onClick = { showCreateTeam = !showCreateTeam },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (showCreateTeam) "بستن ساخت تیم" else "ساخت تیم جدید با همین حساب") }

                        if (showCreateTeam) {
                            OutlinedTextField(
                                managerName, { managerName = it },
                                label = { Text("نام مدیر") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            OutlinedTextField(
                                newTeamName, { newTeamName = it },
                                label = { Text("نام تیم / ایستگاه") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        runCatching { repo.createTeamForCurrentUser(newTeamName, managerName) }
                                            .onSuccess { created ->
                                                status = "تیم ساخته شد. کد تیم: ${created.code}"
                                                newTeamName = ""
                                                showCreateTeam = false
                                                refreshTeams()
                                                selectedTeamId = created.teamId
                                                prefs.teamId = created.teamId
                                            }.onFailure { status = it.message ?: "ساخت تیم ناموفق بود." }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("ساخت تیم") }
                        }
                    }
                }
            }

            if (selectedTeam != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftRed)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(selectedTeam.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                            Text("کد تیم: ${selectedTeam.code}", color = FireRed, fontWeight = FontWeight.Bold)
                            Text(if (isAdmin) "دسترسی: مدیر تیم" else "دسترسی: عضو تیم", color = Muted)
                        }
                    }
                }

                if (isAdmin) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftRed)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "درخواست‌های عضویت در انتظار تأیید (${joinRequests.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (joinRequests.isEmpty()) {
                                    Text("درخواستی در انتظار نیست.", color = Muted)
                                } else {
                                    joinRequests.forEach { req ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(7.dp)
                                            ) {
                                                Text(req.displayName, fontWeight = FontWeight.Bold)
                                                Text(req.email, color = Muted, style = MaterialTheme.typography.bodySmall)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            scope.launch {
                                                                runCatching { repo.approveJoinRequest(selectedTeamId, req) }
                                                                    .onSuccess { status = "${req.displayName} عضو تیم شد." }
                                                                    .onFailure { status = it.message ?: "تأیید ناموفق بود" }
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) { Text("تأیید") }
                                                    OutlinedButton(
                                                        onClick = {
                                                            scope.launch {
                                                                runCatching { repo.rejectJoinRequest(selectedTeamId, req) }
                                                                    .onSuccess { status = "درخواست ${req.displayName} رد شد." }
                                                                    .onFailure { status = it.message ?: "رد درخواست ناموفق بود" }
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) { Text("رد") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            unitName, { unitName = it; prefs.unitName = it },
                            label = { Text("نام واحد / خودرو") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            smsNumber, { smsNumber = it; prefs.smsNumber = it },
                            label = { Text("شماره مقصد SMS (اختیاری)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            note, { note = it },
                            label = { Text("توضیح حادثه (اختیاری)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

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
                                        ContextCompat.startForegroundService(
                                            this@MainActivity,
                                            Intent(this@MainActivity, ShiftService::class.java)
                                        )
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
                            enabled = unitName.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    val intent = Intent(this@MainActivity, MapPickerActivity::class.java)
                                    val hasLocation =
                                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (hasLocation) {
                                        runCatching { locationHelper.current() }.getOrNull()?.let { loc ->
                                            intent.putExtra("startLat", loc.latitude)
                                            intent.putExtra("startLon", loc.longitude)
                                        }
                                    }
                                    mapPickerLauncher.launch(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FireRed)
                        ) { Text("انتخاب نقطه روی نقشه و ارسال") }

                        Button(
                            enabled = unitName.isNotBlank(),
                            onClick = {
                                val hasLocation =
                                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (!hasLocation) {
                                    status = "ابتدا مجوز موقعیت را بدهید."
                                } else {
                                    scope.launch {
                                        status = "در حال دریافت GPS..."
                                        runCatching {
                                            val location = locationHelper.current()
                                            repo.sendIncident(
                                                selectedTeamId,
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
                                        }.onFailure { status = it.message ?: "ارسال ناموفق" }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("ارسال موقعیت فعلی") }

                        if (status.isNotBlank()) {
                            Text(
                                status,
                                color = if (
                                    status.contains("خطا") ||
                                    status.contains("ناموفق") ||
                                    status.contains("معتبر نیست")
                                ) FireRed else Success
                            )
                        }

                        HorizontalDivider()
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
                            Text("%.6f, %.6f".format(incident.latitude, incident.longitude))
                            Text("دقت GPS: ${incident.accuracyM.toInt()} متر", color = Muted)
                            if (incident.note.isNotBlank()) Text(incident.note)
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                TextButton(onClick = { ShareUtils.openMap(this@MainActivity, incident) }) {
                                    Text("نقشه", color = FireRed)
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching { repo.acknowledge(selectedTeamId, incident.id, unitName) }
                                            .onSuccess { status = "دریافت تأیید شد." }
                                            .onFailure { status = it.message ?: "خطا" }
                                    }
                                }) { Text("دریافت شد") }
                                TextButton(onClick = { ShareUtils.openSms(this@MainActivity, smsNumber, incident) }) {
                                    Text("SMS", color = FireRed)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = onSignOut) { Text("خروج") }
                    Text(
                        "Designed by  GM",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = Muted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
