package com.example.ui.channel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class ChannelMenuPage {
    MAIN,
    AUTO_DELETE
}

/**
 * Dropdown Menu anchored to the top-right kebab (⋮) button in Channel Profile.
 * Features Telegram-style seamless animated transitions between Main Menu and Auto-Delete submenu.
 */
@Composable
fun ChannelProfileDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isAdmin: Boolean,
    isChannel: Boolean,
    channelTitle: String,
    currentAutoDeletePeriod: String?,
    onAutoDeleteSelected: (String?) -> Unit,
    onOpenAutoDeleteCustomWheel: () -> Unit,
    onStartLiveStream: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenStoriesArchive: () -> Unit,
    onShare: () -> Unit,
    onSendGift: () -> Unit,
    onViewDiscussion: () -> Unit,
    onCreateShortcut: () -> Unit,
    onLeaveChannel: () -> Unit,
    onDeleteChannel: () -> Unit
) {
    var currentPage by remember { mutableStateOf(ChannelMenuPage.MAIN) }

    // Reset page to MAIN whenever menu is closed
    LaunchedEffect(expanded) {
        if (!expanded) {
            currentPage = ChannelMenuPage.MAIN
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 4.dp),
        modifier = Modifier
            .width(260.dp)
            .background(Color(0xFF222631), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
        containerColor = Color(0xFF222631),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 12.dp
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState == ChannelMenuPage.AUTO_DELETE) {
                    (slideInHorizontally(animationSpec = tween(220)) { it } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(200)) { -it } + fadeOut(animationSpec = tween(180)))
                } else {
                    (slideInHorizontally(animationSpec = tween(220)) { -it } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(200)) { it } + fadeOut(animationSpec = tween(180)))
                }
            },
            label = "ChannelMenuTransition"
        ) { page ->
            when (page) {
                ChannelMenuPage.MAIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // 1. Автоудаление
                        TelegramMenuRow(
                            icon = Icons.Outlined.AccessTime,
                            title = "Автоудаление",
                            hasSubmenu = true,
                            onClick = {
                                currentPage = ChannelMenuPage.AUTO_DELETE
                            }
                        )

                        // 2. Начать трансляцию
                        TelegramMenuRow(
                            icon = Icons.Outlined.LiveTv,
                            title = "Начать трансляцию",
                            onClick = {
                                onDismissRequest()
                                onStartLiveStream()
                            }
                        )

                        // 3. Статистика
                        TelegramMenuRow(
                            icon = Icons.Outlined.TrendingUp,
                            title = "Статистика",
                            onClick = {
                                onDismissRequest()
                                onOpenStats()
                            }
                        )

                        // 4. Архив историй
                        TelegramMenuRow(
                            icon = Icons.Outlined.Archive,
                            title = "Архив историй",
                            onClick = {
                                onDismissRequest()
                                onOpenStoriesArchive()
                            }
                        )

                        // 5. Поделиться
                        TelegramMenuRow(
                            icon = Icons.Outlined.Share,
                            title = "Поделиться",
                            onClick = {
                                onDismissRequest()
                                onShare()
                            }
                        )

                        // 6. Отправить подарок
                        TelegramMenuRow(
                            icon = Icons.Outlined.CardGiftcard,
                            title = "Отправить подарок",
                            onClick = {
                                onDismissRequest()
                                onSendGift()
                            }
                        )

                        // 7. Просмотреть обсуждение
                        TelegramMenuRow(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = "Просмотреть обсуждение",
                            onClick = {
                                onDismissRequest()
                                onViewDiscussion()
                            }
                        )

                        // 8. Создать ярлык
                        TelegramMenuRow(
                            icon = Icons.Outlined.AddBox,
                            title = "Создать ярлык",
                            onClick = {
                                onDismissRequest()
                                onCreateShortcut()
                            }
                        )

                        // 9. Покинуть канал
                        TelegramMenuRow(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            title = if (isChannel) "Покинуть канал" else "Покинуть группу",
                            onClick = {
                                onDismissRequest()
                                onLeaveChannel()
                            }
                        )

                        // 10. Удалить канал (if admin / owner)
                        if (isAdmin) {
                            TelegramMenuRow(
                                icon = Icons.Outlined.Delete,
                                title = if (isChannel) "Удалить канал" else "Удалить группу",
                                textColor = Color(0xFFFF5252),
                                iconTint = Color(0xFFFF5252),
                                onClick = {
                                    onDismissRequest()
                                    onDeleteChannel()
                                }
                            )
                        }
                    }
                }

                ChannelMenuPage.AUTO_DELETE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // Back to Main Menu
                        TelegramMenuRow(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            title = "Назад",
                            onClick = {
                                currentPage = ChannelMenuPage.MAIN
                            }
                        )

                        // 1 день
                        TelegramMenuRow(
                            badgeText = "1D",
                            title = "1 день",
                            onClick = {
                                onDismissRequest()
                                onAutoDeleteSelected("1 день")
                            }
                        )

                        // 7 дней
                        TelegramMenuRow(
                            badgeText = "1W",
                            title = "7 дней",
                            onClick = {
                                onDismissRequest()
                                onAutoDeleteSelected("7 дней")
                            }
                        )

                        // 1 месяц
                        TelegramMenuRow(
                            badgeText = "1M",
                            title = "1 месяц",
                            onClick = {
                                onDismissRequest()
                                onAutoDeleteSelected("1 месяц")
                            }
                        )

                        // Настроить
                        TelegramMenuRow(
                            icon = Icons.Outlined.Tune,
                            title = "Настроить",
                            onClick = {
                                onDismissRequest()
                                onOpenAutoDeleteCustomWheel()
                            }
                        )

                        // Выключить (if auto delete is on)
                        if (!currentAutoDeletePeriod.isNullOrEmpty() && currentAutoDeletePeriod != "Нет") {
                            TelegramMenuRow(
                                icon = Icons.Outlined.Block,
                                title = "Выключить",
                                textColor = Color(0xFFFF5252),
                                iconTint = Color(0xFFFF5252),
                                onClick = {
                                    onDismissRequest()
                                    onAutoDeleteSelected(null)
                                }
                            )
                        }

                        // Info helper card at the bottom
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.Black.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Сообщения, отправленные в этот чат, будут удалены через выбранное Вами время.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelegramMenuRow(
    icon: ImageVector? = null,
    badgeText: String? = null,
    title: String,
    textColor: Color = Color.White,
    iconTint: Color = Color.White.copy(alpha = 0.85f),
    hasSubmenu: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, iconTint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = iconTint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (hasSubmenu) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Telegram Auto-delete Wheel Bottom Sheet ("Автоудаление через...")
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TelegramAutoDeleteWheelBottomSheet(
    currentValue: String?,
    onDismissRequest: () -> Unit,
    onApply: (String?) -> Unit
) {
    val periods = listOf(
        "Нет",
        "1 день",
        "2 дня",
        "3 дня",
        "4 дня",
        "5 дней",
        "6 дней",
        "1 нед.",
        "2 нед.",
        "3 нед.",
        "1 месяц",
        "2 месяца",
        "3 месяца",
        "4 месяца",
        "5 месяцев",
        "6 месяцев",
        "1 год"
    )

    val initialIndex = periods.indexOf(currentValue).takeIf { it >= 0 } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (initialIndex - 1).coerceAtLeast(0))
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    var selectedIndex by remember { mutableIntStateOf(initialIndex) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            // Determine item in center (firstVisibleItemIndex + 1)
            val center = (listState.firstVisibleItemIndex + 1).coerceIn(0, periods.lastIndex)
            selectedIndex = center
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1B1F2A),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Автоудаление через...",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            // Wheel Selector Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Purple Selection Highlight Lines (exact Telegram look from screenshot)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFA855F7), // Vivid purple line
                            shape = RoundedCornerShape(0.dp)
                        )
                )

                LazyColumn(
                    state = listState,
                    flingBehavior = snapBehavior,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 68.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(periods) { index, item ->
                        val isSelected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable {
                                    selectedIndex = index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                fontSize = if (isSelected) 18.sp else 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            val selectedPeriod = periods[selectedIndex]
            val isOff = selectedPeriod == "Нет"

            Button(
                onClick = {
                    val finalVal = if (isOff) null else selectedPeriod
                    onApply(finalVal)
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOff) Color(0xFF262C3A) else Color(0xFF8B5CF6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isOff) "Отключить автоудаление" else "Включить удаление по таймеру",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Fullscreen / Dialog Stories Archive ("Архив историй")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramStoriesArchiveDialog(
    onDismissRequest: () -> Unit,
    onAddNewStory: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Архив историй",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F141C))
                )
            },
            containerColor = Color(0xFF0F141C)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Empty Stories Graphic
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E2330))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CollectionsBookmark,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Историй пока нет...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Загрузите новую историю — она появится здесь.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onDismissRequest()
                            onAddNewStory()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .widthIn(min = 160.dp)
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFDB2777), Color(0xFF8B5CF6))),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Icon(
                                Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Добавить",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Telegram Star Gifts Dialog
 */
@Composable
fun TelegramSendGiftDialog(
    channelTitle: String,
    onDismissRequest: () -> Unit,
    onGiftSent: (String, Int) -> Unit
) {
    val gifts = listOf(
        Triple("🌟", "Звезда", 25),
        Triple("🧸", "Мишка", 50),
        Triple("🌹", "Роза", 100),
        Triple("🎂", "Торт", 250),
        Triple("🚀", "Ракета", 500),
        Triple("💎", "Бриллиант", 1000)
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1B1F2A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🎁 Отправить подарок",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Порадуйте подписчиков и автора канала «$channelTitle» эксклюзивным подарком за Telegram Stars!",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gifts.take(3).forEach { (emoji, name, price) ->
                        GiftItemCard(
                            emoji = emoji,
                            name = name,
                            price = price,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onGiftSent(name, price)
                                onDismissRequest()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gifts.drop(3).forEach { (emoji, name, price) ->
                        GiftItemCard(
                            emoji = emoji,
                            name = name,
                            price = price,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onGiftSent(name, price)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Закрыть", color = Color(0xFF8B5CF6))
            }
        }
    )
}

@Composable
private fun GiftItemCard(
    emoji: String,
    name: String,
    price: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF262C3B),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "⭐ $price",
                color = Color(0xFFFFD54F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
