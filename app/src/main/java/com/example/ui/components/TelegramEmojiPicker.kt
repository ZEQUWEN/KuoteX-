package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Telegram Emoji Model & Full Unicode + Custom Pack Repository
 */
data class EmojiItem(
    val emoji: String,
    val name: String = "",
    val keywords: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val packName: String? = null
)

data class EmojiCategory(
    val id: String,
    val title: String,
    val icon: String,
    val emojis: List<EmojiItem>
)

object TelegramEmojiData {

    val recentEmojis = mutableStateListOf(
        EmojiItem("🔥", "Огонь", listOf("fire", "огонь", "пламя")),
        EmojiItem("⚡", "Молния", listOf("lightning", "молния", "ток")),
        EmojiItem("❤️", "Красное сердце", listOf("heart", "любовь", "сердце")),
        EmojiItem("😂", "Смех до слез", listOf("joy", "смех", "ржака")),
        EmojiItem("🥰", "Влюбленный", listOf("love", "мило", "сердца")),
        EmojiItem("👍", "Палец вверх", listOf("like", "лайк", "класс")),
        EmojiItem("💎", "Бриллиант", listOf("gem", "алмаз", "кристалл")),
        EmojiItem("👑", "Корона", listOf("crown", "король", "вип")),
        EmojiItem("✨", "Искры", listOf("sparkles", "блеск", "магия")),
        EmojiItem("🚀", "Ракета", listOf("rocket", "старт", "космос")),
        EmojiItem("🪫", "Разряженная батарея", listOf("battery", "энергия", "статус")),
        EmojiItem("🎉", "Праздник", listOf("party", "туса", "салют")),
        EmojiItem("😎", "Крутой в очках", listOf("cool", "крутой", "очки")),
        EmojiItem("🤝", "Рукопожатие", listOf("deal", "согласие", "дружба"))
    )

    fun addRecent(emoji: EmojiItem) {
        val existingIndex = recentEmojis.indexOfFirst { it.emoji == emoji.emoji }
        if (existingIndex != -1) {
            recentEmojis.removeAt(existingIndex)
        }
        recentEmojis.add(0, emoji)
        if (recentEmojis.size > 28) {
            recentEmojis.removeAt(recentEmojis.size - 1)
        }
    }

