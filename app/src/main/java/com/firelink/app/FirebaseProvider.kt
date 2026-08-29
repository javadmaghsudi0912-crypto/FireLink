package com.firelink.app

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object FirebaseProvider {
    fun configured(): Boolean = FirebaseApp.getApps(AppContext.ctx).isNotEmpty()
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val db: FirebaseDatabase get() = FirebaseDatabase.getInstance()
}
