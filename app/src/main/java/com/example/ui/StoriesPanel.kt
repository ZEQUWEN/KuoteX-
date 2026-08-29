package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

data class Story(
    val id: String, 
    val author: String, 
    val avatarUrl: String, 
    val mediaUrl: String, 
    val isViewed: Boolean = false,
    val isLive: Boolean = false
)

val defaultSampleStories = listOf(
    Story("1", "My Story", "https://i.pravatar.cc/150?u=my", "https://picsum.photos/400/800?random=1", false),
    Story("2", "Alice", "https://i.pravatar.cc/150?u=alice", "https://picsum.photos/400/800?random=2", false, isLive = true),
    Story("3", "Bob", "https://i.pravatar.cc/150?u=bob", "https://picsum.photos/400/800?random=3", false),
    Story("4", "Charlie", "https://i.pravatar.cc/150?u=charlie", "https://picsum.photos/400/800?random=4", true),
    Story("5", "Dave", "https://i.pravatar.cc/150?u=dave", "https://picsum.photos/400/800?random=5", true)
)

@Composable
fun LivePulsatingRing(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val gradientColors = listOf(
        Color(0xFFFF1744), // Vivid Red
        Color(0xFFFF4081), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF00E5FF), // Cyan
        Color(0xFFFF1744)  // Loop
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
        },
        contentAlignment = Alignment.Center
    ) {
        // Rotating gradient border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val brush = Brush.sweepGradient(
                        colors = gradientColors
                    )
                    onDrawBehind {
                        drawCircle(
                            brush = brush,
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
                .padding(4.dp)
        ) {
            content()
        }

        // "LIVE" Badge overlay at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 4.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFE50914), Color(0xFFFF2A6D))
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 1.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun JoinStreamConfirmationDialog(
    streamerName: String,
    streamerAvatar: String,
    streamTitle: String = "Прямой эфир",
    viewerCount: Int = 120,
    priceStars: Int = 0,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .clickable(enabled = false) {}, // Prevent dismiss when clicking card
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Streamer Avatar with Live Pulse
                    LivePulsatingRing(modifier = Modifier.size(80.dp)) {
                        AsyncImage(
                            model = streamerAvatar.takeIf { it.isNotBlank() } ?: "https://i.pravatar.cc/150?u=streamer",
                            contentDescription = "Streamer Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = streamerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(Color(0xFFE91E63).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFE91E63), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ПРЯМОЙ ЭФИР • 👁 $viewerCount зрителей",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = streamTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (priceStars > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "⭐️ Платные сообщения: $priceStars звезд", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6A100))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Вы собираетесь подключиться к видеотрансляции. Желаете войти в эфир прямо сейчас?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp)
                        ) {
                            Text("Отмена")
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(23.dp)
                        ) {
                            Icon(Icons.Filled.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Войти", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoriesPanel(
    onStorySwipe: (Boolean) -> Unit, 
    onAvatarClick: (Story) -> Unit = {},
    onLiveClick: (String) -> Unit = {},
    viewModel: AppViewModel? = null
) {
    var selectedStory by remember { mutableStateOf<Story?>(null) }
    var storyToConfirmLive by remember { mutableStateOf<Story?>(null) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    val fallbackStreams = remember { MutableStateFlow(emptyMap<String, LiveStreamSession>()) }
    val activeStreams by (viewModel?.activeStreams ?: fallbackStreams).collectAsStateWithLifecycle()
    
    val fallbackAccount = remember { MutableStateFlow<UserAccount?>(null) }
    val activeAccount by (viewModel?.activeAccount ?: fallbackAccount).collectAsStateWithLifecycle()
    val isSelfStreaming = activeAccount?.let { viewModel?.isUserStreaming(it.id) } ?: false

    val stories = remember(activeStreams, isSelfStreaming, activeAccount) {
        defaultSampleStories.map { s ->
            val isMyStory = s.id == "1"
            val isHostStreaming = if (isMyStory) isSelfStreaming else (activeStreams.containsKey(s.id) || s.isLive)
            s.copy(
                author = if (isMyStory && activeAccount != null) "Моя история" else s.author,
                avatarUrl = if (isMyStory && activeAccount != null && activeAccount?.profilePicUrl?.isNotBlank() == true) activeAccount!!.profilePicUrl else s.avatarUrl,
                isLive = isHostStreaming
            )
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            onStorySwipe(true)
        } else if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            onStorySwipe(false)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            val isMyStory = story.id == "1"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { 
                    if (story.isLive) {
                        if (isMyStory && isSelfStreaming) {
                            // If user is the host streaming, enter directly
                            onLiveClick(story.id)
                        } else {
                            // Show confirmation dialog before joining
                            storyToConfirmLive = story
                        }
                    } else {
                        selectedStory = story 
                    }
                }
            ) {
                if (story.isLive) {
                    LivePulsatingRing(
                        modifier = Modifier.size(68.dp)
                    ) {
                        AsyncImage(
                            model = story.avatarUrl,
                            contentDescription = "Live Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (story.isViewed) Color.Gray.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        AsyncImage(
                            model = story.avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        if (isMyStory) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isMyStory && story.isLive) "🔴 В эфире" else story.author,
                    fontSize = 12.sp,
                    color = if (story.isLive) Color(0xFFFF2A6D) else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (story.isLive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }

    val currentStoryToConfirm = storyToConfirmLive
    if (currentStoryToConfirm != null) {
        val streamSession = activeStreams[currentStoryToConfirm.id]
        val targetStreamId = currentStoryToConfirm.id
        JoinStreamConfirmationDialog(
            streamerName = currentStoryToConfirm.author,
            streamerAvatar = currentStoryToConfirm.avatarUrl,
            streamTitle = streamSession?.title ?: "KuoteX Live Broadcast 🚀",
            viewerCount = streamSession?.viewerCount ?: 142,
            priceStars = streamSession?.commentPriceStars ?: 0,
            onConfirm = {
                storyToConfirmLive = null
                onLiveClick(targetStreamId)
            },
            onDismiss = {
                storyToConfirmLive = null
            }
        )
    }

    if (selectedStory != null) {
        StoryViewerPopup(
            story = selectedStory!!, 
            onAvatarClick = onAvatarClick,
            onJoinLive = { streamId -> 
                val story = stories.find { it.id == streamId } ?: selectedStory!!
                selectedStory = null
                storyToConfirmLive = story
            }
        ) {
            selectedStory = null
        }
    }
}

@Composable
fun StoryViewerPopup(
    story: Story, 
    onAvatarClick: (Story) -> Unit = {}, 
    onJoinLive: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(story) {
        val duration = 15000
        val steps = 100
        val delayTime = (duration / steps).toLong()
        for (i in 1..steps) {
            delay(delayTime)
            progress = i.toFloat() / steps
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = "Story Media",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        onDismiss()
                        onAvatarClick(story)
                    }.padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    if (story.isLive) {
                        LivePulsatingRing(modifier = Modifier.size(44.dp)) {
                            AsyncImage(
                                model = story.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = story.avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = story.author,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (story.isLive) {
                            Text(
                                text = "🔴 В эфире",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                if (story.isLive) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            onDismiss()
                            onJoinLive(story.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Filled.LiveTv, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Смотреть трансляцию", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            // Allow tap to skip if not clicking the live button
            if (!story.isLive) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDismiss() })
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDismiss() })
                }
            }
        }
    }
}
