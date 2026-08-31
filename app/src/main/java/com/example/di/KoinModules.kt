package com.example.di

import android.content.Context
import android.content.SharedPreferences
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
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // SharedPreferences & DataStore
    single<SharedPreferences> {
        androidContext().getSharedPreferences("neon_messenger_prefs", Context.MODE_PRIVATE)
    }
    single<UserPreferencesRepository> {
        UserPreferencesRepository(androidContext().dataStore)
    }

    // Network & Cryptography
    single<OkHttpClient> {
        NetworkModule.provideOkHttpClient(androidContext()) { _, _, _, _ -> }
    }
    single<WebSocketManager> {
        WebSocketManager(get())
    }
    single<SignalProtocolManager> {
        SignalProtocolManager()
    }

    // Domain & Feature Engines (Pluggable DI Interfaces)
    single<MessengerSandbox> {
        AdvancedMessengerSandboxImpl(androidContext())
    }
    single<ThemeEngine> {
        DefaultThemeEngine()
    }
}

val databaseModule = module {
    // Room & SQLCipher Database
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

    // Repositories
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

val allKoinModules = listOf(appModule, databaseModule, viewModelModule)
