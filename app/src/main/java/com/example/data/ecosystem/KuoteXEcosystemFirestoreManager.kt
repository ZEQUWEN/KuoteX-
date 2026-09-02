package com.example.data.ecosystem

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Custom Exceptions for Ecosystem Operations.
 */
class InsufficientBalanceException(message: String) : Exception(message)
class GiftSoldOutException(message: String) : Exception(message)
class DuplicateTransactionException(message: String) : Exception(message)
class UnauthorizedBoostException(message: String) : Exception(message)
class InsufficientBoostVotesException(message: String) : Exception(message)

/**
 * KuoteX Ecosystem Firestore Manager
 * Handles ACID-compliant atomic transactions, balance management, VIP subscriptions,
 * channel boosting with privilege validation, and animated profile gifts.
 */
object KuoteXEcosystemFirestoreManager {

    private const val TAG = "KuoteXEcosystem"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_CHANNELS = "channels"
    private const val COLLECTION_GIFTS_CATALOG = "gifts_catalog"
    private const val COLLECTION_USER_GIFTS = "user_gifts"
    private const val COLLECTION_LEDGER_TX = "ledger_transactions"
    private const val COLLECTION_IDEMPOTENCY = "idempotency_keys"
    private const val COLLECTION_POLL_VOTES = "poll_votes"

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Local in-memory caching fallback for offline & fast UI rendering
    private val _currentUserState = MutableStateFlow<KuoteXUserDoc?>(null)
    val currentUserState: StateFlow<KuoteXUserDoc?> = _currentUserState.asStateFlow()

    private val _catalogGifts = MutableStateFlow<List<KuoteXCatalogGiftDoc>>(emptyList())
    val catalogGifts: StateFlow<List<KuoteXCatalogGiftDoc>> = _catalogGifts.asStateFlow()

    private val _pinnedGiftsMap = MutableStateFlow<Map<String, List<KuoteXUserGiftDoc>>>(emptyMap())
    val pinnedGiftsMap: StateFlow<Map<String, List<KuoteXUserGiftDoc>>> = _pinnedGiftsMap.asStateFlow()

    init {
        initDefaultCatalog()
    }

