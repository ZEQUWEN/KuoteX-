package com.example.ui.channel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ui.AppViewModel
import com.example.ui.components.TelegramEmojiPickerBottomSheet
import com.example.utils.QrCodeGenerator

/**
 * ChannelGroupAdminScreen ("Изменить")
 * Implements Telegram's exact Channel Administration & Edit Screen:
 * - Profile header: Avatar, "Выбрать фотографию", Title with interactive Emoji Picker, Description
 * - Section 1: Тип канала, Обсуждение, Сообщения каналу (Stars Monetization), Оформление, Автоперевод
 * - Section 2: Реакции (71), Приветствие, Администраторы, Подписчики, Чёрный список, Статистика, Недавние действия, Партнёрские программы
 * - Section 3: Добавить канал в сообщество, Удалить канал
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelGroupAdminScreen(
    viewModel: AppViewModel,
    chatId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val chat = chats.find { it.id == chatId }

    val customizationsMap by ChannelCustomizationManager.getCustomizationFlow(chatId).collectAsState()
    val customization = customizationsMap[chatId] ?: ChannelCustomization(chatId = chatId)
    val palette = TelegramProfilePalettes.getPalette(customization.profileColorId)

    val members by viewModel.getGroupMembers(chatId).collectAsState(initial = emptyList())
    val recentActionsMap by ChannelCustomizationManager.getRecentActionsFlow(chatId).collectAsState()
    val recentActions = recentActionsMap[chatId] ?: emptyList()

    // Local Edit States
    var channelTitle by remember { mutableStateOf(chat?.title ?: "KuoteX Officiall") }
    var channelDesc by remember { mutableStateOf(customization.description) }
    var selectedStatusEmoji by remember { mutableStateOf(customization.emojiStatus ?: "🪫") }

    // Dialog states
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showChannelTypeDialog by remember { mutableStateOf(false) }
    var showDiscussionDialog by remember { mutableStateOf(false) }
    var showDirectMessagesDialog by remember { mutableStateOf(false) }
    var showBoostRequirementDialog by remember { mutableStateOf(false) }
    var showReactionsDialog by remember { mutableStateOf(false) }
    var showGreetingDialog by remember { mutableStateOf(false) }
    var showAdminsDialog by remember { mutableStateOf(false) }
    var showSubscribersDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showRecentActionsDialog by remember { mutableStateOf(false) }
    var showAffiliateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddAdminDialog by remember { mutableStateOf(false) }

    // --- EMOJI PICKER BOTTOM SHEET ---
    if (showEmojiPicker) {
        TelegramEmojiPickerBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            selectedEmoji = selectedStatusEmoji,
            onEmojiSelected = { emojiItem ->
                selectedStatusEmoji = emojiItem.emoji
            },
            title = "Выберите статус канала"
        )
    }

    // --- 1. CHANNEL TYPE DIALOG ---
    if (showChannelTypeDialog) {
        var isPublicType by remember { mutableStateOf(customization.isPublic) }
        var linkInput by remember { mutableStateOf(customization.inviteLink) }
        var restrictSaving by remember { mutableStateOf(customization.restrictSavingContent) }

        AlertDialog(
            onDismissRequest = { showChannelTypeDialog = false },
            containerColor = Color(0xFF161E2E),
            title = { Text("Тип канала", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPublicType = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isPublicType,
                            onClick = { isPublicType = true },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2AABEE))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Публичный", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Канал можно найти в поиске, доступен всем", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPublicType = false }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isPublicType,
                            onClick = { isPublicType = false },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2AABEE))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Частный", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Вступить можно только по ссылке-приглашению", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        label = { Text("Постоянная ссылка") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2AABEE)
                        )
                    )

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Запретить копирование", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                            Text("Подписчики не смогут копировать и пересылать контент", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Switch(
                            checked = restrictSaving,
                            onCheckedChange = { restrictSaving = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2AABEE))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ChannelCustomizationManager.updateChannelType(chatId, isPublicType, linkInput, restrictSaving)
                        Toast.makeText(context, "Настройки типа канала сохранены!", Toast.LENGTH_SHORT).show()
                        showChannelTypeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChannelTypeDialog = false }) { Text("Отмена", color = Color.Gray) }
            }
        )
    }

    // --- 2. DISCUSSION GROUP DIALOG ---
    if (showDiscussionDialog) {
        var discTitle by remember { mutableStateOf(customization.discussionChatTitle) }
        AlertDialog(
            onDismissRequest = { showDiscussionDialog = false },
            containerColor = Color(0xFF161E2E),
            title = { Text("Группа для обсуждений", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "К каналу можно привязать группу, чтобы подписчики могли обсуждать посты и оставлять комментарии.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = discTitle,
                        onValueChange = { discTitle = it },
                        label = { Text("Название группы обсуждения") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ChannelCustomizationManager.updateDiscussionChat(chatId, discTitle)
                        Toast.makeText(context, "Группа обсуждений привязана: $discTitle", Toast.LENGTH_SHORT).show()
                        showDiscussionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                ) { Text("Привязать") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscussionDialog = false }) { Text("Отмена", color = Color.Gray) }
            }
        )
    }

    // --- 3. DIRECT MESSAGES / STARS MONETIZATION DIALOG ---
    if (showDirectMessagesDialog) {
        var dmEnabled by remember { mutableStateOf(customization.directMessagesEnabled) }
        var starPrice by remember { mutableFloatStateOf(customization.directMessageStarPrice.toFloat()) }

        AlertDialog(
            onDismissRequest = { showDirectMessagesDialog = false },
            containerColor = Color(0xFF161E2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐ Сообщения каналу", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Платные сообщения", fontWeight = FontWeight.Bold, color = Color.White)
                        Switch(
                            checked = dmEnabled,
                            onCheckedChange = { dmEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2AABEE))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Позволяет подписчикам отправлять платные сообщения вашему каналу за Telegram Звёзды (⭐). 85% суммы начисляется владельцу.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (dmEnabled) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Стоимость сообщения: ⭐ ${starPrice.toInt()}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            fontSize = 16.sp
                        )
                        Slider(
                            value = starPrice,
                            onValueChange = { starPrice = it },
                            valueRange = 1f..100f,
                            steps = 99,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD54F), activeTrackColor = Color(0xFFFFD54F))
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0E131D),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = Color(0xFF2AABEE), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("t.me/KuoteXMessenger?direct", color = Color(0xFF2AABEE), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text(
                                    "Копировать",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Direct Link", "https://t.me/KuoteXMessenger?direct"))
                                        Toast.makeText(context, "Прямая ссылка скопирована!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ChannelCustomizationManager.updateDirectMessages(chatId, dmEnabled, starPrice.toInt())
                        Toast.makeText(context, "Настройки сообщений каналу сохранены!", Toast.LENGTH_SHORT).show()
                        showDirectMessagesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                ) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showDirectMessagesDialog = false }) { Text("Закрыть", color = Color.Gray) }
            }
        )
    }

    // --- 4. BOOST REQUIREMENTS / AUTO-TRANSLATE PERK DIALOG ---
    if (showBoostRequirementDialog) {
        AlertDialog(
            onDismissRequest = { showBoostRequirementDialog = false },
            containerColor = Color(0xFF161E2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFFD54F))
                    Spacer(Modifier.width(8.dp))
                    Text("Требуется Уровень 3", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Автоматический перевод сообщений доступен каналам, достигшим Уровня 3 бустов.\n\nТекущий статус вашего канала:\n• Уровень: ${customization.boostLevel}\n• Бустов: ${customization.boostCount} из ${customization.boostsRequiredForNextLevel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBoostRequirementDialog = false
                        navController.navigate("channel_boost/$chatId")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                ) {
                    Text("Получить бусты", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBoostRequirementDialog = false }) { Text("Понятно", color = Color.Gray) }
            }
        )
    }

    // --- 5. REACTIONS MANAGEMENT DIALOG ---
    if (showReactionsDialog) {
        var reactionsEnabled by remember { mutableStateOf(customization.reactionsEnabled) }
        val selectedReactions = remember { mutableStateListOf(*customization.availableReactions.toTypedArray()) }
        var maxPerPost by remember { mutableFloatStateOf(customization.maxReactionsPerPost.toFloat()) }
        var paidStarsEnabled by remember { mutableStateOf(customization.paidStarReactionsEnabled) }

        val allAvailableReactions = remember {
            listOf(
                "❤️", "👍", "👎", "🔥", "🥰", "👏", "😁", "🤔", "🤯", "😱",
                "🤬", "😢", "🎉", "🤩", "🙏", "👌", "🕊️", "🥱", "🥴", "😍", "🐳",
                "❤️‍🔥", "🌚", "🌭", "💯", "🤣", "⚡", "🦄", "💊", "🍌", "🏆", "💔",
                "🤨", "😐", "🍓", "🍾", "💋", "🖕", "😈", "😴", "😭", "🤓", "👻",
                "👨‍💻", "👀", "🎃", "🙈", "😇", "🤝", "🚀", "💎", "⭐", "✨", "👑"
            )
        }

        AlertDialog(
            onDismissRequest = { showReactionsDialog = false },
            containerColor = Color(0xFF161E2E),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Реакции", fontWeight = FontWeight.Bold, color = Color.White)
                    Switch(
                        checked = reactionsEnabled,
                        onCheckedChange = { reactionsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2AABEE))
                    )
                }
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    item {
                        Text(
                            "Выберите реакции, доступные подписчикам под постами канала:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    if (reactionsEnabled) {
                        item {
                            // Reaction Grid
                            val rows = allAvailableReactions.chunked(7)
                            rows.forEach { rowEmojis ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowEmojis.forEach { emoji ->
                                        val isSelected = selectedReactions.contains(emoji)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFF2AABEE).copy(alpha = 0.3f) else Color(0xFF0E131D))
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                    color = if (isSelected) Color(0xFF2AABEE) else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    if (isSelected) {
                                                        if (selectedReactions.size > 1) selectedReactions.remove(emoji)
                                                    } else {
                                                        selectedReactions.add(emoji)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        item {
                            Text(
                                text = "Максимум реакций на пост: ${maxPerPost.toInt()}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Slider(
                                value = maxPerPost,
                                onValueChange = { maxPerPost = it },
                                valueRange = 1f..11f,
                                steps = 9,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF2AABEE), activeTrackColor = Color(0xFF2AABEE))
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Платные реакции (⭐ Звезды)", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                                    Text("Подписчики могут отправлять платные звездные реакции в поддержку канала", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = paidStarsEnabled,
                                    onCheckedChange = { paidStarsEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFFD54F))
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ChannelCustomizationManager.updateReactionsConfig(
                            chatId,
                            reactionsEnabled,
                            selectedReactions.toList(),
                            maxPerPost.toInt(),
                            paidStarsEnabled
                        )
                        Toast.makeText(context, "Настройки реакций обновлены!", Toast.LENGTH_SHORT).show()
                        showReactionsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showReactionsDialog = false }) { Text("Закрыть", color = Color.Gray) }
            }
        )
    }

    // --- 6. AUTO-GREETING DIALOG ---
    if (showGreetingDialog) {
        var greetingEnabled by remember { mutableStateOf(customization.autoGreetingEnabled) }
        var greetingText by remember { mutableStateOf(customization.autoGreetingText) }

        AlertDialog(
            onDismissRequest = { showGreetingDialog = false },
            containerColor = Color(0xFF161E2E),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Приветствие", fontWeight = FontWeight.Bold, color = Color.White)
                    Switch(
                        checked = greetingEnabled,
                        onCheckedChange = { greetingEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2AABEE))
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Приветствие будет автоматически отправляться новым участникам при вступлении в канал.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = greetingText,
                        onValueChange = { greetingText = it },
                        label = { Text("Текст приветственного сообщения") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ChannelCustomizationManager.updateAutoGreeting(chatId, greetingEnabled, greetingText)
                        Toast.makeText(context, "Настройки приветствия сохранены!", Toast.LENGTH_SHORT).show()
                        showGreetingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showGreetingDialog = false }) { Text("Отмена", color = Color.Gray) }
            }
        )
    }

    // --- 7. ADMINS DIALOG ---
    if (showAdminsDialog) {
        var isSignatures by remember { mutableStateOf(customization.isSignaturesEnabled) }
        var showProfiles by remember { mutableStateOf(customization.showAuthorProfiles) }

        AlertDialog(
            onDismissRequest = { showAdminsDialog = false },
            containerColor = Color(0xFF161E2E),
            title = { Text("Администраторы", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    item {
                        Button(
                            onClick = {
                                showAddAdminDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить администратора")
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        // Current Admin Row
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0E131D)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2AABEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("KU", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Владелец канала", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Все права", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2AABEE))
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Подписывать сообщения", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                                Text("Имя автора будет отображаться под постами", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Switch(
                                checked = isSignatures,
                                onCheckedChange = {
                                    isSignatures = it
                                    ChannelCustomizationManager.updateSignatures(chatId, it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2AABEE))
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Показывать профили авторов", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                                Text("При клике на подпись открывается профиль автора", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Switch(
                                checked = showProfiles,
                                onCheckedChange = {
                                    showProfiles = it
                                    ChannelCustomizationManager.updateAuthorProfiles(chatId, it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2AABEE))
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdminsDialog = false }) { Text("Закрыть", color = Color(0xFF2AABEE)) }
            }
        )
    }

    // --- ADD ADMIN SUB-DIALOG ---
    if (showAddAdminDialog) {
        var adminTitleInput by remember { mutableStateOf("Модератор") }
        AlertDialog(
            onDismissRequest = { showAddAdminDialog = false },
            containerColor = Color(0xFF161E2E),
            title = { Text("Назначить администратора", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = adminTitleInput,
                        onValueChange = { adminTitleInput = it },
                        label = { Text("Должность / Титул") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Права: Публикация, Редактирование сообщений, Управление трансляциями", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Администратор добавлен с титулом '$adminTitleInput'!", Toast.LENGTH_SHORT).show()
                        showAddAdminDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                ) { Text("Назначить") }
            },
            dismissButton = {
                TextButton(onClick = { showAddAdminDialog = false }) { Text("Отмена", color = Color.Gray) }
            }
        )
    }

    // --- 8. SUBSCRIBERS DIALOG ---
    if (showSubscribersDialog) {
        var searchQuery by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSubscribersDialog = false },
            containerColor = Color(0xFF161E2E),
            title = { Text("Подписчики (${customization.subscriberCount})", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Поиск контактов для добавления...", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Color(0xFF2AABEE))
                        Spacer(Modifier.width(12.dp))
                        Text("Добавить подписчиков", color = Color(0xFF2AABEE), fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Link", "https://${customization.inviteLink}"))
                                Toast.makeText(context, "Ссылка скопирована!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, tint = Color(0xFF2AABEE))
                        Spacer(Modifier.width(12.dp))
                        Text("Пригласить по ссылке", color = Color(0xFF2AABEE), fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(8.dp))

                    // Subscriber Item
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0E131D)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2AABEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Я", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Вы (Владелец)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("в сети", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2AABEE))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubscribersDialog = false }) { Text("Закрыть", color = Color(0xFF2AABEE)) }
            }
        )
    }

    // --- 9. BLACKLIST DIALOG ---
    if (showBlacklistDialog) {
        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            containerColor = Color(0xFF161E2E),
            title = { Text("Чёрный список (0)", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Block, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("В чёрном списке пока никого нет", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlacklistDialog = false }) { Text("Закрыть", color = Color(0xFF2AABEE)) }
            }
        )
    }

    // --- 10. STATS DIALOG ---
    if (showStatsDialog) {
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            containerColor = Color(0xFF161E2E),
            title = { Text("📊 Статистика канала", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Обзор за последние 7 дней:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2AABEE))
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Просмотры постов:", color = Color.White.copy(alpha = 0.8f))
                        Text("14 820", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Пересылки (репосты):", color = Color.White.copy(alpha = 0.8f))
                        Text("1 340", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Реакции подписчиков:", color = Color.White.copy(alpha = 0.8f))
                        Text("3 910", fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Бусты канала:", color = Color.White.copy(alpha = 0.8f))
                        Text("${customization.boostCount}", fontWeight = FontWeight.Bold, color = Color(0xFF2AABEE))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) { Text("Понятно", color = Color(0xFF2AABEE)) }
            }
        )
    }

    // --- 11. RECENT ACTIONS AUDIT LOG DIALOG ---
    if (showRecentActionsDialog) {
        AlertDialog(
            onDismissRequest = { showRecentActionsDialog = false },
            containerColor = Color(0xFF161E2E),
            title = { Text("📋 Недавние действия", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    if (recentActions.isEmpty()) {
                        item {
                            Text("История действий администраторов пуста", color = Color.Gray)
                        }
                    } else {
                        items(recentActions) { action ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0E131D)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(action.actionTitle, fontWeight = FontWeight.Bold, color = Color(0xFF2AABEE), fontSize = 13.sp)
                                    Text(action.details, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                    Text(action.adminName, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecentActionsDialog = false }) { Text("Закрыть", color = Color(0xFF2AABEE)) }
            }
        )
    }

    // --- 12. AFFILIATE PROGRAMS DIALOG ---
    if (showAffiliateDialog) {
        AlertDialog(
            onDismissRequest = { showAffiliateDialog = false },
            containerColor = Color(0xFF161E2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐ Партнёрские программы", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Зарабатывайте Звёзды (⭐) и реальный доход, рекомендуя каналы, ботов и цифровые товары Telegram.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0E131D),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Комиссия канала: до 20%", fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F), fontSize = 13.sp)
                            Text("Выплаты автоматически в Звёздах и через Fragment.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAffiliateDialog = false }) { Text("Понятно", color = Color(0xFF2AABEE)) }
            }
        )
    }

    // --- 13. DELETE CHANNEL CONFIRMATION DIALOG ---
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF161E2E),
            title = { Text("Удалить канал?", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252)) },
            text = {
                Text(
                    "Вы действительно хотите удалить канал '$channelTitle'? Все посты, медиафайлы и подписчики будут безвозвратно удалены.",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteChat(chatId)
                        Toast.makeText(context, "Канал удален", Toast.LENGTH_SHORT).show()
                        navController.navigate("chat_list") {
                            popUpTo("chat_list") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) { Text("Удалить канал") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Отмена", color = Color.Gray) }
            }
        )
    }

    // --- MAIN SCREEN SCAFFOLD ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Изменить", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            ChannelCustomizationManager.updateChannelInfo(chatId, channelDesc, customization.inviteLink)
                            ChannelCustomizationManager.updateEmojiStatus(chatId, selectedStatusEmoji, isAnimated = true)
                            Toast.makeText(context, "Изменения сохранены!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Сохранить", tint = Color(0xFF2AABEE))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F141C))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F141C), Color(0xFF141A25), Color(0xFF0B0E14))
                    )
                )
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- TOP HEADER CARD: Avatar + Camera + Title + Description ---
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar circle with camera icon
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFF9900), Color(0xFFFF5E36))
                                    )
                                )
                                .clickable {
                                    Toast.makeText(context, "Выбор фотографии из галереи...", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = channelTitle.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2AABEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Выбрать фотографию",
                            color = Color(0xFF2AABEE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Открытие галереи...", Toast.LENGTH_SHORT).show()
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        // Title field with interactive Emoji Status button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = channelTitle,
                                onValueChange = { channelTitle = it },
                                label = { Text("Название канала") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF2AABEE)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            // Status Emoji Button
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0E131D))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { showEmojiPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedStatusEmoji,
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Description multi-line field
                        OutlinedTextField(
                            value = channelDesc,
                            onValueChange = { channelDesc = it },
                            label = { Text("Описание") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF2AABEE)
                            )
                        )
                    }
                }
            }

            // --- SECTION 1: CHANNEL SETTINGS & MONETIZATION (Card 1) ---
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        // 1. Тип канала -> Публичный
                        AdminMenuRow(
                            icon = Icons.Filled.Campaign,
                            title = "Тип канала",
                            value = if (customization.isPublic) "Публичный" else "Частный",
                            onClick = { showChannelTypeDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 2. Обсуждение -> KuoteX chat✨
                        AdminMenuRow(
                            icon = Icons.Filled.ChatBubbleOutline,
                            title = "Обсуждение",
                            value = customization.discussionChatTitle,
                            onClick = { showDiscussionDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 3. Сообщения каналу -> ⭐ 26
                        AdminMenuRow(
                            icon = Icons.Filled.Star,
                            title = "Сообщения каналу",
                            value = "⭐ ${customization.directMessageStarPrice}",
                            iconTint = Color(0xFFFFD54F),
                            onClick = { showDirectMessagesDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 4. Оформление -> 🔒 Уровень 1+  KuoteX ✨
                        AdminMenuRow(
                            icon = Icons.Filled.Palette,
                            title = "Оформление",
                            value = "KuoteX ✨",
                            badgeText = "🔒 Уровень 1+",
                            onClick = { navController.navigate("channel_appearance/$chatId") }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 5. Автоперевод сообщений -> Switch toggle
                        AdminSwitchMenuRow(
                            icon = Icons.Filled.Translate,
                            title = "Автоперевод сообщений",
                            checked = customization.autoTranslateEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && customization.boostLevel < 3) {
                                    showBoostRequirementDialog = true
                                } else {
                                    ChannelCustomizationManager.updateAutoTranslate(chatId, isChecked)
                                }
                            }
                        )
                    }
                }
            }

            // --- SECTION 2: MODERATION & FEATURES (Card 2) ---
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        // 1. Реакции -> 71
                        AdminMenuRow(
                            icon = Icons.Filled.FavoriteBorder,
                            title = "Реакции",
                            value = "${customization.availableReactions.size}",
                            iconTint = Color(0xFFFF5252),
                            onClick = { showReactionsDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 2. Приветствие -> Выкл.
                        AdminMenuRow(
                            icon = Icons.Filled.WavingHand,
                            title = "Приветствие",
                            value = if (customization.autoGreetingEnabled) "Вкл." else "Выкл.",
                            iconTint = Color(0xFFFFB300),
                            onClick = { showGreetingDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 3. Администраторы -> 1
                        AdminMenuRow(
                            icon = Icons.Filled.Shield,
                            title = "Администраторы",
                            value = "1",
                            onClick = { showAdminsDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 4. Подписчики -> 1
                        AdminMenuRow(
                            icon = Icons.Filled.People,
                            title = "Подписчики",
                            value = "${customization.subscriberCount}",
                            onClick = { showSubscribersDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 5. Чёрный список -> 0
                        AdminMenuRow(
                            icon = Icons.Filled.Block,
                            title = "Чёрный список",
                            value = "0",
                            onClick = { showBlacklistDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 6. Статистика
                        AdminMenuRow(
                            icon = Icons.Filled.BarChart,
                            title = "Статистика",
                            value = "",
                            onClick = { showStatsDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 7. Недавние действия
                        AdminMenuRow(
                            icon = Icons.Filled.History,
                            title = "Недавние действия",
                            value = "",
                            onClick = { showRecentActionsDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // 8. Партнёрские программы -> NEW
                        AdminMenuRow(
                            icon = Icons.Filled.WorkspacePremium,
                            title = "Партнёрские программы",
                            value = "",
                            badgeText = "NEW",
                            iconTint = Color(0xFFFFD54F),
                            onClick = { showAffiliateDialog = true }
                        )
                    }
                }
            }

            // --- SECTION 3: COMMUNITY & DANGER ZONE (Card 3) ---
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        // Добавить канал в сообщество
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Добавление канала в сообщество чатов...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = Color(0xFF2AABEE), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Добавить канал в сообщество", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp)
                                Text(
                                    "Вы можете добавить канал в сообщество чатов, связанных между собой.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                        }

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color.White.copy(alpha = 0.06f))

                        // Удалить канал (Danger Button)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeleteConfirmDialog = true }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("Удалить канал", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun AdminMenuRow(
    icon: ImageVector,
    title: String,
    value: String = "",
    badgeText: String? = null,
    iconTint: Color = Color(0xFF2AABEE),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF2AABEE).copy(alpha = 0.18f),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color(0xFF2AABEE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (value.isNotEmpty()) {
            Text(
                text = value,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
            Spacer(Modifier.width(6.dp))
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AdminSwitchMenuRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color = Color(0xFF2AABEE)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2AABEE)
            )
        )
    }
}
