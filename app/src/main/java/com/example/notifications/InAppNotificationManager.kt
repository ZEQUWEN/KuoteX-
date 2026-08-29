package com.example.notifications

import android.content.Context
import com.example.data.MessengerRepository
import com.example.data.SecureDatabaseHelper
import com.example.ui.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

data class TelegramBubbleNotification(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val chatTitle: String? = null,
    val text: String,
    val isMention: Boolean = false,
    val isGroup: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object InAppNotificationManager {
    private val _currentBubble = MutableStateFlow<TelegramBubbleNotification?>(null)
    val currentBubble: StateFlow<TelegramBubbleNotification?> = _currentBubble.asStateFlow()

    fun postNotification(notification: TelegramBubbleNotification, currentActiveChatId: String? = null) {
        // If user is currently in this exact chat, we do not need to pop up an intrusive banner
        if (currentActiveChatId != null && currentActiveChatId == notification.chatId) {
            return
        }
        _currentBubble.value = notification
    }

    fun dismissBubble() {
        _currentBubble.value = null
    }

    fun dismissIfChatId(chatId: String) {
        if (_currentBubble.value?.chatId == chatId) {
            _currentBubble.value = null
        }
    }

    fun markAsRead(chatId: String, context: Context, onCompleted: () -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SecureDatabaseHelper.getInstance(context).database
                val activeAccount = db.userDao().getActiveAccount().firstOrNull()
                val currentUserId = activeAccount?.id ?: "123456789"
                db.messageDao().markAsRead(chatId, currentUserId)
                val chat = db.chatDao().getChatById(chatId)
                if (chat != null) {
                    db.chatDao().insertChat(chat.copy(unreadCount = 0))
                }
                NotificationHelper.cancelChatNotifications(context, chatId)
                dismissIfChatId(chatId)
            } catch (e: Exception) {
                android.util.Log.e("InAppNotification", "Error marking as read: ${e.message}")
            } finally {
                CoroutineScope(Dispatchers.Main).launch {
                    onCompleted()
                }
            }
        }
    }

    fun sendReply(
        chatId: String,
        replyText: String,
        context: Context,
        onCompleted: () -> Unit = {}
    ) {
        if (replyText.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SecureDatabaseHelper.getInstance(context).database
                val activeAccount = db.userDao().getActiveAccount().firstOrNull()
                val currentUserId = activeAccount?.id ?: "123456789"
                val sanitizedReply = com.example.utils.MessageSanitizer.sanitize(replyText)
                val encryptedText = com.example.data.CryptoManager.encrypt(sanitizedReply)
                val messageId = UUID.randomUUID().toString()

                val replyMsg = Message(
                    id = messageId,
                    chatId = chatId,
                    senderId = currentUserId,
                    text = encryptedText,
                    timestamp = System.currentTimeMillis(),
                    isDelivered = true
                )
                db.messageDao().insertMessage(replyMsg)

                val chat = db.chatDao().getChatById(chatId)
                if (chat != null) {
                    db.chatDao().insertChat(
                        chat.copy(
                            lastMessage = sanitizedReply,
                            lastMessageTimestamp = System.currentTimeMillis(),
                            lastMessageSenderName = "You",
                            unreadCount = 0
                        )
                    )
                }
                db.messageDao().markAsRead(chatId, currentUserId)
                NotificationHelper.cancelChatNotifications(context, chatId)
                dismissIfChatId(chatId)

                com.example.analytics.FirebaseAnalyticsHelper.logMessageSendSuccess(
                    messageId = messageId,
                    chatId = chatId,
                    durationMs = 25L,
                    transportType = "bubble_quick_reply",
                    retryCount = 0,
                    wasCachedOffline = false
                )
            } catch (e: Exception) {
                android.util.Log.e("InAppNotification", "Error sending reply: ${e.message}")
            } finally {
                CoroutineScope(Dispatchers.Main).launch {
                    onCompleted()
                }
            }
        }
    }
}
