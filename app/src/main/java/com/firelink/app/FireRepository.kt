package com.firelink.app
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
class FireRepository {
    private fun teamRoot(teamId:String)=FirebaseProvider.db.reference.child("teams").child(teamId)
    suspend fun signIn(email:String,password:String){FirebaseProvider.auth.signInWithEmailAndPassword(email.trim(),password).await()}
    suspend fun sendIncident(teamId:String, incident:Incident):Incident {
        val ref=teamRoot(teamId).child("incidents").push()
        val out=incident.copy(id=ref.key?:"",creatorUid=FirebaseProvider.auth.currentUser?.uid?:"",createdAt=System.currentTimeMillis())
        ref.setValue(out).await(); return out
    }
    suspend fun acknowledge(teamId:String,incidentId:String,unitName:String){
        val uid=FirebaseProvider.auth.currentUser?.uid?:error("Not signed in")
        teamRoot(teamId).child("incidents").child(incidentId).child("acks").child(uid).setValue(Ack(unitName,System.currentTimeMillis())).await()
    }
    fun observeIncidents(teamId:String,onChange:(List<Incident>)->Unit,onError:(String)->Unit):ValueEventListener {
        val ref=teamRoot(teamId).child("incidents")
        val l=object:ValueEventListener{
            override fun onDataChange(s:DataSnapshot){onChange(s.children.mapNotNull{it.getValue(Incident::class.java)}.sortedByDescending{it.createdAt})}
            override fun onCancelled(e:DatabaseError){onError(e.message)}
        }
        ref.limitToLast(50).addValueEventListener(l); return l
    }
}