    /**
     * Pre-populates the default gifts catalog if not present.
     */
    private fun initDefaultCatalog() {
        val defaultCatalog = listOf(
            KuoteXCatalogGiftDoc(
                catalogGiftId = "gift_cyber_dragon_001",
                title = "Cyber Dragon 2026",
                price = 250L,
                totalSupply = 1000L,
                availableSupply = 782L,
                isExclusive = true,
                lottieAssetUrl = "cyber_dragon.json",
                backdropColorHex = "#1E1B4B",
                emojiIcon = "🐉",
                maxUpgradeLevel = 5
            ),
            KuoteXCatalogGiftDoc(
                catalogGiftId = "gift_golden_crown_002",
                title = "Золотая Корона VIP",
                price = 150L,
                totalSupply = 5000L,
                availableSupply = 4120L,
                isExclusive = true,
                lottieAssetUrl = "golden_crown.json",
                backdropColorHex = "#281904",
                emojiIcon = "👑",
                maxUpgradeLevel = 5
            ),
            KuoteXCatalogGiftDoc(
                catalogGiftId = "gift_neon_diamond_003",
                title = "Неоновый Алмаз",
                price = 100L,
                totalSupply = -1L,
                availableSupply = -1L,
                isExclusive = false,
                lottieAssetUrl = "neon_diamond.json",
                backdropColorHex = "#062826",
                emojiIcon = "💎",
                maxUpgradeLevel = 3
            ),
            KuoteXCatalogGiftDoc(
                catalogGiftId = "gift_cosmic_rocket_004",
                title = "Космическая Ракета",
                price = 75L,
                totalSupply = -1L,
                availableSupply = -1L,
                isExclusive = false,
                lottieAssetUrl = "cosmic_rocket.json",
                backdropColorHex = "#1E1035",
                emojiIcon = "🚀",
                maxUpgradeLevel = 3
            ),
            KuoteXCatalogGiftDoc(
                catalogGiftId = "gift_magic_crystal_005",
                title = "Магический Кристалл",
                price = 50L,
                totalSupply = -1L,
                availableSupply = -1L,
                isExclusive = false,
                lottieAssetUrl = "magic_crystal.json",
                backdropColorHex = "#280B1E",
                emojiIcon = "🔮",
                maxUpgradeLevel = 3
            ),
            KuoteXCatalogGiftDoc(
                catalogGiftId = "gift_sakura_flower_006",
                title = "Цветущая Сакура",
                price = 30L,
                totalSupply = -1L,
                availableSupply = -1L,
                isExclusive = false,
                lottieAssetUrl = "sakura.json",
                backdropColorHex = "#1C0D17",
                emojiIcon = "🌸",
                maxUpgradeLevel = 3
            )
        )
        _catalogGifts.value = defaultCatalog

        // Sync with Firestore asynchronously
        managerScope.launch {
            try {
                for (gift in defaultCatalog) {
                    val docRef = firestore.collection(COLLECTION_GIFTS_CATALOG).document(gift.catalogGiftId)
                    val snap = docRef.get().await()
                    if (!snap.exists()) {
                        docRef.set(gift).await()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing default catalog to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Synchronizes a user account with Firestore and observes changes.
     */
    suspend fun syncAndObserveUser(userId: String, username: String, displayName: String, role: String = "user") {
        withContext(Dispatchers.IO) {
            try {
                val userRef = firestore.collection(COLLECTION_USERS).document(userId)
                val snapshot = userRef.get().await()

                if (!snapshot.exists()) {
                    val newUser = KuoteXUserDoc(
                        userId = userId,
                        username = username,
                        displayName = displayName,
                        balance = 1000L,
                        role = role,
                        vipStatus = false,
                        vipExpiration = 0L,
                        availableBoostVotes = 0,
                        allocatedBoosts = emptyList(),
                        pinnedGiftsCount = 0,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    userRef.set(newUser).await()
                    _currentUserState.value = newUser
                } else {
                    val userDoc = snapshot.toObject(KuoteXUserDoc::class.java)
                    _currentUserState.value = userDoc
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user with Firestore: ${e.message}", e)
                if (_currentUserState.value == null) {
                    _currentUserState.value = KuoteXUserDoc(
                        userId = userId,
                        username = username,
                        displayName = displayName,
                        balance = 1000L,
                        role = role
                    )
                }
            }
        }
    }

    /**
     * ATOMIC TRANSACTION: Process Gift Purchase and Transfer to Profile Header
     */
    suspend fun processGiftPurchaseAtomic(
        senderUserId: String,
        targetUserId: String,
        catalogGiftId: String,
        idempotencyKey: String,
        message: String = "",
        pinToHeader: Boolean = true,
        isAnonymous: Boolean = false
    ): Result<KuoteXUserGiftDoc> = withContext(Dispatchers.IO) {
        val txId = "tx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val userGiftId = "ug_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = System.currentTimeMillis()

        try {
            val userGiftResult = firestore.runTransaction { transaction ->
                val idempotencyRef = firestore.collection(COLLECTION_IDEMPOTENCY).document(idempotencyKey)
                val idemSnap = transaction.get(idempotencyRef)
                if (idemSnap.exists()) {
                    throw DuplicateTransactionException("Idempotency key already processed: $idempotencyKey")
                }

                val senderRef = firestore.collection(COLLECTION_USERS).document(senderUserId)
                val targetRef = firestore.collection(COLLECTION_USERS).document(targetUserId)
                val catalogRef = firestore.collection(COLLECTION_GIFTS_CATALOG).document(catalogGiftId)
                val ledgerRef = firestore.collection(COLLECTION_LEDGER_TX).document(txId)
                val userGiftRef = firestore.collection(COLLECTION_USER_GIFTS).document(userGiftId)

                val senderSnap = transaction.get(senderRef)
                val targetSnap = transaction.get(targetRef)
                val catalogSnap = transaction.get(catalogRef)

                val senderBalance = senderSnap.getLong("balance") ?: (_currentUserState.value?.balance ?: 1000L)
                val catalogData = catalogSnap.toObject(KuoteXCatalogGiftDoc::class.java)
                    ?: _catalogGifts.value.find { it.catalogGiftId == catalogGiftId }
                    ?: throw IllegalArgumentException("Gift $catalogGiftId not found in catalog")

                val giftPrice = catalogData.price
                if (senderBalance < giftPrice) {
                    throw InsufficientBalanceException("Insufficient balance ($senderBalance < $giftPrice)")
                }

                val availSupply = catalogData.availableSupply
                if (availSupply != -1L && availSupply <= 0) {
                    throw GiftSoldOutException("Gift is completely sold out")
                }

                // 1. Deduct sender balance
                val newSenderBalance = senderBalance - giftPrice
                transaction.update(senderRef, mapOf(
                    "balance" to newSenderBalance,
                    "updated_at" to now
                ))

                // 2. Decrement catalog supply if limited
                if (availSupply > 0) {
                    transaction.update(catalogRef, "available_supply", availSupply - 1)
                }

                // 3. Pin logic for receiver profile
                val targetPinnedCount = (targetSnap.getLong("pinned_gifts_count") ?: 0L).toInt()
                val shouldPin = pinToHeader && (targetPinnedCount < 6)

                if (shouldPin) {
                    transaction.update(targetRef, mapOf(
                        "pinned_gifts_count" to targetPinnedCount + 1,
                        "updated_at" to now
                    ))
                }

                // 4. Create User Gift Document
                val userGiftDoc = KuoteXUserGiftDoc(
                    userGiftId = userGiftId,
                    catalogGiftId = catalogGiftId,
                    senderId = if (isAnonymous) "anonymous" else senderUserId,
                    receiverId = targetUserId,
                    isPinnedToHeader = shouldPin,
                    pinOrderIndex = if (shouldPin) targetPinnedCount else -1,
                    upgradeLevel = 1,
                    transferable = false,
                    message = message,
                    isAnonymous = isAnonymous,
                    acquiredAt = now,
                    cachedTitle = catalogData.title,
                    cachedEmoji = catalogData.emojiIcon,
                    cachedColorHex = catalogData.backdropColorHex
                )
                transaction.set(userGiftRef, userGiftDoc)

                // 5. Append Immutable Ledger Record
                val ledgerDoc = KuoteXLedgerTxDoc(
                    txId = txId,
                    idempotencyKey = idempotencyKey,
                    type = LedgerTransactionType.GIFT_PURCHASE.value,
                    fromUserId = senderUserId,
                    toUserId = targetUserId,
                    amount = giftPrice,
                    fee = 0L,
                    status = LedgerTransactionStatus.COMMITTED.value,
                    metadata = mapOf(
                        "catalog_gift_id" to catalogGiftId,
                        "user_gift_id" to userGiftId,
                        "is_pinned" to shouldPin
                    ),
                    createdAt = now
                )
                transaction.set(ledgerRef, ledgerDoc)

                // 6. Mark idempotency key
                transaction.set(idempotencyRef, mapOf(
                    "tx_id" to txId,
                    "user_id" to senderUserId,
                    "created_at" to now
                ))

                userGiftDoc
            }.await()

            // Update local state
            _currentUserState.update { current ->
                current?.let {
                    if (it.userId == senderUserId) {
                        it.copy(balance = it.balance - (_catalogGifts.value.find { g -> g.catalogGiftId == catalogGiftId }?.price ?: 100L))
                    } else it
                }
            }

            _pinnedGiftsMap.update { currentMap ->
                val existing = currentMap[targetUserId]?.toMutableList() ?: mutableListOf()
                if (userGiftResult.isPinnedToHeader) {
                    existing.add(userGiftResult)
                }
                currentMap + (targetUserId to existing)
            }

            Result.success(userGiftResult)
        } catch (e: Exception) {
            Log.e(TAG, "Transaction failed for gift purchase: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * ATOMIC TRANSACTION: Activate or Extend KuoteX VIP Subscription
     */
    suspend fun activateVipSubscriptionAtomic(
        userId: String,
        months: Int = 1,
        idempotencyKey: String,
        price: Long = 300L
    ): Result<KuoteXUserDoc> = withContext(Dispatchers.IO) {
        val txId = "tx_vip_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val durationMs = months * 30L * 86400000L

        try {
            val updatedUser = firestore.runTransaction { transaction ->
                val idempotencyRef = firestore.collection(COLLECTION_IDEMPOTENCY).document(idempotencyKey)
                if (transaction.get(idempotencyRef).exists()) {
                    throw DuplicateTransactionException("Idempotency key already processed")
                }

                val userRef = firestore.collection(COLLECTION_USERS).document(userId)
                val ledgerRef = firestore.collection(COLLECTION_LEDGER_TX).document(txId)
                val userSnap = transaction.get(userRef)

                val currentBalance = userSnap.getLong("balance") ?: (_currentUserState.value?.balance ?: 1000L)
                if (currentBalance < price) {
                    throw InsufficientBalanceException("Insufficient balance for VIP subscription ($currentBalance < $price)")
                }

                val currentVipExp = userSnap.getLong("vip_expiration") ?: 0L
                val newVipExp = (if (currentVipExp > now) currentVipExp else now) + durationMs
                val currentVotes = (userSnap.getLong("available_boost_votes") ?: 0L).toInt()
                val newVotes = currentVotes + (4 * months)

                val newBalance = currentBalance - price

                transaction.update(userRef, mapOf(
                    "balance" to newBalance,
                    "vip_status" to true,
                    "vip_expiration" to newVipExp,
                    "available_boost_votes" to newVotes,
                    "updated_at" to now
                ))

                // Immutable Ledger Entry
                val ledgerDoc = KuoteXLedgerTxDoc(
                    txId = txId,
                    idempotencyKey = idempotencyKey,
                    type = LedgerTransactionType.VIP_SUBSCRIPTION.value,
                    fromUserId = userId,
                    toUserId = "system_kuotex_vip",
                    amount = price,
                    status = LedgerTransactionStatus.COMMITTED.value,
                    metadata = mapOf("months" to months, "vip_expiration" to newVipExp),
                    createdAt = now
                )
                transaction.set(ledgerRef, ledgerDoc)

                transaction.set(idempotencyRef, mapOf("tx_id" to txId, "created_at" to now))

                val userDoc = userSnap.toObject(KuoteXUserDoc::class.java) ?: KuoteXUserDoc(userId = userId)
                userDoc.copy(
                    balance = newBalance,
                    vipStatus = true,
                    vipExpiration = newVipExp,
                    availableBoostVotes = newVotes,
                    updatedAt = now
                )
            }.await()

            _currentUserState.value = updatedUser
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed VIP activation: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * ATOMIC TRANSACTION: Apply Channel Boost with Privilege Validation & Level Recalculation
     */
    suspend fun applyChannelBoostAtomic(
        userId: String,
        channelId: String,
        votesToApply: Int = 1
    ): Result<KuoteXChannelDoc> = withContext(Dispatchers.IO) {
        val txId = "tx_boost_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()

        try {
            val updatedChannel = firestore.runTransaction { transaction ->
                val userRef = firestore.collection(COLLECTION_USERS).document(userId)
                val channelRef = firestore.collection(COLLECTION_CHANNELS).document(channelId)
                val ledgerRef = firestore.collection(COLLECTION_LEDGER_TX).document(txId)

                val userSnap = transaction.get(userRef)
                val channelSnap = transaction.get(channelRef)

                val userDoc = userSnap.toObject(KuoteXUserDoc::class.java)
                    ?: _currentUserState.value
                    ?: throw IllegalArgumentException("User document $userId not found")

                // Privilege Validation
                if (!userDoc.hasBoostPrivilege()) {
                    throw UnauthorizedBoostException("Only KuoteX VIP or Admins/Developers can boost channels.")
                }

                val availableVotes = (userSnap.getLong("available_boost_votes") ?: userDoc.availableBoostVotes.toLong()).toInt()
                if (availableVotes < votesToApply && !userDoc.role.equals("developer", ignoreCase = true)) {
                    throw InsufficientBoostVotesException("Insufficient boost votes available ($availableVotes < $votesToApply)")
                }

                val newAvailableVotes = (availableVotes - votesToApply).coerceAtLeast(0)

                // Update allocated boosts list
                val currentAllocated = userDoc.allocatedBoosts.toMutableList()
                val existingIdx = currentAllocated.indexOfFirst { it.channelId == channelId }
                if (existingIdx >= 0) {
                    val old = currentAllocated[existingIdx]
                    currentAllocated[existingIdx] = old.copy(
                        votesCount = old.votesCount + votesToApply,
                        boostedAt = now
                    )
                } else {
                    currentAllocated.add(AllocatedBoost(channelId = channelId, votesCount = votesToApply, boostedAt = now))
                }

                transaction.update(userRef, mapOf(
                    "available_boost_votes" to newAvailableVotes,
                    "allocated_boosts" to currentAllocated,
                    "updated_at" to now
                ))

                // Recalculate Channel Level Progression
                val currentVotes = (channelSnap.getLong("current_votes") ?: 0L).toInt()
                val totalVotes = currentVotes + votesToApply
                val newLevel = KuoteXBoostProgression.calculateLevel(totalVotes)
                val nextRequirement = KuoteXBoostProgression.nextLevelRequirement(totalVotes)

                val channelDoc = KuoteXChannelDoc(
                    channelId = channelId,
                    title = channelSnap.getString("title") ?: "Канал",
                    currentVotes = totalVotes,
                    level = newLevel,
                    nextLevelRequiredVotes = nextRequirement,
                    customColorUnlocked = newLevel >= 1,
                    statusEmojiUnlocked = newLevel >= 2,
                    wallpaperUnlocked = newLevel >= 3,
                    storiesPerDayLimit = (newLevel * 2).coerceAtLeast(0),
                    updatedAt = now
                )

                transaction.set(channelRef, channelDoc, SetOptions.merge())

                // Immutable Ledger entry
                val ledgerDoc = KuoteXLedgerTxDoc(
                    txId = txId,
                    idempotencyKey = "boost_${userId}_${channelId}_${now}",
                    type = LedgerTransactionType.CHANNEL_BOOST.value,
                    fromUserId = userId,
                    toUserId = channelId,
                    amount = votesToApply.toLong(),
                    status = LedgerTransactionStatus.COMMITTED.value,
                    metadata = mapOf("votes" to votesToApply, "channel_level" to newLevel),
                    createdAt = now
                )
                transaction.set(ledgerRef, ledgerDoc)

                channelDoc
            }.await()

            // Update user state locally
            _currentUserState.update { curr ->
                curr?.copy(
                    availableBoostVotes = (curr.availableBoostVotes - votesToApply).coerceAtLeast(0)
                )
            }

            Result.success(updatedChannel)
        } catch (e: Exception) {
            Log.e(TAG, "Channel boost transaction failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * ATOMIC TRANSACTION: Upgrade Pinned User Gift Level (Phase 4)
     */
    suspend fun upgradeUserGiftAtomic(
        userId: String,
        userGiftId: String,
        upgradeCostStars: Long = 50L
    ): Result<KuoteXUserGiftDoc> = withContext(Dispatchers.IO) {
        val txId = "tx_gift_upg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()

        try {
            val updatedUserGift = firestore.runTransaction { transaction ->
                val userRef = firestore.collection(COLLECTION_USERS).document(userId)
                val giftRef = firestore.collection(COLLECTION_USER_GIFTS).document(userGiftId)
                val ledgerRef = firestore.collection(COLLECTION_LEDGER_TX).document(txId)

                val userSnap = transaction.get(userRef)
                val giftSnap = transaction.get(giftRef)

                val currentBalance = userSnap.getLong("balance") ?: (_currentUserState.value?.balance ?: 1000L)
                if (currentBalance < upgradeCostStars) {
                    throw InsufficientBalanceException("Insufficient Stars balance to upgrade gift ($currentBalance < $upgradeCostStars)")
                }

                val currentGift = giftSnap.toObject(KuoteXUserGiftDoc::class.java)
                    ?: _pinnedGiftsMap.value[userId]?.find { it.userGiftId == userGiftId }
                    ?: throw IllegalArgumentException("User gift $userGiftId not found")

                val newLevel = (currentGift.upgradeLevel + 1).coerceAtMost(5)
                val upgradedDoc = currentGift.copy(
                    upgradeLevel = newLevel
                )

                transaction.update(userRef, "balance", currentBalance - upgradeCostStars)
                transaction.set(giftRef, upgradedDoc, SetOptions.merge())

                // Immutable Ledger
                val ledgerDoc = KuoteXLedgerTxDoc(
                    txId = txId,
                    idempotencyKey = "upg_${userGiftId}_lvl${newLevel}_$now",
                    type = LedgerTransactionType.GIFT_UPGRADE.value,
                    fromUserId = userId,
                    toUserId = "system_gift_vault",
                    amount = upgradeCostStars,
                    status = LedgerTransactionStatus.COMMITTED.value,
                    metadata = mapOf("user_gift_id" to userGiftId, "new_level" to newLevel),
                    createdAt = now
                )
                transaction.set(ledgerRef, ledgerDoc)

                upgradedDoc
            }.await()

            // Update local state
            _currentUserState.update { curr ->
                curr?.let { it.copy(balance = (it.balance - upgradeCostStars).coerceAtLeast(0L)) }
            }

            _pinnedGiftsMap.update { currentMap ->
                val list = currentMap[userId]?.toMutableList() ?: mutableListOf()
                val idx = list.indexOfFirst { it.userGiftId == userGiftId }
                if (idx >= 0) {
                    list[idx] = updatedUserGift
                } else {
                    list.add(updatedUserGift)
                }
                currentMap + (userId to list)
            }

            Result.success(updatedUserGift)
        } catch (e: Exception) {
            Log.e(TAG, "Gift upgrade failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * ATOMIC TRANSACTION: Vote in Telegram-style channel/group poll
     */
    suspend fun voteInPollAtomic(
        pollId: String,
        chatId: String,
        userId: String,
        selectedOptionIds: List<Int>
    ): Result<KuoteXPollVoteDoc> = withContext(Dispatchers.IO) {
        val voteDocId = "${pollId}_${userId}"
        val now = System.currentTimeMillis()

        try {
            val voteDoc = firestore.runTransaction { transaction ->
                val voteRef = firestore.collection(COLLECTION_POLL_VOTES).document(voteDocId)
                val existingVote = transaction.get(voteRef)
                if (existingVote.exists()) {
                    throw DuplicateTransactionException("User $userId has already voted in poll $pollId")
                }

                val newVote = KuoteXPollVoteDoc(
                    voteId = voteDocId,
                    pollId = pollId,
                    chatId = chatId,
                    userId = userId,
                    selectedOptionIds = selectedOptionIds,
                    timestamp = now
                )
                transaction.set(voteRef, newVote)
                newVote
            }.await()

            Result.success(voteDoc)
        } catch (e: Exception) {
            Log.e(TAG, "Poll vote transaction error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Top-up user internal currency balance (KuoteX Stars / Coins)
     */
    suspend fun topUpUserBalanceAtomic(
        userId: String,
        amount: Long,
        providerTxId: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        val txId = "topup_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()

        try {
            val newBalance = firestore.runTransaction { transaction ->
                val idempotencyRef = firestore.collection(COLLECTION_IDEMPOTENCY).document(providerTxId)
                if (transaction.get(idempotencyRef).exists()) {
                    throw DuplicateTransactionException("Provider tx $providerTxId already credited")
                }

                val userRef = firestore.collection(COLLECTION_USERS).document(userId)
                val ledgerRef = firestore.collection(COLLECTION_LEDGER_TX).document(txId)
                val userSnap = transaction.get(userRef)

                val currentBalance = userSnap.getLong("balance") ?: (_currentUserState.value?.balance ?: 1000L)
                val updatedBalance = currentBalance + amount

                transaction.update(userRef, mapOf(
                    "balance" to updatedBalance,
                    "updated_at" to now
                ))

                val ledgerDoc = KuoteXLedgerTxDoc(
                    txId = txId,
                    idempotencyKey = providerTxId,
                    type = LedgerTransactionType.BALANCE_TOPUP.value,
                    fromUserId = "external_payment_gateway",
                    toUserId = userId,
                    amount = amount,
                    status = LedgerTransactionStatus.COMMITTED.value,
                    metadata = mapOf("provider_tx_id" to providerTxId),
                    createdAt = now
                )
                transaction.set(ledgerRef, ledgerDoc)
                transaction.set(idempotencyRef, mapOf("tx_id" to txId, "created_at" to now))

                updatedBalance
            }.await()

            _currentUserState.update { it?.copy(balance = newBalance) }
            Result.success(newBalance)
        } catch (e: Exception) {
            Log.e(TAG, "Balance top-up error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time Pinned Gifts for a profile header.
     */
    fun observePinnedGifts(userId: String): Flow<List<KuoteXUserGiftDoc>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_USER_GIFTS)
            .whereEqualTo("receiver_id", userId)
            .whereEqualTo("is_pinned_to_header", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to pinned gifts: ${error.message}")
                    trySend(_pinnedGiftsMap.value[userId] ?: emptyList())
                    return@addSnapshotListener
                }
                val gifts = snapshot?.documents?.mapNotNull { it.toObject(KuoteXUserGiftDoc::class.java) } ?: emptyList()
                val sorted = gifts.sortedBy { it.pinOrderIndex }
                _pinnedGiftsMap.update { it + (userId to sorted) }
                trySend(sorted)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observes a channel's boost level, votes, and unlocked perks in real-time.
     */
    fun observeChannelEcosystem(channelId: String): Flow<KuoteXChannelDoc?> = callbackFlow {
        val listener = firestore.collection(COLLECTION_CHANNELS).document(channelId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing channel ecosystem: ${error.message}")
                    return@addSnapshotListener
                }
                val doc = snapshot?.toObject(KuoteXChannelDoc::class.java)
                trySend(doc)
            }
        awaitClose { listener.remove() }
    }
}