    val categories: List<EmojiCategory> by lazy {
        listOf(
            EmojiCategory(
                id = "smileys",
                title = "Смайлы и эмоции",
                icon = "😀",
                emojis = listOf(
                    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "🥹", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗",
                    "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕",
                    "🙁", "☹️", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😮‍💨", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨",
                    "😰", "😥", "😓", "🫣", "🤗", "🫡", "🤔", "🫢", "🤫", "🤥", "😶", "😶‍🌫️", "😐", "😑", "😬", "🫨", "🫠", "🙄", "😯", "😦",
                    "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "😵‍💫", "🫥", "🤐", "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠",
                    "😈", "👿", "👹", "👺", "🤡", "💩", "👻", "💀", "☠️", "👽", "👾", "🤖", "🎃", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾"
                ).map { EmojiItem(it) }
            ),
            EmojiCategory(
                id = "people",
                title = "Люди и жесты",
                icon = "👋",
                emojis = listOf(
                    "👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
                    "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳",
                    "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🫀", "🫁", "🧠", "🦷", "🦴", "👀", "👁️", "👅", "👄", "🫦", "👶", "🧒",
                    "👦", "👧", "🧑", "👱", "👨", "🧔", "🧔‍♂️", "🧔‍♀️", "👨‍🦰", "👨‍🦱", "👨‍🦳", "👨‍🦲", "👩", "👩‍🦰", "🧑‍🦰", "👩‍🦱", "🧑‍🦱", "👩‍🦳", "🧑‍🦳", "👩‍🦲",
                    "🧑‍🦲", "👱‍♀️", "👱‍♂️", "🧓", "👴", "👵", "🙍", "🙍‍♂️", "🙍‍♀️", "🙎", "🙎‍♂️", "🙎‍♀️", "🙅", "🙅‍♂️", "🙅‍♀️", "🙆", "🙆‍♂️", "🙆‍♀️", "💁", "💁‍♂️"
                ).map { EmojiItem(it) }
            ),
            EmojiCategory(
                id = "animals",
                title = "Животные и природа",
                icon = "🐻",
                emojis = listOf(
                    "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊",
                    "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🪱", "🐛", "🦋", "🐌",
                    "🐞", "🐜", "🪰", "🪲", "🪳", "🦟", "🦗", "🕷️", "🕸️", "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🦐", "🦞", "🦀",
                    "🪼", "🐡", "🐠", "🐟", "🐬", "🐳", "🐋", "🦈", "🦭", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🦣", "🐘", "🦛", "🦏", "🐪",
                    "🐫", "🦒", "🦘", "🦬", "🐃", "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🦙", "🐐", "🦌", "🐕", "🐩", "🦮", "🐕‍🦺", "🐈", "🐈‍⬛",
                    "🪶", "🐓", "🦃", "🦤", "🦚", "🦜", "🦢", "🦩", "🕊️", "🐇", "🦝", "🦨", "🦡", "🦫", "🦦", "🦥", "🐁", "🐀", "🐿️", "🦔",
                    "🌲", "🌳", "🌴", "🌵", "🌾", "🌿", "☘️", "🍀", "🍁", "🍂", "🍃", "🍄", "🪨", "🪵", "🌺", "🌸", "🌼", "🌻", "🌞", "🌝",
                    "🌛", "🌜", "🌚", "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓", "🌔", "🌙", "🌎", "🌍", "🌏", "🪐", "💫", "⭐", "🌟", "✨",
                    "⚡", "☄️", "💥", "🔥", "🌪️", "🌈", "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌧️", "⛈️", "🌩️", "🌨️", "❄️", "☃️", "⛄", "🌬️"
                ).map { EmojiItem(it) }
            ),
            EmojiCategory(
                id = "food",
                title = "Еда и напитки",
                icon = "🍔",
                emojis = listOf(
                    "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑",
                    "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠", "🫘", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚",
                    "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🦴", "🌭", "🍔", "🍟", "🍕", "🫓", "🥪", "🥙", "🧆", "🌮", "🌯", "🫔",
                    "🥗", "🥘", "🫕", "🥫", "🍝", "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🦪", "🍤", "🍙", "🍚", "🍘", "🍥", "🥠", "🥮", "🍢",
                    "🍡", "🍧", "🍨", "🍦", "🥧", "🧁", "🍰", "🎂", "🍮", "🍭", "🍬", "🍫", "🍿", "🍩", "🍪", "🌰", "🥜", "🍯", "🥛", "🍼",
                    "🫖", "☕", "🍵", "🧃", "🥤", "🧋", "🫗", "🍶", "🍺", "🍻", "🥂", "🍷", "🫗", "🥃", "🍸", "🍹", "🧉", "🍾", "🧊", "🥄"
                ).map { EmojiItem(it) }
            ),
            EmojiCategory(
                id = "activity",
                title = "Активности и спорт",
                icon = "⚽",
                emojis = listOf(
                    "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "🪃", "🥅", "⛳",
                    "🪁", "🏹", "🎣", "🤿", "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🥌", "🎿", "⛷️", "🏂", "🪂", "🏋️", "🤼", "🤸", "🤺",
                    "⛹️", "🤾", "🧗", "🏇", "🏄", "🏊", "🤽", "🚣", "🧗", "🚵", "🚴", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "🏵️", "🎗️", "🎫",
                    "🎟️", "🎪", "🤹", "🎭", "🩰", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🪘", "🎷", "🎺", "🪗", "🎸", "🪕", "🎻", "🎲",
                    "♟️", "🎯", "🎳", "🎮", "🎰", "🧩"
                ).map { EmojiItem(it) }
            ),
            EmojiCategory(
                id = "travel",
                title = "Путешествия и места",
                icon = "🚗",
                emojis = listOf(
                    "🚗", "🚙", "🚕", "🛺", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🛵", "🏍️", "🛞", "🚲", "🛴",
                    "🛹", "🛼", "🚁", "🛸", "✈️", "🛫", "🛬", "🪂", "🚀", "🛰️", "⛵", "🚤", "🛥️", "🛳️", "⛴️", "🚢", "⚓", "🛟", "🪝", "⛽",
                    "🚨", "🚥", "🚦", "🛑", "🚧", "🗿", "🗽", "🗼", "🏰", "🏯", "🏟️", "🎡", "🎢", "🎠", "⛲", "⛱️", "🏖️", "🏝️", "🏜️", "🌋",
                    "⛰️", "🏔️", "🗻", "🏕️", "⛺", "🛖", "🏠", "🏡", "🏘️", "🏚️", "🏗️", "🏢", "🏬", "🏣", "🏤", "🏥", "🏦", "🏨", "🏪", "🏫"
                ).map { EmojiItem(it) }
            ),
            EmojiCategory(
                id = "objects",
                title = "Предметы и объекты",
                icon = "💡",
                emojis = listOf(
                    "💡", "🔦", "🏮", "🪔", "🕯️", "🪩", "🔋", "🪫", "🔌", "💻", "🖥️", "🖨️", "⌨️", "🖱️", "🖲️", "💽", "💾", "💿", "📀", "📼",
                    "📷", "📸", "📹", "🎥", "📽️", "🎞️", "📞", "☎️", "📟", "📠", "📺", "📻", "🎙️", "🎚️", "🎛️", "⏱️", "⏲️", "⏰", "🕰️", "⌛",
                    "⏳", "📡", "🔋", "🧭", "💎", "🔮", "🪄", "📿", "🧿", "💈", "⚗️", "🔭", "🔬", "🕳️", "💊", "💉", "🩸", "🩹", "🩺", "🩻",
                    "🚪", "🛗", "🪞", "🪟", "🛏️", "🛋️", "🪑", "🚽", "🪠", "🚿", "🛁", "🪤", "🪒", "🧴", "🧷", "🧹", "🧺", "🧻", "🪣", "🧼",
                    "🫧", "🪥", "🧽", "🧯", "🛒", "🚬", "⚰️", "🪦", "⚱️", "🏺", "🎁", "🎈", "🎉", "🎊", "🎏", "🎀", "🧧", "✉️", "📦", "📫"
                ).map { EmojiItem(it) }
            ),
            EmojiCategory(
                id = "symbols",
                title = "Символы и знаки",
                icon = "🔣",
                emojis = listOf(
                    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝",
                    "💟", "☮️", "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🛐", "⛎", "♈", "♉", "♊", "♋", "♌", "♍", "♎",
                    "♏", "♐", "♑", "♒", "♓", "🆔", "⚛️", "🉑", "☢️", "☣️", "📴", "📳", "🈶", "🈚", "🈸", "🈺", "🈷️", "✴️", "VS", "🈴",
                    "💮", "🉐", "㊙️", "㊗️", "🈡", "🔴", "🟠", "🟡", "🟢", "🔵", "🟣", "⚫", "⚪", "🟤", "🔺", "🔻", "💠", "🔘", "🔳", "🔲",
                    "🏁", "🚩", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️", "💯", "🔠", "🔡", "🔢", "🔣", "🔤", "🅰️", "🆎", "🅱️", "🆑", "🆒", "🆓"
                ).map { EmojiItem(it) }
            ),
            // --- Custom Telegram Packs ---
            EmojiCategory(
                id = "pack_duck",
                title = "🦆 Duck Emoji Pack",
                icon = "🦆",
                emojis = listOf(
                    EmojiItem("🦆", "Duck Cool", isCustom = true, packName = "Duck"),
                    EmojiItem("🐥", "Baby Duck", isCustom = true, packName = "Duck"),
                    EmojiItem("🦆‍🔥", "Duck Fire", isCustom = true, packName = "Duck"),
                    EmojiItem("🪿", "Goose Honk", isCustom = true, packName = "Duck"),
                    EmojiItem("🍗", "Duck Snack", isCustom = true, packName = "Duck"),
                    EmojiItem("🌊", "Duck Lake", isCustom = true, packName = "Duck"),
                    EmojiItem("🎩", "Duck Gentleman", isCustom = true, packName = "Duck"),
                    EmojiItem("🕶️", "Duck Matrix", isCustom = true, packName = "Duck")
                )
            ),
            EmojiCategory(
                id = "pack_cyber",
                title = "🪐 Neon Cyber Pack",
                icon = "👾",
                emojis = listOf(
                    EmojiItem("👾", "Pixel Cyber", isCustom = true, packName = "Cyber"),
                    EmojiItem("⚡", "Electric Neon", isCustom = true, packName = "Cyber"),
                    EmojiItem("🤖", "Android Bot", isCustom = true, packName = "Cyber"),
                    EmojiItem("🧬", "DNA Neon", isCustom = true, packName = "Cyber"),
                    EmojiItem("🔮", "Holo Orb", isCustom = true, packName = "Cyber"),
                    EmojiItem("🪫", "Cyber Core Empty", isCustom = true, packName = "Cyber"),
                    EmojiItem("🛸", "Cyber Ship", isCustom = true, packName = "Cyber"),
                    EmojiItem("🌐", "Meta Matrix", isCustom = true, packName = "Cyber")
                )
            ),
            EmojiCategory(
                id = "pack_crayons",
                title = "🖍️ Crayons Collection",
                icon = "🖍️",
                emojis = listOf(
                    EmojiItem("🖍️", "Red Crayon", isCustom = true, packName = "Crayons"),
                    EmojiItem("🎨", "Artist Palette", isCustom = true, packName = "Crayons"),
                    EmojiItem("🖌️", "Paintbrush", isCustom = true, packName = "Crayons"),
                    EmojiItem("✏️", "Pencil Sketch", isCustom = true, packName = "Crayons"),
                    EmojiItem("✒️", "Fountain Pen", isCustom = true, packName = "Crayons"),
                    EmojiItem("📝", "Color Memo", isCustom = true, packName = "Crayons"),
                    EmojiItem("🌈", "Rainbow Spectrum", isCustom = true, packName = "Crayons"),
                    EmojiItem("✨", "Color Glitter", isCustom = true, packName = "Crayons")
                )
            ),
            EmojiCategory(
                id = "pack_birthday",
                title = "🎂 Birthday & Celebrations",
                icon = "🎂",
                emojis = listOf(
                    EmojiItem("🎂", "Birthday Cake", isCustom = true, packName = "Birthday"),
                    EmojiItem("🧁", "Cupcake", isCustom = true, packName = "Birthday"),
                    EmojiItem("🎈", "Party Balloon", isCustom = true, packName = "Birthday"),
                    EmojiItem("🎁", "Gift Box", isCustom = true, packName = "Birthday"),
                    EmojiItem("🎉", "Confetti Blast", isCustom = true, packName = "Birthday"),
                    EmojiItem("🥳", "Party Face", isCustom = true, packName = "Birthday"),
                    EmojiItem("🥂", "Cheers Glasses", isCustom = true, packName = "Birthday"),
                    EmojiItem("🍾", "Champagne Pop", isCustom = true, packName = "Birthday")
                )
            )
        )
    }

