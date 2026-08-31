package com.example.di

import android.content.Context
import android.content.SharedPreferences
import com.example.crypto.CryptoSession
import com.example.crypto.SignalProtocolManager
import com.example.data.AppDatabase
import com.example.data.MessengerRepository
import com.example.data.NetworkModule
import com.example.data.SecureDatabaseHelper
import com.example.data.UserPreferencesRepository
import com.example.data.WebSocketManager
import com.example.data.dataStore
import com.example.ui.AccountViewModel
import com.example.ui.AppViewModel
import com.example.ui.botapi.BotFatherViewModel
import com.example.webrtc.WebRtcCallSession
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * App-wide long-lived singleton services.
 */
val appServiceModule = module {
    // SharedPreferences & DataStore
    single<SharedPreferences> {
        androidContext().getSharedPreferences("neon_messenger_prefs", Context.MODE_PRIVATE)
    }
    single<UserPreferencesRepository> {
        UserPreferencesRepository(androidContext().dataStore)
    }

    // Network Services
    single<OkHttpClient> {
        NetworkModule.provideOkHttpClient(androidContext()) { _, _, _, _ -> }
    }
    single<WebSocketManager> {
        WebSocketManager(get())
    }

    // Domain & Feature Engines
    single<MessengerSandbox> {
        AdvancedMessengerSandboxImpl(androidContext())
    }
    single<ThemeEngine> {
        DefaultThemeEngine()
    }
}

/**
 * Database & Persistence singletons.
 */
val databaseModule = module {
    single<AppDatabase> {
        SecureDatabaseHelper.getInstance(androidContext()).database
    }

    // DAOs
    single { get<AppDatabase>().botDao() }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().chatDao() }
    single { get<AppDatabase>().messageDao() }
    single { get<AppDatabase>().groupMemberDao() }
    single { get<AppDatabase>().draftDao() }
    single { get<AppDatabase>().contactDao() }
    single { get<AppDatabase>().paymentTransactionDao() }
    single { get<AppDatabase>().queuedMessageDao() }

    // Central Data Repository
    single<MessengerRepository> {
        MessengerRepository(
            botDao = get(),
            userDao = get(),
            chatDao = get(),
            messageDao = get(),
            groupMemberDao = get(),
            draftDao = get(),
            contactDao = get(),
            paymentTransactionDao = get(),
            queuedMessageDao = get(),
            sharedPrefs = get<SharedPreferences>(),
            webSocketManager = get()
        )
    }
}

/**
 * Ephemeral Session Objects (Factory-scoped to prevent memory leaks on resource-constrained devices).
 * Instances are created fresh per request and disposed of immediately upon call/encryption termination.
 */
val sessionModule = module {
    // Ephemeral Signal Protocol & Crypto Sessions
    factory<SignalProtocolManager> {
        SignalProtocolManager()
    }
    factory<CryptoSession> { (sessionId: String) ->
        CryptoSession(sessionId = sessionId)
    }

    // Ephemeral WebRTC Call Session (active only during active call screen)
    factory<WebRtcCallSession> { (chatId: String, isVideo: Boolean) ->
        WebRtcCallSession(chatId = chatId, isVideo = isVideo)
    }
}

/**
 * Jetpack Compose ViewModel bindings.
 */
val viewModelModule = module {
    viewModel {
        AppViewModel(
            repository = get(),
            userPrefs = get(),
            themeEngine = get(),
            sandboxEngine = get()
        )
    }
    viewModel {
        BotFatherViewModel()
    }
    viewModel {
        AccountViewModel()
    }
}

/**
 * Main application Koin module aggregating app services and session factories.
 */
val appModule = module {
    includes(appServiceModule, databaseModule, sessionModule, viewModelModule)
}

val allKoinModules = listOf(appModule)
