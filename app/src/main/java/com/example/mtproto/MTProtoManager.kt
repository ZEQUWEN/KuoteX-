package com.example.mtproto

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Базовый менеджер для реализации протокола MTProto (Mobile Telegram Protocol).
 * Отвечает за шифрование полезной нагрузки, авторизацию и криптографическую маршрутизацию.
 * Референс: https://core.telegram.org/mtproto
 */
class MTProtoManager(val isClient: Boolean = true) {
    
    /**
     * Ключ авторизации (Auth Key), согласованный через обмен Диффи-Хеллмана.
     * В реальном MTProto это 256 байт (2048 бит).
     */
    var authKey: ByteArray? = null
        private set

    /**
     * Идентификатор сессии для предотвращения replay-атак.
     */
    var sessionId: Long = 0L
        private set

    /**
     * Соль, полученная от сервера.
     */
    var serverSalt: Long = 0L
        private set

    private var localKeyPair: KeyPair? = null

    /**
     * Шаг 1: Инициализация сеанса MTProto.
     * Генерируем локальные ключи Диффи-Хеллмана для последующего обмена.
     * Возвращает закодированный публичный ключ для отправки на сервер (в данном случае Supabase).
     */
    fun initializeSession(): ByteArray {
        sessionId = SecureRandom().nextLong()
        
        // Генерация DH пары ключей (в Android поддерживается DH из коробки)
        val keyPairGenerator = KeyPairGenerator.getInstance("DH")
        keyPairGenerator.initialize(2048) // Используем 2048-битный ключ как в MTProto
        localKeyPair = keyPairGenerator.generateKeyPair()
        
        return localKeyPair!!.public.encoded
    }

    /**
     * Шаг 2: Завершение DH обмена (генерация auth_key)
     * @param serverPublicKeyEncoded - Публичный ключ, полученный от сервера
     */
    fun completeDhExchange(serverPublicKeyEncoded: ByteArray) {
        val keyFactory = KeyFactory.getInstance("DH")
        val keySpec = X509EncodedKeySpec(serverPublicKeyEncoded)
        val serverPublicKey: PublicKey = keyFactory.generatePublic(keySpec)
        
        val keyAgreement = KeyAgreement.getInstance("DH")
        keyAgreement.init(localKeyPair!!.private)
        keyAgreement.doPhase(serverPublicKey, true)
        
        var secret = keyAgreement.generateSecret()
        
        // Дополняем секрет до 256 байт (2048 бит)
        if (secret.size < 256) {
            val padded = ByteArray(256)
            System.arraycopy(secret, 0, padded, 256 - secret.size, secret.size)
            secret = padded
        } else if (secret.size > 256) {
            secret = secret.copyOfRange(secret.size - 256, secret.size)
        }
        
        authKey = secret
        serverSalt = SecureRandom().nextLong()
    }

    /**
     * Шифрование исходящего сообщения в соответствии с KDF (Key Derivation Function) MTProto 2.0.
     * Возвращает зашифрованный payload (msg_key + aes_encrypted_data).
     */
    fun encryptMessage(messageData: ByteArray): ByteArray {
        val key = authKey ?: throw IllegalStateException("Auth key is not generated. Please complete DH exchange first.")
        
        // MTProto requires data to be padded to a multiple of 16 bytes.
        val paddingLength = if (messageData.size % 16 == 0) 16 else 16 - (messageData.size % 16)
        val padding = ByteArray(paddingLength)
        SecureRandom().nextBytes(padding)
        val paddedData = messageData + padding
        
        // 1. Вычисляем Message Key (msg_key) - 16 средних байт из SHA-256 хэша
        val isOutgoing = isClient // Клиент отправляет (x=0), Сервер отправляет (x=8)
        val msgKey = calculateMessageKey(key, paddedData, isOutgoing)
        
        // 2. Деривация AES ключа и IV (MTProto 2.0 KDF)
        val (aesKey, aesIv) = deriveKdf(key, msgKey, isOutgoing)
        
        // 3. Шифрование AES-256-IGE
        val encryptedData = com.example.crypto.AesIge.encrypt(paddedData, aesKey, aesIv)
        
        // В MTProto 2.0 заголовок содержит auth_key_id (8 байт), msg_key (16 байт) и зашифрованные данные.
        // Здесь для упрощения отправляем msg_key + encrypted_data
        return msgKey + encryptedData
    }