    fun search(query: String): List<EmojiItem> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()
        val all = categories.flatMap { it.emojis }
        return all.filter { item ->
            item.emoji.contains(trimmed) ||
            item.name.lowercase().contains(trimmed) ||
            item.keywords.any { it.lowercase().contains(trimmed) } ||
            (item.packName?.lowercase()?.contains(trimmed) == true)
        }.distinctBy { it.emoji }
    }
}

/**
 * TelegramEmojiPicker
 * Full-fledged Telegram-style emoji & animated status picker:
 * - High-speed streaming virtualization (LazyColumn with chunked rows)
 * - Synchronized category tab navigation with smooth scrolling
 * - Real-time search with instant filtering
 * - Custom pack badges & support for all Unicode emojis
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramEmojiPicker(
    modifier: Modifier = Modifier,
    selectedEmoji: String? = null,
    onEmojiSelected: (EmojiItem) -> Unit,
    onDismiss: (() -> Unit)? = null,
    title: String = "Выбор эмодзи",
    showRecentTab: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(searchQuery) {
        if (searchQuery.isNotEmpty()) TelegramEmojiData.search(searchQuery) else emptyList()
    }

    val categories = remember { TelegramEmojiData.categories }
    val listState = rememberLazyListState()

    // Active Category tracking
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    // Calculate chunks per category (8 items per row for smooth phone grid)
    val chunkSize = 8

    // Map each category to row chunks
    val categoryRowOffsets = remember(categories, showRecentTab) {
        val offsets = mutableListOf<Int>()
        var currentOffset = 0
        if (showRecentTab && TelegramEmojiData.recentEmojis.isNotEmpty()) {
            offsets.add(currentOffset)
            val recentRows = (TelegramEmojiData.recentEmojis.size + chunkSize - 1) / chunkSize
            currentOffset += 1 + recentRows // header + rows
        }
        categories.forEach { cat ->
            offsets.add(currentOffset)
            val rows = (cat.emojis.size + chunkSize - 1) / chunkSize
            currentOffset += 1 + rows // header + rows
        }
        offsets
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF161E2E))
    ) {
        // Top Header with Drag Handle & Close
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Search Input Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0E131D),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Поиск эмодзи и стикеров...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    singleLine = true
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Очистить",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Category Tab Bar (Sticky Icons)
        if (searchQuery.isEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121926))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showRecentTab && TelegramEmojiData.recentEmojis.isNotEmpty()) {
                    item {
                        val isSelected = selectedCategoryIndex == 0
                        CategoryTabButton(
                            icon = "🕒",
                            title = "Недавние",
                            isSelected = isSelected,
                            onClick = {
                                selectedCategoryIndex = 0
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        )
                    }
                }

                itemsIndexed(categories) { index, cat ->
                    val actualIndex = if (showRecentTab && TelegramEmojiData.recentEmojis.isNotEmpty()) index + 1 else index
                    val isSelected = selectedCategoryIndex == actualIndex
                    CategoryTabButton(
                        icon = cat.icon,
                        title = cat.title,
                        isSelected = isSelected,
                        onClick = {
                            selectedCategoryIndex = actualIndex
                            val targetItemIndex = categoryRowOffsets.getOrElse(actualIndex) { 0 }
                            coroutineScope.launch {
                                listState.animateScrollToItem(targetItemIndex)
                            }
                        }
                    )
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        }

        // Content Area: Virtualized Streaming LazyColumn
        Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 380.dp)) {
            if (searchQuery.isNotEmpty()) {
                // Search Results Grid
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Эмодзи не найдены",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val searchRows = searchResults.chunked(chunkSize)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        items(searchRows) { rowItems ->
                            EmojiGridRow(
                                items = rowItems,
                                selectedEmoji = selectedEmoji,
                                onSelect = { item ->
                                    TelegramEmojiData.addRecent(item)
                                    onEmojiSelected(item)
                                }
                            )
                        }
                    }
                }
            } else {
                // Full Categorized Streaming List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    // 1. Recent Section
                    if (showRecentTab && TelegramEmojiData.recentEmojis.isNotEmpty()) {
                        item {
                            CategoryHeader(title = "Недавние эмодзи")
                        }
                        val recentChunks = TelegramEmojiData.recentEmojis.chunked(chunkSize)
                        items(recentChunks) { rowItems ->
                            EmojiGridRow(
                                items = rowItems,
                                selectedEmoji = selectedEmoji,
                                onSelect = { item ->
                                    TelegramEmojiData.addRecent(item)
                                    onEmojiSelected(item)
                                }
                            )
                        }
                    }

                    // 2. All Categories
                    categories.forEach { category ->
                        item {
                            CategoryHeader(
                                title = category.title,
                                isPack = category.emojis.firstOrNull()?.isCustom == true
                            )
                        }
                        val chunks = category.emojis.chunked(chunkSize)
                        items(chunks) { rowItems ->
                            EmojiGridRow(
                                items = rowItems,
                                selectedEmoji = selectedEmoji,
                                onSelect = { item ->
                                    TelegramEmojiData.addRecent(item)
                                    onEmojiSelected(item)
                                }
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabButton(
    icon: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF2AABEE).copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(16.dp)
                    .height(2.dp)
                    .background(Color(0xFF2AABEE), RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
private fun CategoryHeader(title: String, isPack: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPack) Color(0xFF2AABEE) else Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        if (isPack) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF2AABEE).copy(alpha = 0.18f)
            ) {
                Text(
                    text = "PACK",
                    color = Color(0xFF2AABEE),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmojiGridRow(
    items: List<EmojiItem>,
    selectedEmoji: String?,
    onSelect: (EmojiItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { item ->
            val isSelected = selectedEmoji == item.emoji
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF2AABEE).copy(alpha = 0.3f) else Color.Transparent)
                    .clickable { onSelect(item) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        // Fill remaining spaces to keep alignment
        repeat(8 - items.size) {
            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}

/**
 * Modal Bottom Sheet variant for Telegram Emoji Picker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramEmojiPickerBottomSheet(
    onDismissRequest: () -> Unit,
    onEmojiSelected: (EmojiItem) -> Unit,
    selectedEmoji: String? = null,
    title: String = "Выбор эмодзи"
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF161E2E),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        TelegramEmojiPicker(
            onEmojiSelected = { emoji ->
                onEmojiSelected(emoji)
                onDismissRequest()
            },
            selectedEmoji = selectedEmoji,
            onDismiss = onDismissRequest,
            title = title
        )
    }
}
