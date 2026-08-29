package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.example.analytics.FirebaseAnalyticsHelper

class MyApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:372420700937:android:a1b2c3d4e5f6789012345678")
                    .setApiKey("AIzaSyDummyApiKeyForAnalyticsLogging12345")
                    .setProjectId("ais-dev-lnnfj7dsvqbi5276snipon")
                    .setGcmSenderId("372420700937")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            android.util.Log.w("MyApplication", "FirebaseApp init: ${e.message}")
        }
        try {
            FirebaseAnalyticsHelper.init(this)
        } catch (e: Exception) {
            android.util.Log.w("MyApplication", "FirebaseAnalyticsHelper init: ${e.message}")
        }
        try {
            com.example.data.FirestoreUserRoleManager.init(this)
        } catch (e: Exception) {
            android.util.Log.w("MyApplication", "FirestoreUserRoleManager init: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .allowHardware(false)
            .build()
    }
}
