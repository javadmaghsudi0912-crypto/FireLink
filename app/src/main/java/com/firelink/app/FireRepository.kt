package com.firelink.app

import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FireRepository {
    private fun normalizeTeamId(value: String): String {
        val v = value.trim()
        return if (v.startsWith("FL-", ignoreCase = true)) v.uppercase() else v
    }

    private fun teamRoot(teamId: String) =
        FirebaseProvider.db.reference.child("teams").child(normalizeTeamId(teamId))

    suspend fun signIn(email: String, password: String) {
        FirebaseProvider.auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun registerUser(email: String, password: String, displayName: String, teamId: String): String {
        require(password.length >= 8) { "رمز عبور باید حداقل ۸ کاراکتر باشد." }
        val result = FirebaseProvider.auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = result.user?.uid ?: error("ساخت حساب ناموفق بود")
        try {
            submitJoinRequest(teamId, displayName, email)
        } finally {
            FirebaseProvider.auth.signOut()
        }
        return uid
    }

    suspend fun requestJoinExisting(email: String, password: String, displayName: String, teamId: String) {
        FirebaseProvider.auth.signInWithEmailAndPassword(email.trim(), password).await()
        try {
            submitJoinRequest(teamId, displayName, email)
        } finally {
            FirebaseProvider.auth.signOut()
        }
    }

    suspend fun registerAdminAndCreateTeam(
        email: String,
        password: String,
        displayName: String,
        teamName: String
    ): TeamSummary {
        require(password.length >= 8) { "رمز عبور باید حداقل ۸ کاراکتر باشد." }
        FirebaseProvider.auth.createUserWithEmailAndPassword(email.trim(), password).await()
        return try {
            createTeamForCurrentUser(teamName, displayName)
        } catch (t: Throwable) {
            FirebaseProvider.auth.signOut()
            throw t
        }
    }

    suspend fun createTeamForCurrentUser(teamName: String, displayName: String): TeamSummary {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: error("کاربر وارد نشده است")
        require(teamName.trim().length >= 2) { "نام تیم را کامل وارد کنید." }
        require(displayName.trim().length >= 2) { "نام مدیر را کامل وارد کنید." }

        val teamId = "FL-" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        val now = System.currentTimeMillis()
            val team = mapOf(
                "info" to mapOf(
                    "name" to teamName.trim(),
                    "code" to teamId,
                    "ownerUid" to uid,
                    "createdAt" to now,
                    "active" to true
                ),
                "members" to mapOf(
                    uid to mapOf(
                        "displayName" to displayName.trim(),
                        "role" to "admin",
                        "joinedAt" to now
                    )
                )
            )
        teamRoot(teamId).setValue(team).await()
        FirebaseProvider.db.reference.child("userTeams").child(uid).child(teamId).setValue(true).await()
        return TeamSummary(teamId, teamName.trim(), teamId, "admin")
    }

    private suspend fun submitJoinRequest(teamId: String, displayName: String, email: String) {
        val normalized = normalizeTeamId(teamId)
        require(normalized.isNotBlank()) { "کد تیم را وارد کنید." }
        val uid = FirebaseProvider.auth.currentUser?.uid ?: error("ابتدا وارد حساب شوید")
        val values = mapOf(
            "uid" to uid,
            "displayName" to displayName.trim(),
            "email" to email.trim(),
            "teamId" to normalized,
            "createdAt" to System.currentTimeMillis()
        )
        try {
            FirebaseProvider.db.reference.child("joinRequests").child(normalized).child(uid)
                .setValue(values).await()
        } catch (t: Throwable) {
            if (t.message?.contains("permission", ignoreCase = true) == true) {
                throw IllegalStateException("کد تیم معتبر نیست یا تیم غیرفعال است.")
            }
            throw t
        }
    }

    suspend fun ensureLegacyMembershipIndex(candidates: List<String>) {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: return
        val currentIndex = FirebaseProvider.db.reference.child("userTeams").child(uid).get().await()
        if (currentIndex.exists()) return

        for (candidate in candidates.map(::normalizeTeamId).filter { it.isNotBlank() }.distinct()) {
            try {
                val member = teamRoot(candidate).child("members").child(uid).get().await()
                if (!member.exists()) continue
                FirebaseProvider.db.reference.child("userTeams").child(uid).child(candidate).setValue(true).await()

                val role = member.child("role").getValue(String::class.java) ?: "member"
                if (role == "admin") {
                    val infoRef = teamRoot(candidate).child("info")
                    if (!infoRef.get().await().exists()) {
                        infoRef.setValue(
                            mapOf(
                                "name" to "تیم عملیاتی",
                                "code" to candidate,
                                "ownerUid" to uid,
                                "createdAt" to System.currentTimeMillis(),
                                "active" to true
                            )
                        ).await()
                    }
                }
                return
            } catch (_: Throwable) {
            }
        }
    }

    suspend fun loadMyTeams(): List<TeamSummary> {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: error("کاربر وارد نشده است")
        val index = FirebaseProvider.db.reference.child("userTeams").child(uid).get().await()
        val ids = index.children.mapNotNull { it.key }.distinct()
        return ids.mapNotNull { teamId ->
            runCatching {
                val snap = teamRoot(teamId).get().await()
                if (!snap.exists()) return@runCatching null
                val info = snap.child("info").getValue(TeamInfo::class.java)
                val role = snap.child("members").child(uid).child("role").getValue(String::class.java) ?: "member"
                TeamSummary(
                    teamId = teamId,
                    name = info?.name?.takeIf { it.isNotBlank() } ?: teamId,
                    code = info?.code?.takeIf { it.isNotBlank() } ?: teamId,
                    role = role
                )
            }.getOrNull()
        }.filterNotNull().sortedBy { it.name }
    }

    fun observeJoinRequests(teamId: String, onChange: (List<JoinRequest>) -> Unit, onError: (String) -> Unit): ValueEventListener {
        val ref = FirebaseProvider.db.reference.child("joinRequests").child(normalizeTeamId(teamId))
        val listener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                onChange(
                    s.children.mapNotNull { snap ->
                        snap.getValue(JoinRequest::class.java)?.copy(uid = snap.key ?: "")
                    }.sortedBy { it.createdAt }
                )
            }
            override fun onCancelled(e: DatabaseError) = onError(e.message)
        }
        ref.addValueEventListener(listener)
        return listener
    }

    fun removeJoinRequestsListener(teamId: String, listener: ValueEventListener) {
        FirebaseProvider.db.reference.child("joinRequests").child(normalizeTeamId(teamId))
            .removeEventListener(listener)
    }

    suspend fun approveJoinRequest(teamId: String, request: JoinRequest) {
        val normalized = normalizeTeamId(teamId)
        val member = mapOf(
            "displayName" to request.displayName,
            "role" to "member",
            "joinedAt" to System.currentTimeMillis()
        )
        val root = FirebaseProvider.db.reference
        root.child("teams").child(normalized).child("members").child(request.uid).setValue(member).await()
        root.child("userTeams").child(request.uid).child(normalized).setValue(true).await()
        root.child("joinRequests").child(normalized).child(request.uid).removeValue().await()
    }

    suspend fun rejectJoinRequest(teamId: String, request: JoinRequest) {
        FirebaseProvider.db.reference.child("joinRequests").child(normalizeTeamId(teamId))
            .child(request.uid).removeValue().await()
    }

    suspend fun sendIncident(teamId: String, incident: Incident): Incident {
        val ref = teamRoot(teamId).child("incidents").push()
        val uid = FirebaseProvider.auth.currentUser?.uid ?: error("کاربر وارد نشده است")
        val now = System.currentTimeMillis()
        val values = mapOf(
            "latitude" to incident.latitude,
            "longitude" to incident.longitude,
            "createdAt" to now,
            "createdBy" to incident.unitName,
            "creatorUid" to uid,
            "unitName" to incident.unitName,
            "status" to incident.status
        )
        ref.setValue(values).await()
        return incident.copy(id = ref.key ?: "", creatorUid = uid, createdAt = now)
    }

    suspend fun acknowledge(teamId: String, incidentId: String, unitName: String) {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: error("Not signed in")
        teamRoot(teamId).child("incidents").child(incidentId).child("acks").child(uid)
            .setValue(Ack(unitName, System.currentTimeMillis())).await()
    }

    fun observeIncidents(teamId: String, onChange: (List<Incident>) -> Unit, onError: (String) -> Unit): ValueEventListener {
        val ref = teamRoot(teamId).child("incidents")
        val listener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                onChange(
                    s.children.mapNotNull { snap ->
                        snap.getValue(Incident::class.java)?.copy(id = snap.key ?: "")
                    }.sortedByDescending { it.createdAt }
                )
            }
            override fun onCancelled(e: DatabaseError) = onError(e.message)
        }
        ref.limitToLast(50).addValueEventListener(listener)
        return listener
    }

    fun removeIncidentsListener(teamId: String, listener: ValueEventListener) {
        teamRoot(teamId).child("incidents").removeEventListener(listener)
    }
}
