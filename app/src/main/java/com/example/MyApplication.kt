package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.initialize
import com.example.analytics.FirebaseAnalyticsHelper
import com.example.di.allKoinModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.json.JSONObject
import java.io.InputStream

class MyApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        initFirebase()
        try {
            startKoin {
                androidLogger(Level.ERROR)
                androidContext(this@MyApplication)
                modules(allKoinModules)
            }
        } catch (e: Exception) {
            Log.w("MyApplication", "Koin start: ${e.message}")
        }
        try {
            com.example.di.Injector.init(com.example.di.DefaultAppContainer(this))
        } catch (e: Exception) {
            Log.w("MyApplication", "Injector init: ${e.message}")
        }
        try {
            com.example.config.FirebaseRemoteConfigManager.init(this)
        } catch (e: Exception) {
            Log.w("MyApplication", "FirebaseRemoteConfigManager init: ${e.message}")
        }
        try {
            FirebaseAnalyticsHelper.init(this)
        } catch (e: Exception) {
            Log.w("MyApplication", "FirebaseAnalyticsHelper init: ${e.message}")
        }
        try {
            com.example.data.FirestoreUserRoleManager.init(this)
        } catch (e: Exception) {
            Log.w("MyApplication", "FirestoreUserRoleManager init: ${e.message}")
        }
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                var options: FirebaseOptions? = null

                try {
                    val inputStream: InputStream = assets.open("google-services.json")
                    val jsonStr = inputStream.bufferedReader().use { it.readText() }
                    val rootJson = JSONObject(jsonStr)
                    val projectInfo = rootJson.getJSONObject("project_info")
                    val projectId = projectInfo.getString("project_id")
                    val projectNumber = projectInfo.optString("project_number", "")
                    val storageBucket = projectInfo.optString("storage_bucket", "$projectId.firebasestorage.app")

                    val clients = rootJson.getJSONArray("client")
                    if (clients.length() > 0) {
                        val client0 = clients.getJSONObject(0)
                        val clientInfo = client0.getJSONObject("client_info")
                        val appId = clientInfo.getString("mobilesdk_app_id")
                        val apiKeys = client0.getJSONArray("api_key")
                        val apiKey = if (apiKeys.length() > 0) apiKeys.getJSONObject(0).getString("current_key") else ""

                        options = FirebaseOptions.Builder()
                            .setApplicationId(appId)
                            .setApiKey(apiKey)
                            .setProjectId(projectId)
                            .setGcmSenderId(projectNumber)
                            .setStorageBucket(storageBucket)
                            .build()
                        Log.i("MyApplication", "FirebaseOptions loaded from google-services.json (project=$projectId)")
                    }
                } catch (e: Exception) {
                    Log.d("MyApplication", "Direct asset load exception: ${e.message}")
                }

                if (options == null) {
                    options = FirebaseOptions.Builder()
                        .setApplicationId("1:372420700937:android:a1b2c3d4e5f6789012345678")
                        .setApiKey("AIzaSyDummyApiKeyForAnalyticsLogging12345")
                        .setProjectId("ais-dev-lnnfj7dsvqbi5276snipon")
                        .setGcmSenderId("372420700937")
                        .setStorageBucket("ais-dev-lnnfj7dsvqbi5276snipon.firebasestorage.app")
                        .build()
                }

                if (options != null) {
                    Firebase.initialize(context = this, options = options)
                } else {
                    Firebase.initialize(context = this)
                }
                Log.i("MyApplication", "Firebase initialized successfully")
                
                Firebase.appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.i("MyApplication", "Firebase App Check initialized with Play Integrity")
            }
        } catch (e: Exception) {
            Log.w("MyApplication", "FirebaseApp init error: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .allowHardware(false)
            .build()
    }
}
