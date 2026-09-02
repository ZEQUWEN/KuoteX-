package com.example.ui.gifts

import androidx.compose.ui.graphics.Color
import com.example.data.ecosystem.KuoteXCatalogGiftDoc
import com.example.data.ecosystem.KuoteXUserGiftDoc

/**
 * PinnedGift - Client-side UI Model for Profile Header Pinned Exclusive Gifts.
 * Seamlessly maps backend gift IDs, upgrade levels, and animation / vector configurations.
 */
data class PinnedGift(
    val id: String,
    val catalogGiftId: String,
    val title: String,
    val senderName: String,
    val receiverId: String,
    val upgradeLevel: Int = 1,
    val maxUpgradeLevel: Int = 5,
    val animationAssetUrl: String = "",
    val vectorConfigString: String = "",
    val backdropColorHex: String = "#1E1B4B",
    val accentGlowHex: String = "#7C3AED",
    val emojiIcon: String = "🎁",
    val isExclusive: Boolean = true,
    val pinOrderIndex: Int = 0,
    val message: String = "",
    val acquiredAt: Long = System.currentTimeMillis()
) {
    /**
     * Rating / Rarity based on upgrade level and exclusivity.
     */
    val rarityTier: RarityTier
        get() = when {
            upgradeLevel >= 5 -> RarityTier.MYTHIC
            upgradeLevel >= 4 -> RarityTier.LEGENDARY
            upgradeLevel >= 3 -> RarityTier.EPIC
            isExclusive -> RarityTier.EXCLUSIVE
            else -> RarityTier.RARE
        }

    val upgradeStars: String
        get() = "★".repeat(upgradeLevel.coerceIn(1, 5))

    val parsedBackdropColor: Color
        get() = parseHexColor(backdropColorHex, Color(0xFF1E1B4B))

    val parsedAccentColor: Color
        get() = parseHexColor(accentGlowHex, Color(0xFF8B5CF6))

    companion object {
        private fun parseHexColor(hex: String?, fallback: Color): Color {
            if (hex.isNullOrBlank()) return fallback
            return try {
                val clean = hex.removePrefix("#")
                val colorInt = if (clean.length == 6) {
                    (0xFF000000 or clean.toLong(16)).toInt()
                } else {
                    clean.toLong(16).toInt()
                }
                Color(colorInt)
            } catch (_: Exception) {
                fallback
            }
        }

        /**
         * Seamlessly maps from Firestore KuoteXUserGiftDoc & Catalog metadata.
         */
        fun fromUserGift(
            userGift: KuoteXUserGiftDoc,
            catalog: KuoteXCatalogGiftDoc? = null
        ): PinnedGift {
            val title = catalog?.title ?: userGift.cachedTitle
            val emoji = catalog?.emojiIcon ?: userGift.cachedEmoji
            val backdrop = catalog?.backdropColorHex ?: userGift.cachedColorHex
            val isExcl = catalog?.isExclusive ?: true
            val lottie = catalog?.lottieAssetUrl ?: ""

            val accent = when (userGift.upgradeLevel) {
                5 -> "#FFD700" // Gold / Mythic
                4 -> "#F43F5E" // Crimson / Legendary
                3 -> "#A855F7" // Purple / Epic
                2 -> "#3B82F6" // Blue / Rare
                else -> "#10B981" // Emerald
            }

            return PinnedGift(
                id = userGift.userGiftId,
                catalogGiftId = userGift.catalogGiftId,
                title = title,
                senderName = if (userGift.isAnonymous) "Аноним" else userGift.senderId,
                receiverId = userGift.receiverId,
                upgradeLevel = userGift.upgradeLevel,
                maxUpgradeLevel = catalog?.maxUpgradeLevel ?: 5,
                animationAssetUrl = lottie,
                vectorConfigString = "M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5",
                backdropColorHex = backdrop,
                accentGlowHex = accent,
                emojiIcon = emoji,
                isExclusive = isExcl,
                pinOrderIndex = userGift.pinOrderIndex,
                message = userGift.message,
                acquiredAt = userGift.acquiredAt
            )
        }

        /**
         * Default sample pinned gifts for profile demonstration.
         */
        fun samplePinnedGifts(): List<PinnedGift> = listOf(
            PinnedGift(
                id = "ug_cyber_dragon_sample",
                catalogGiftId = "gift_cyber_dragon_001",
                title = "Cyber Dragon 2026",
                senderName = "KuoteX Official",
                receiverId = "me",
                upgradeLevel = 5,
                maxUpgradeLevel = 5,
                animationAssetUrl = "cyber_dragon.json",
                backdropColorHex = "#1E1B4B",
                accentGlowHex = "#FFD700",
                emojiIcon = "🐉",
                isExclusive = true,
                pinOrderIndex = 0,
                message = "Эксклюзивный подарок основателю KuoteX!",
                acquiredAt = System.currentTimeMillis() - 86400000L * 3
            ),
            PinnedGift(
                id = "ug_golden_crown_sample",
                catalogGiftId = "gift_golden_crown_002",
                title = "Золотая Корона VIP",
                senderName = "Alexey M.",
                receiverId = "me",
                upgradeLevel = 3,
                maxUpgradeLevel = 5,
                animationAssetUrl = "golden_crown.json",
                backdropColorHex = "#281904",
                accentGlowHex = "#F59E0B",
                emojiIcon = "👑",
                isExclusive = true,
                pinOrderIndex = 1,
                message = "С днем рождения!",
                acquiredAt = System.currentTimeMillis() - 86400000L * 10
            ),
            PinnedGift(
                id = "ug_neon_diamond_sample",
                catalogGiftId = "gift_neon_diamond_003",
                title = "Неоновый Алмаз",
                senderName = "Elena",
                receiverId = "me",
                upgradeLevel = 2,
                maxUpgradeLevel = 3,
                animationAssetUrl = "neon_diamond.json",
                backdropColorHex = "#062826",
                accentGlowHex = "#06B6D4",
                emojiIcon = "💎",
                isExclusive = false,
                pinOrderIndex = 2,
                message = "За отличную работу над проектом 🚀",
                acquiredAt = System.currentTimeMillis() - 86400000L * 15
            ),
            PinnedGift(
                id = "ug_cosmic_rocket_sample",
                catalogGiftId = "gift_cosmic_rocket_004",
                title = "Космическая Ракета",
                senderName = "Аноним",
                receiverId = "me",
                upgradeLevel = 1,
                maxUpgradeLevel = 3,
                animationAssetUrl = "cosmic_rocket.json",
                backdropColorHex = "#1E1035",
                accentGlowHex = "#EC4899",
                emojiIcon = "🚀",
                isExclusive = false,
                pinOrderIndex = 3,
                message = "Только вперед!",
                acquiredAt = System.currentTimeMillis() - 86400000L * 25
            )
        )
    }
}

enum class RarityTier(val title: String, val colorHex: String) {
    COMMON("Обычный", "#94A3B8"),
    RARE("Редкий", "#3B82F6"),
    EPIC("Эпический", "#A855F7"),
    LEGENDARY("Легендарный", "#F43F5E"),
    MYTHIC("Мифический", "#FFD700"),
    EXCLUSIVE("Эксклюзив", "#F59E0B")
}
