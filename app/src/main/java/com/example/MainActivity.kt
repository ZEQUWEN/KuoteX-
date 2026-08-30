package com.example

import android.os.Bundle
import com.example.data.dataStore
import com.example.data.UserPreferencesRepository
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.MessengerRepository
import com.example.ui.AppViewModel
import com.example.ui.MainAppNavigation
import com.example.ui.theme.NeonMessengerTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import android.util.Log
import android.widget.Toast
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.ui.ConnectionStatus



class MainActivity : ComponentActivity() {
    // Ask for POST_NOTIFICATIONS permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM", "Notification permission granted")
            fetchFCMToken()
        } else {
            Log.d("FCM", "Notification permission denied")
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchFCMToken()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            fetchFCMToken()
        }
    }

    private fun fetchFCMToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d("FCM", "FCM Token: $token")
            }
        } catch(e: Exception) {
            Log.e("FCM", "Error fetching FCM token: ${e.message}")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("mock-project")
                    .setApplicationId("1:1234567890:android:abcdef123456")
                    .setApiKey("mock-api-key")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
            com.example.analytics.AnalyticsTracker.init(this)
        } catch (e: Exception) {
            Log.e("Firebase", "Firebase initialization failed: ${e.message}")
        }

        com.example.utils.CrashReporter.init(this)
        com.example.utils.CrashReporter.logLifecycleEvent("onCreate")

        val lastCrash = com.example.utils.CrashReporter.getLastCrash(this)
        if (lastCrash != null) {
            setContent {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(text = "PREVIOUS CRASH:\n$lastCrash", modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()))
                }
            }
            return
        }

        try {
            com.example.data.CryptoManager.init(applicationContext)
            net.sqlcipher.database.SQLiteDatabase.loadLibs(applicationContext)
            val db = com.example.data.SecureDatabaseHelper.getInstance(applicationContext).database

            val sharedPrefs = getSharedPreferences("neon_messenger_prefs", android.content.Context.MODE_PRIVATE)
            com.example.ui.botapi.BotRegistry.init(db.botDao())
            val okHttpClient = com.example.data.NetworkModule.provideOkHttpClient(applicationContext) { type, bytesReceived, bytesSent, path ->
                // Do nothing for now
            }

            val webSocketManager = com.example.data.WebSocketManager(okHttpClient)
            
            // Setup WorkManager for edge cases (background sync)
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.data.MessageSyncWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            ).build()
            androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "MessageSync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            val cacheCleanupRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.data.CacheCleanupWorker>(
                7, java.util.concurrent.TimeUnit.DAYS
            ).build()
            androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "CacheCleanup",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                cacheCleanupRequest
            )
            
            val repository = MessengerRepository(
                db.botDao(), 
                db.userDao(), 
                db.chatDao(), 
                db.messageDao(), 
                db.groupMemberDao(), 
                db.draftDao(), 
                db.contactDao(), 
                db.paymentTransactionDao(),
                db.queuedMessageDao(),
                sharedPrefs, 
                webSocketManager
            )



            val userPrefs = com.example.data.UserPreferencesRepository(applicationContext.dataStore)
            
            // Start presence background worker
            try {
                com.example.ui.PresenceManager.updatePresence(applicationContext, "current_user_id", true)
            } catch(e: Exception) { e.printStackTrace() }
            
            val factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return AppViewModel(repository, userPrefs) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }

            
            val viewModel: AppViewModel by viewModels { factory }
            viewModel.checkAutoTheme()

            // Initialize Notification Channels
            com.example.notifications.NotificationHelper.initNotificationChannels(applicationContext)
            askNotificationPermission()

            intent?.getStringExtra("OPEN_CHAT_ID")?.let { chatId ->
                viewModel.setPendingOpenChatId(chatId)
            }

            intent?.getStringExtra("OPEN_STREAM_HOST_ID")?.let { hostId ->
                viewModel.setPendingOpenStreamHostId(hostId)
            }

            val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Check initial network state
            val activeNet = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNet)
            val isInitiallyOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            viewModel.setConnectionStatus(if (isInitiallyOnline) ConnectionStatus.ONLINE else ConnectionStatus.OFFLINE)

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    viewModel.setConnectionStatus(ConnectionStatus.CONNECTING)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        viewModel.setConnectionStatus(ConnectionStatus.ONLINE)
                    }, 600)
                }
                override fun onLost(network: Network) {
                    viewModel.setConnectionStatus(ConnectionStatus.OFFLINE)
                }
                override fun onUnavailable() {
                    viewModel.setConnectionStatus(ConnectionStatus.OFFLINE)
                }
            }

            try {
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            enableEdgeToEdge()
            setContent {
                val primaryColorLong by viewModel.customPrimaryColor.collectAsState()
                val secondaryColorLong by viewModel.customSecondaryColor.collectAsState()
                
                val themeOpacity by viewModel.themeOpacity.collectAsState()
                val primary = if (primaryColorLong != null && primaryColorLong != 0L) Color(primaryColorLong!!.toULong()) else null
                val secondary = if (secondaryColorLong != null && secondaryColorLong != 0L) Color(secondaryColorLong!!.toULong()) else null
                
val isDarkThemeEnabled by viewModel.isDarkThemeEnabled.collectAsState()
                val isAutoThemeEnabled by viewModel.isAutoThemeEnabled.collectAsState()
                val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                val batterySaverEnabled by viewModel.batterySaverEnabled.collectAsState()
                
                var batteryLevel by androidx.compose.runtime.remember { mutableStateOf(100f) }
                LaunchedEffect(Unit) {
                    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                        applicationContext.registerReceiver(null, ifilter)
                    }
                    val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    if (level != -1 && scale != -1) {
                        batteryLevel = level * 100f / scale.toFloat()
                    }
                }
                
                val disableNeon = batterySaverEnabled && batteryLevel < 20f
                val finalCustomPrimary = if (disableNeon) null else primary
                val finalCustomSecondary = if (disableNeon) null else secondary

                NeonMessengerTheme(darkTheme = if (isAutoThemeEnabled) systemDarkTheme else isDarkThemeEnabled, customPrimary = finalCustomPrimary, customSecondary = finalCustomSecondary, themeOpacity = themeOpacity) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        MainAppNavigation(viewModel)
                    }
                }
            }
        } catch (e: Exception) {
            setContent {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(text = "CRASH: \${e.stackTraceToString()}", modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        com.example.utils.CrashReporter.logLifecycleEvent("onStart")
    }

    override fun onResume() {
        super.onResume()
        com.example.utils.CrashReporter.logLifecycleEvent("onResume")
    }

    override fun onPause() {
        super.onPause()
        com.example.utils.CrashReporter.logLifecycleEvent("onPause")
    }

    override fun onStop() {
        super.onStop()
        com.example.utils.CrashReporter.logLifecycleEvent("onStop")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("OPEN_CHAT_ID")?.let { chatId ->
            try {
                val vm: AppViewModel by viewModels()
                vm.setPendingOpenChatId(chatId)
            } catch (e: Exception) {
                // Handled safely
            }
        }
        intent.getStringExtra("OPEN_STREAM_HOST_ID")?.let { hostId ->
            try {
                val vm: AppViewModel by viewModels()
                vm.setPendingOpenStreamHostId(hostId)
            } catch (e: Exception) {
                // Handled safely
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.utils.CrashReporter.logLifecycleEvent("onDestroy")
    }
}