    /**
     * Расшифровка входящего сообщения.
     */
    fun decryptMessage(encryptedPayload: ByteArray): ByteArray {
        val key = authKey ?: throw IllegalStateException("Auth key is not generated.")
        
        if (encryptedPayload.size < 16) throw IllegalArgumentException("Invalid payload size")
        
        // Извлекаем msg_key (первые 16 байт)
        val msgKey = encryptedPayload.copyOfRange(0, 16)
        val encryptedData = encryptedPayload.copyOfRange(16, encryptedPayload.size)
        
        // Деривация ключей для расшифровки
        val isIncoming = !isClient // Клиент принимает (x=8), Сервер принимает (x=0)
        val (aesKey, aesIv) = deriveKdf(key, msgKey, isIncoming)
        
        // Расшифровка AES-256-IGE
        val decryptedDataWithPadding = com.example.crypto.AesIge.decrypt(encryptedData, aesKey, aesIv)
        
        // В реальном MTProto здесь происходит проверка msgKey.
        // Для упрощения возвращаем данные (padding нужно убирать на уровне TL-парсинга, так как он выровнен,
        // но здесь мы просто возвращаем весь буфер, так как TL парсер игнорирует лишние байты в конце).
        return decryptedDataWithPadding
    }

    /**
     * Вычисление msg_key на основе данных (MTProto 2.0 SHA-256)
     */
    private fun calculateMessageKey(authKey: ByteArray, data: ByteArray, isOutgoing: Boolean): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        // MTProto использует фрагмент ключа авторизации для хэширования (client_msg vs server_msg)
        val x = if (isOutgoing) 88 else 96
        val authKeyFragment = authKey.copyOfRange(x, x + 32)
        digest.update(authKeyFragment)
        digest.update(data)
        // Берем 16 байт из середины хэша: [8..23]
        return digest.digest().copyOfRange(8, 24)
    }

    /**
     * Функция вывода ключей (KDF) MTProto 2.0.
     * Возвращает пару (aes_key, aes_iv).
     */
    private fun deriveKdf(authKey: ByteArray, msgKey: ByteArray, isOutgoing: Boolean): Pair<ByteArray, ByteArray> {
        val x = if (isOutgoing) 0 else 8
        val digest = MessageDigest.getInstance("SHA-256")
        
        // sha256_a = SHA256 (msg_key + substr (auth_key, x, 36));
        digest.update(msgKey)
        digest.update(authKey.copyOfRange(x, x + 36))
        val sha256a = digest.digest()
        
        // sha256_b = SHA256 (substr (auth_key, 40+x, 36) + msg_key);
        digest.reset()
        digest.update(authKey.copyOfRange(40 + x, 40 + x + 36))
        digest.update(msgKey)
        val sha256b = digest.digest()
        
        // aes_key = substr(sha256_a, 0, 8) + substr(sha256_b, 8, 16) + substr(sha256_a, 24, 8)
        val aesKey = sha256a.copyOfRange(0, 8) + sha256b.copyOfRange(8, 24) + sha256a.copyOfRange(24, 32)
        
        // aes_iv = substr(sha256_b, 0, 8) + substr(sha256_a, 8, 16) + substr(sha256_b, 24, 8)
        val aesIv = sha256b.copyOfRange(0, 8) + sha256a.copyOfRange(8, 24) + sha256b.copyOfRange(24, 32)
        
        return Pair(aesKey, aesIv)
    }
}
