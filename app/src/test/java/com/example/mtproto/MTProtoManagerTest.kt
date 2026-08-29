package com.example.mtproto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MTProtoManagerTest {

    private lateinit var client: MTProtoManager
    private lateinit var server: MTProtoManager

    @Before
    fun setup() {
        client = MTProtoManager(isClient = true)
        server = MTProtoManager(isClient = false)

        // Simulate DH exchange
        val clientPublic = client.initializeSession()
        val serverPublic = server.initializeSession()

        client.completeDhExchange(serverPublic)
        server.completeDhExchange(clientPublic)

        // Ensure keys match
        assertArrayEquals(client.authKey, server.authKey)
    }

    @Test
    fun testEncryptionAndDecryptionOutgoing() {
        val plaintext = "Hello, MTProto!".toByteArray(Charsets.UTF_8)
        
        // Client encrypts
        val encrypted = client.encryptMessage(plaintext)
        
        // Server decrypts (client->server, so server uses isOutgoing=false)
        val decryptedWithPadding = server.decryptMessage(encrypted)
        
        // Since we pad, we only check the prefix
        val decrypted = decryptedWithPadding.copyOfRange(0, plaintext.size)
        
        assertArrayEquals(plaintext, decrypted)
    }
}
