package com.firelink.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

class FireLinkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.ctx = applicationContext
        if (BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_DATABASE_URL.isNotBlank() &&
            FirebaseApp.getApps(this).isEmpty()
        ) {
            val options = FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setDatabaseUrl(BuildConfig.FIREBASE_DATABASE_URL)
                .build()
            FirebaseApp.initializeApp(this, options)
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        }
    }
}
