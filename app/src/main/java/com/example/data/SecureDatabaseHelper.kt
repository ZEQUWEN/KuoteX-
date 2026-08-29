package com.example.data

import android.content.Context
import androidx.room.Room
import net.sqlcipher.database.SupportFactory

class SecureDatabaseHelper private constructor(context: Context) {

    val database: AppDatabase

    init {
        val dbName = "messenger_database_encrypted"
        val passphrase = CryptoManager.getDatabasePassphrase(context)
        DatabaseDiagnosticUtility.performStartupDiagnostics(context, dbName, passphrase)

        val factory = SupportFactory(String(passphrase).toByteArray())

        database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            dbName
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration()
        .build()
    }

    companion object {
        @Volatile
        private var INSTANCE: SecureDatabaseHelper? = null

        fun getInstance(context: Context): SecureDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureDatabaseHelper(context).also { INSTANCE = it }
            }
        }
    }
}
