package com.firelink.app

import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

class FireRepository {
    private fun teamRoot(teamId: String): DatabaseReference =
        FirebaseProvider.db.reference.child("teams").child(teamId)

    suspend fun signIn(email: String, password: String) {
        FirebaseProvider.auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    fun signOut() = FirebaseProvider.auth.signOut()

    suspend fun sendIncident(teamId: String, incident: Incident): Incident {
        val ref = teamRoot(teamId).child("incidents").push()
        val finalIncident = incident.copy(
            id = ref.key ?: "",
            creatorUid = FirebaseProvider.auth.currentUser?.uid ?: "",
            createdAt = System.currentTimeMillis()
        )
        ref.setValue(finalIncident).await()
        return finalIncident
    }

    suspend fun acknowledge(teamId: String, incidentId: String, unitName: String) {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: error("Not signed in")
        teamRoot(teamId).child("incidents").child(incidentId)
            .child("acks").child(uid)
            .setValue(Ack(unitName = unitName, at = System.currentTimeMillis()))
            .await()
    }

    fun observeIncidents(
        teamId: String,
        onChange: (List<Incident>) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener {
        val ref = teamRoot(teamId).child("incidents")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Incident::class.java) }
                    .sortedByDescending { it.createdAt }
                onChange(list)
            }
            override fun onCancelled(error: DatabaseError) = onError(error.message)
        }
        ref.limitToLast(50).addValueEventListener(listener)
        return listener
    }
}
