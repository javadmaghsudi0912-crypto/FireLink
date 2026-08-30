package com.firelink.app
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
class FireRepository {
    private fun teamRoot(teamId:String)=FirebaseProvider.db.reference.child("teams").child(teamId)
    suspend fun signIn(email:String,password:String){FirebaseProvider.auth.signInWithEmailAndPassword(email.trim(),password).await()}
    suspend fun registerUser(email:String, password:String, displayName:String, teamId:String):String {
        require(password.length >= 8) { "رمز عبور باید حداقل ۸ کاراکتر باشد." }
        val result = FirebaseProvider.auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = result.user?.uid ?: error("ساخت حساب ناموفق بود")
        submitJoinRequest(teamId, displayName, email)
        FirebaseProvider.auth.signOut()
        return uid
    }

    suspend fun requestJoinExisting(email:String, password:String, displayName:String, teamId:String) {
        FirebaseProvider.auth.signInWithEmailAndPassword(email.trim(), password).await()
        submitJoinRequest(teamId, displayName, email)
        FirebaseProvider.auth.signOut()
    }

    private suspend fun submitJoinRequest(teamId:String, displayName:String, email:String) {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: error("ابتدا وارد حساب شوید")
        val values = mapOf(
            "uid" to uid,
            "displayName" to displayName.trim(),
            "email" to email.trim(),
            "teamId" to teamId.trim(),
            "createdAt" to System.currentTimeMillis()
        )
        FirebaseProvider.db.reference.child("joinRequests").child(teamId.trim()).child(uid).setValue(values).await()
    }

    fun observeJoinRequests(teamId:String,onChange:(List<JoinRequest>)->Unit,onError:(String)->Unit):ValueEventListener {
        val ref = FirebaseProvider.db.reference.child("joinRequests").child(teamId)
        val listener = object:ValueEventListener {
            override fun onDataChange(s:DataSnapshot) {
                onChange(s.children.mapNotNull { snap -> snap.getValue(JoinRequest::class.java)?.copy(uid = snap.key ?: "") }.sortedBy { it.createdAt })
            }
            override fun onCancelled(e:DatabaseError) { onError(e.message) }
        }
        ref.addValueEventListener(listener)
        return listener
    }

    suspend fun approveJoinRequest(teamId:String, request:JoinRequest) {
        val member = mapOf("displayName" to request.displayName, "role" to "member")
        val root = FirebaseProvider.db.reference
        val updates = mapOf<String,Any?>(
            "/teams/$teamId/members/${request.uid}" to member,
            "/joinRequests/$teamId/${request.uid}" to null
        )
        root.updateChildren(updates).await()
    }
    suspend fun sendIncident(teamId:String, incident:Incident):Incident {
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
    suspend fun acknowledge(teamId:String,incidentId:String,unitName:String){
        val uid=FirebaseProvider.auth.currentUser?.uid?:error("Not signed in")
        teamRoot(teamId).child("incidents").child(incidentId).child("acks").child(uid).setValue(Ack(unitName,System.currentTimeMillis())).await()
    }
    fun observeIncidents(teamId:String,onChange:(List<Incident>)->Unit,onError:(String)->Unit):ValueEventListener {
        val ref=teamRoot(teamId).child("incidents")
        val l=object:ValueEventListener{
            override fun onDataChange(s:DataSnapshot){onChange(s.children.mapNotNull{ snap -> snap.getValue(Incident::class.java)?.copy(id = snap.key ?: "") }.sortedByDescending{it.createdAt})}
            override fun onCancelled(e:DatabaseError){onError(e.message)}
        }
        ref.limitToLast(50).addValueEventListener(l); return l
    }
}
