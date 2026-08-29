package com.example.data

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.nio.charset.StandardCharsets
import android.util.Base64

object CryptoManager {
    private var aead: Aead? = null
    private const val KEYSET_NAME = "messenger_keyset"
    private const val PREF_FILE_NAME = "messenger_crypto_prefs"
    private const val MASTER_KEY_URI = "android-keystore://messenger_master_key"

    
    private const val PREF_DB_PASSPHRASE = "db_passphrase"

    fun getDatabasePassphrase(context: Context): CharArray {
        val prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        var encryptedPassphrase = prefs.getString(PREF_DB_PASSPHRASE, null)
        if (encryptedPassphrase == null) {
            val randomBytes = ByteArray(32)
            java.security.SecureRandom().nextBytes(randomBytes)
            val newPassphrase = android.util.Base64.encodeToString(randomBytes, android.util.Base64.NO_WRAP)
            encryptedPassphrase = encrypt(newPassphrase)
            prefs.edit().putString(PREF_DB_PASSPHRASE, encryptedPassphrase).apply()
            return newPassphrase.toCharArray()
        }
        return decrypt(encryptedPassphrase).toCharArray()
    }

    fun init(context: Context) {
        if (aead != null) return
        try {
            AeadConfig.register()
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context.applicationContext, KEYSET_NAME, PREF_FILE_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
            
            aead = keysetHandle.getPrimitive(Aead::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return plaintext
        return try {
            val aeadPrimitive = aead ?: return plaintext
            val ciphertext = aeadPrimitive.encrypt(plaintext.toByteArray(StandardCharsets.UTF_8), null)
            Base64.encodeToString(ciphertext, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            plaintext
        }
    }

    fun decrypt(ciphertextBase64: String): String {
        if (ciphertextBase64.isEmpty()) return ciphertextBase64
        return try {
            val aeadPrimitive = aead ?: return ciphertextBase64
            val ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)
            val plaintext = aeadPrimitive.decrypt(ciphertext, null)
            String(plaintext, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ciphertextBase64
        }
    }
}
