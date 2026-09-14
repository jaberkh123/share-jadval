package com.jadval.shahr.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.jadval.shahr.data.*
import com.jadval.shahr.ui.theme.*
import java.util.Locale
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import android.widget.Toast

/**
 * DEBUG FLAG — وقتی true باشد همهٔ مراحل از ابتدا باز (unlocked) هستند.
 * ⚠️ قبل از گرفتن خروجی ریلیز به false برگردانده شود.
 */
const val DEBUG_UNLOCK_ALL_LEVELS = false

// Custom Local Composition to check theme modes
val LocalDarkTheme = compositionLocalOf { false }

/** Returns the GameModeColors for the current game mode */
@Composable
fun currentModeColors(gameMode: String): GameModeColors {
    return when (gameMode) {
        "hard" -> HardModeColors
        "biggrid" -> BigGridModeColors
        else -> EasyModeColors
    }
}




@Composable
fun CoinPill(coins: Int, onClick: () -> Unit) {
    val isDark = LocalDarkTheme.current
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        modifier = androidx.compose.ui.Modifier
            .background(
                color = if (isDark) DarkSurface else androidx.compose.ui.graphics.Color(0xFFADE8F4),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
            .border(
                1.5.dp,
                if (isDark) DarkPrimary else androidx.compose.ui.graphics.Color(0xFF00B4D8),
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Add,
            contentDescription = "کسب سکه",
            tint = if (isDark) DarkOnPrimary else androidx.compose.ui.graphics.Color(0xFF0096C7),
            modifier = androidx.compose.ui.Modifier.size(16.dp)
        )
        androidx.compose.material3.Text(
            text = coins.toString().toPersianDigits(),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (isDark) DarkPrimary else androidx.compose.ui.graphics.Color(0xFF0077B6)
            ),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.MonetizationOn,
            contentDescription = "سکه",
            tint = if (isDark) DarkOnPrimary else androidx.compose.ui.graphics.Color(0xFF0096C7),
            modifier = androidx.compose.ui.Modifier.size(18.dp)
        )
    }
}


@Composable
fun AppContent(viewModel: PuzzleViewModel) {
    val isDark = false
    val currentScreen = viewModel.currentScreen

    // Handle physical back button presses safely
    BackHandler(enabled = currentScreen != Screen.HOME) {
        viewModel.handleBackPress()
    }

    // Cap system font scale so text proportions stay predictable (guide §8.3)
    val density = LocalDensity.current
    val fontScale = minOf(density.fontScale, 1.15f)

    CompositionLocalProvider(
        LocalDarkTheme provides isDark,
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides Density(density.density, fontScale = fontScale)
    ) {
        MyApplicationTheme(darkTheme = isDark) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Page routing
                    AnimatedContent(
                        targetState = viewModel.currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith
                                    fadeOut(animationSpec = tween(250))
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.HOME -> HomeScreen(viewModel)
                            Screen.GAME_MODE_SELECT -> GameModeSelectScreen(viewModel)
                            Screen.DIFFICULTY_SELECT -> DifficultySelectScreen(viewModel)
                            Screen.GAME -> GameScreen(viewModel)
                            Screen.SETTINGS -> SettingsScreen(viewModel)
                            Screen.HELP -> HelpScreen(viewModel)
                        }
                    }

                    // Rank Up Dialog overlay
                    viewModel.showRankUpDialog?.let { rankName ->
                        RankUpDialog(
                            rankName = rankName,
                            onDismiss = { viewModel.showRankUpDialog = null }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CustomAdBanner(
    ad: com.jadval.shahr.data.CustomAd?,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 9f / 3f
) {
    ad?.let { customAd ->
        if (customAd.imageUrl.isNotEmpty() && customAd.clickUrl.isNotEmpty()) {
            val context = LocalContext.current
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clickable {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(customAd.clickUrl)
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                coil.compose.AsyncImage(
                    model = customAd.imageUrl,
                    contentDescription = customAd.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: PuzzleViewModel) {
    val progressMap by viewModel.allProgress.collectAsState()
    
    var showInactiveDialog by remember { mutableStateOf(false) }
    if (showInactiveDialog) {
        InactiveGameDialog(onDismiss = { showInactiveDialog = false })
    }
    
    // Calculate total stats
    val solvedCount = progressMap.values.count { it.isCompleted }
    val totalCoins = viewModel.getTotalCoins()
    val totalCoinsEarned = viewModel.getTotalCoinsEarned()
    val totalHints = progressMap.values.sumOf { it.hintsUsed } + viewModel.hintDeductions

    // Check if there is a last played puzzle to continue
    val lastPlayed = progressMap.values.maxByOrNull { it.lastPlayedTime }
    val canContinue = lastPlayed != null && !lastPlayed.isCompleted

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                    SoundManager.playClick()
                    viewModel.navigateTo(Screen.SETTINGS)
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "تنظیمات",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Interactive Gold Coin Card that navigates directly to the Help & Coins screen
            CoinPill(coins = totalCoins) {
                SoundManager.playClick()
                viewModel.navigateTo(Screen.HELP)
            }
        }
        // Brand Identity Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "شهر جدول",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = PersianFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        offset = androidx.compose.ui.geometry.Offset(1f, 3f),
                        blurRadius = 6f
                    ),
                    fontSize = 24.sp
                )
            )
            
            Text(
                text = "پرورش ذهن و سرگرمی با کلمات فارسی",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                ),
                fontFamily = PersianFontFamily
            )
        }

        // Hero Active Game Card or Decorative Crossword
        if (canContinue && lastPlayed != null) {
            val puzzle = PuzzleData.getPuzzleById(lastPlayed.id) ?: PuzzleDataHard.getPuzzleById(lastPlayed.id) ?: PuzzleDataBigGrid.getPuzzleById(lastPlayed.id)
            if (puzzle != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("continue_button"),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .clickable {
                                SoundManager.playClick()
                                viewModel.startPuzzle(puzzle)
                            }
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFCAF0F8).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "ادامه بازی",
                                        tint = Color(0xFFCAF0F8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFCAF0F8).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "چالش نیمه‌کاره",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFCAF0F8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        fontFamily = PersianFontFamily
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "ادامه جدول: ${puzzle.title}",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color(0xFFCAF0F8),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                ),
                                fontFamily = PersianFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = "درجه سختی: ${puzzle.difficulty}",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFCAF0F8).copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                ),
                                fontFamily = PersianFontFamily
                            )
                        }
                    }
                }
            }
        } else {
            // 3x3 Decorative Crossword Board with Letters!
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val grid = listOf(
                    listOf("ج", "■", "ی"),
                    listOf("د", "و", "ا"),
                    listOf("■", "ل", "ن")
                )
                val blockColor = if (androidx.compose.foundation.isSystemInDarkTheme()) DarkBlock else LightBlock
                
                grid.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { letter ->
                            if (letter == "■") {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = blockColor,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        fontFamily = PersianFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Close the weight(1f) scrolling column
        }
        
        Spacer(modifier = Modifier.height(16.dp))



        viewModel.ad11?.let { ad ->
            if (ad.imageUrl.isNotEmpty() && ad.clickUrl.isNotEmpty()) {
                CustomAdBanner(ad = ad, aspectRatio = 9f / 4f)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Menu Actions (Bottom)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Start New Game Card (Primary Action)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clickable {
                        SoundManager.playClick()
                        viewModel.selectedSectionSize = null
                        viewModel.navigateTo(Screen.GAME_MODE_SELECT)
                    }
                    .testTag("start_new_button"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ورود به بازی",
                                textAlign = TextAlign.Center,
                                fontFamily = PersianFontFamily,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                            )
                            Text(
                                text = "شروع چالش‌های جذاب کلمات",
                                textAlign = TextAlign.Center,
                                fontFamily = PersianFontFamily,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Secondary Actions Row
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exit Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(85.dp)
                        .clickable { (context as? android.app.Activity)?.finish() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(26.dp).padding(bottom = 4.dp)
                        )
                        Text(
                            text = "خروج",
                            fontFamily = PersianFontFamily,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }

                // Help Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(85.dp)
                        .clickable {
                            SoundManager.playClick()
                            viewModel.navigateTo(Screen.HELP)
                        }
                        .testTag("help_coins_button"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp).padding(bottom = 4.dp)
                        )
                        Text(
                            text = "سکه و راهنما",
                            textAlign = TextAlign.Center,
                            fontFamily = PersianFontFamily,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Brand Footer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "بازی شهر جدول",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                ),
                fontFamily = PersianFontFamily
            )
            Text(
                text = "نسخه ۱.۰.۰".toPersianDigits(),
                textAlign = TextAlign.Center,
                fontFamily = PersianFontFamily,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }
    }
}


@Composable
fun DifficultyProgressItem(
    title: String,
    solved: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) solved.toFloat() / total.toFloat() else 0f
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$solved از $total",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            )
            Text(
                text = title,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            )
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}


@Composable
fun StatItem(value: String, label: String, icon: ImageVector, tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        )
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        )
    }
}

fun String.toPersianDigits(): String {
    val persianDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return this.map { char ->
        if (char.isDigit()) persianDigits[char.toString().toInt()] else char
    }.joinToString("")
}


@Composable
fun InactiveGameDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "متوجه شدم",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "بخش حل جدول موقتاً غیر فعال است",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = PersianFontFamily
                    ),
                    textAlign = TextAlign.Right
                )
                Icon(
                    imageVector = Icons.Default.Construction,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        text = {
            Text(
                text = "طراحان «شهر جدول» در حال بازنگری، ویرایش و بهبود معماها هستند تا بهترین تجربه کاربری و جذاب‌ترین سوالات را برای شما فراهم کنند.\n\nدر به‌روزرسانی‌های بعدی، جدول‌ها با ساختاری جدید و هیجان‌انگیز فعال خواهند شد! از صبوری شما سپاسگزاریم. 💙",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Right,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}


@Composable
fun RankUpDialog(rankName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700), // Gold
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تبریک! ارتقاء درجه",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PersianFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "سطح شما در بازی افزایش یافت!",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center
                )
                
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = rankName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = PersianFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        textAlign = TextAlign.Center
                    )
                }
                
                Text(
                    text = "از این پس با مهارت و اقتدار بیشتری جداول بزرگتر و چالش‌برانگیزتری را حل خواهید کرد. پیروز باشید! 🎖️",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "بسیار عالی",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ConnectingThread(isUnlocked: Boolean, height: Dp = 32.dp, threadColor: Color? = null) {
    val resolvedColor = threadColor ?: if (isUnlocked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val centerX = size.width / 2f
        drawLine(
            color = resolvedColor,
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun SectionCardItem(
    section: GameSection,
    completedCount: Int,
    totalCount: Int,
    isCompleted: Boolean,
    isUnlocked: Boolean,
    modeColors: GameModeColors = EasyModeColors,
    onClick: () -> Unit
) {
    val cardBg = if (!isUnlocked) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    } else if (isCompleted) {
        Color(0xFFE8F5E9)
    } else {
        modeColors.primaryContainer.copy(alpha = 0.9f)
    }

    val borderColor = if (isCompleted) {
        Color(0xFF81C784)
    } else if (isUnlocked) {
        modeColors.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isUnlocked && !isCompleted) 4.dp else 1.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = {
                SoundManager.playClick()
                onClick()
            })
            .testTag("section_card_${section.size}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isCompleted) Color(0xFFC8E6C9)
                        else if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "کامل شده",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "شروع",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل شده",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val badgeBg = when (section.difficulty) {
                        "آسان" -> Color(0xFFE8F5E9)
                        "متوسط" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFEBEE)
                    }
                    val badgeText = when (section.difficulty) {
                        "آسان" -> Color(0xFF2E7D32)
                        "متوسط" -> Color(0xFFE65100)
                        else -> Color(0xFFC62828)
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = section.difficulty,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeText,
                                fontFamily = PersianFontFamily,
                                fontSize = 9.sp
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Text(
                        text = section.title.toPersianDigits(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontFamily = PersianFontFamily,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "درجه: ${section.rankName}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontFamily = PersianFontFamily,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )

                if (isUnlocked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تکمیل شده: ${completedCount.toString().toPersianDigits()} از ${totalCount.toString().toPersianDigits()}",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                fontFamily = PersianFontFamily,
                                fontSize = 9.sp
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        
                        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                        Text(
                            text = "${(progress * 100).toInt().toString().toPersianDigits()}%",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = PersianFontFamily,
                                fontSize = 9.sp
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "برای باز شدن این بخش، ابتدا بخش قبلی را کامل کنید",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            fontFamily = PersianFontFamily,
                            fontSize = 9.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun GameModeSelectScreen(viewModel: PuzzleViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { SoundManager.playClick(); viewModel.navigateTo(Screen.HOME) },
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = "انتخاب حالت بازی",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = PersianFontFamily, fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.size(44.dp))
        }
        Spacer(modifier = Modifier.height(40.dp))
        Box(modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "یک حالت بازی را انتخاب کنید", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = PersianFontFamily, color = MaterialTheme.colorScheme.onBackground))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "هر حالت مراحل و چالش‌های مخصوص خود را دارد", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PersianFontFamily, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)))
        Spacer(modifier = Modifier.height(48.dp))
        GameModeCard(title = "شروع آسان", subtitle = "مراحل متنوع و جذاب", containerColor = Color(0xFF4CAF50), icon = Icons.Default.SentimentSatisfied, onClick = { SoundManager.playClick(); viewModel.selectGameMode("easy") })
        Spacer(modifier = Modifier.height(20.dp))
        GameModeCard(title = "شروع سخت", subtitle = "چالش‌های دشوار و پیچیده", containerColor = Color(0xFFFF9800), icon = Icons.Default.LocalFireDepartment, onClick = { SoundManager.playClick(); viewModel.selectGameMode("hard") })
        Spacer(modifier = Modifier.height(20.dp))
        GameModeCard(title = "شروع با جدول بزرگ", subtitle = "جدول‌های بزرگ و چالشی", containerColor = Color(0xFFAB47BC), icon = Icons.Default.GridView, onClick = { SoundManager.playClick(); viewModel.selectGameMode("biggrid") })
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "برای شروع یکی از حالت‌ها را لمس کنید", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall.copy(fontFamily = PersianFontFamily, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp))
    }
}

@Composable
private fun GameModeCard(title: String, subtitle: String, containerColor: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = containerColor), shape = RoundedCornerShape(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = title, textAlign = TextAlign.Center, fontFamily = PersianFontFamily, style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp))
                    Text(text = subtitle, textAlign = TextAlign.Center, fontFamily = PersianFontFamily, style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Normal, fontSize = 12.sp))
                }
                Box(modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}


@Composable
fun SectionsScreen(
    viewModel: PuzzleViewModel,
    progressMap: Map<String, PuzzleProgressEntity>
) {
    val puzzleData = viewModel.currentPuzzleData()
    val sections = puzzleData.sectionsList
    val context = androidx.compose.ui.platform.LocalContext.current
    val modeColors = currentModeColors(viewModel.selectedGameMode)

    var activeRank = "سرباز تازه کار"
    for (i in sections.indices) {
        val isUnlocked = i == 0 || sections[i - 1].puzzles.all { progressMap[it.id]?.isCompleted == true }
        if (isUnlocked) {
            activeRank = sections[i].rankName
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    SoundManager.playClick()
                    viewModel.navigateTo(Screen.GAME_MODE_SELECT)
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت"
                )
            }

            Text(
                text = "انتخاب بخش جدول",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = PersianFontFamily,
                    fontSize = 18.sp
                )
            )

            val totalCoins = viewModel.getTotalCoins()
            CoinPill(coins = totalCoins) {
                SoundManager.playClick()
                viewModel.navigateTo(Screen.HELP)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = modeColors.primaryContainer.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = modeColors.starTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "رتبه فعلی شما: $activeRank",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PersianFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = modeColors.accentText,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        viewModel.ad93?.let { ad ->
            if (ad.imageUrl.isNotEmpty() && ad.clickUrl.isNotEmpty()) {
                CustomAdBanner(ad = ad, aspectRatio = 9f / 3f)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.jadval.shahr.R.drawable.bg_size_15_1783962728619),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = 0.35f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                sections.forEachIndexed { index, section ->
                    val completedCount = section.puzzles.count { progressMap[it.id]?.isCompleted == true }
                    val totalCount = section.puzzles.size
                    val isCompleted = completedCount == totalCount && totalCount > 0
                    val isUnlocked = if (index == 0) {
                        true
                    } else {
                        val prevSection = sections[index - 1]
                        val prevSectionCompleted = prevSection.puzzles.all { progressMap[it.id]?.isCompleted == true }
                        val firstPuzzleOfThisSection = section.puzzles.firstOrNull()
                        val firstPuzzleGlobalIndex = if (firstPuzzleOfThisSection != null) puzzleData.puzzlesList.indexOf(firstPuzzleOfThisSection) else -1
                        val prevPuzzleCompleted = if (firstPuzzleGlobalIndex > 0) {
                            progressMap[puzzleData.puzzlesList[firstPuzzleGlobalIndex - 1].id]?.isCompleted == true
                        } else false
                        val anyPuzzleInThisSectionStarted = section.puzzles.any { (progressMap[it.id]?.isCompleted == true) || (progressMap[it.id]?.userInput?.isNotBlank() == true) }
                        
                        prevSectionCompleted || prevPuzzleCompleted || anyPuzzleInThisSectionStarted
                    }

                    SectionCardItem(
                        section = section,
                        completedCount = completedCount,
                        totalCount = totalCount,
                        isCompleted = isCompleted,
                        isUnlocked = isUnlocked,
                        modeColors = modeColors,
                        onClick = {
                            if (isUnlocked) {
                                viewModel.selectedSectionSize = section.size
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "این بخش قفل است. ابتدا بخش‌های قبلی را کامل کنید.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                    if (index < sections.lastIndex) {
                        val nextSection = sections[index + 1]
                        val firstPuzzleOfNextSection = nextSection.puzzles.firstOrNull()
                        val firstPuzzleNextGlobalIndex = if (firstPuzzleOfNextSection != null) puzzleData.puzzlesList.indexOf(firstPuzzleOfNextSection) else -1
                        val nextUnlocked = if (firstPuzzleNextGlobalIndex > 0) {
                            progressMap[puzzleData.puzzlesList[firstPuzzleNextGlobalIndex - 1].id]?.isCompleted == true || isCompleted
                        } else isCompleted
                        ConnectingThread(isUnlocked = nextUnlocked, threadColor = modeColors.primary.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionLevelsScreen(
    viewModel: PuzzleViewModel,
    size: Int,
    progressMap: Map<String, PuzzleProgressEntity>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val puzzleData = viewModel.currentPuzzleData()
    val section = puzzleData.sectionsList.find { it.size == size } ?: puzzleData.sectionsList.first()
    val sectionPuzzles = section.puzzles
    
    val firstUncompletedIndexInSection = sectionPuzzles.indexOfFirst { puzzle ->
        !(progressMap[puzzle.id]?.isCompleted ?: false)
    }.let { if (it == -1) sectionPuzzles.size else it }

    var showInactiveDialog by remember { mutableStateOf(false) }
    
    if (showInactiveDialog) {
        InactiveGameDialog(onDismiss = { showInactiveDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    SoundManager.playClick()
                    viewModel.selectedSectionSize = null
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = section.title.toPersianDigits(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = PersianFontFamily,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "درجه : ${section.rankName}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontFamily = PersianFontFamily,
                        fontSize = 10.sp
                    )
                )
            }

            val totalCoins = viewModel.getTotalCoins()
            CoinPill(coins = totalCoins) {
                SoundManager.playClick()
                viewModel.navigateTo(Screen.HELP)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        viewModel.ad93_2?.let { ad ->
            if (ad.imageUrl.isNotEmpty() && ad.clickUrl.isNotEmpty()) {
                CustomAdBanner(ad = ad, aspectRatio = 9f / 3f)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        if (sectionPuzzles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هیچ مرحله‌ای در این بخش یافت نشد.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        } else {
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val bgDrawable = when(size) {
                    8 -> com.jadval.shahr.R.drawable.bg_size_10_1783962714457
                    10 -> com.jadval.shahr.R.drawable.bg_size_10_1783962714457
                    12, 14, 16, 18 -> com.jadval.shahr.R.drawable.bg_size_15_1783962728619
                    else -> com.jadval.shahr.R.drawable.bg_size_20_1783962743964
                }
                
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = bgDrawable),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alpha = 0.5f
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    sectionPuzzles.forEachIndexed { index, puzzle ->
                        val progress = progressMap[puzzle.id]
                        val isCompleted = progress?.isCompleted ?: false
                        
                        val globalIndex = puzzleData.puzzlesList.indexOf(puzzle)
                        val isLocked = if (DEBUG_UNLOCK_ALL_LEVELS) {
                            // دیباگ: همهٔ مراحل باز هستند
                            false
                        } else if (globalIndex == 0) {
                            false
                        } else {
                            val prevPuzzle = puzzleData.puzzlesList.getOrNull(globalIndex - 1)
                            val prevCompleted = prevPuzzle != null && (progressMap[prevPuzzle.id]?.isCompleted == true)
                            val currentStarted = isCompleted || (progress?.userInput?.isNotBlank() == true)
                            !prevCompleted && !currentStarted
                        }
                        
                        val globalLevelNumber = (globalIndex + 1).toString().toPersianDigits()

                        val mainAngle = index * (kotlin.math.PI / 2.8)
                        val complexModifier = 0.28f * kotlin.math.sin(mainAngle).toFloat() + 
                                0.12f * kotlin.math.cos(index * (kotlin.math.PI / 1.1)).toFloat()
                        val mainBias = (0.5f + complexModifier).coerceIn(0.12f, 0.88f)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (mainBias > 0.02f) {
                                Spacer(modifier = Modifier.weight(mainBias))
                            }
                            
                            LevelCircleItem(
                                levelNumber = globalLevelNumber,
                                isCompleted = isCompleted,
                                isLocked = isLocked,
                                onClick = {
                                    if (!isLocked) {
                                        viewModel.startPuzzle(puzzle)
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "این مرحله قفل است. ابتدا مراحل قبلی را حل کنید.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                            
                            if ((1f - mainBias) > 0.02f) {
                                Spacer(modifier = Modifier.weight(1f - mainBias))
                            }
                        }
                        
                        if (index < sectionPuzzles.lastIndex) {
                            for (d in 1..3) {
                                val t = d / 4f
                                val interpIndex = index + t
                                val interpAngle = interpIndex * (kotlin.math.PI / 2.8)
                                val interpModifier = 0.28f * kotlin.math.sin(interpAngle).toFloat() + 
                                        0.12f * kotlin.math.cos(interpIndex * (kotlin.math.PI / 1.1)).toFloat()
                                val interpBias = (0.5f + interpModifier).coerceIn(0.12f, 0.88f)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (interpBias > 0.02f) {
                                        Spacer(modifier = Modifier.weight(interpBias))
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                shape = CircleShape
                                            )
                                    )
                                    
                                    if ((1f - interpBias) > 0.02f) {
                                        Spacer(modifier = Modifier.weight(1f - interpBias))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultySelectScreen(viewModel: PuzzleViewModel) {
    val progressMap by viewModel.allProgress.collectAsState()
    val selectedSize = viewModel.selectedSectionSize

    if (selectedSize == null) {
        SectionsScreen(viewModel = viewModel, progressMap = progressMap)
    } else {
        SectionLevelsScreen(viewModel = viewModel, size = selectedSize, progressMap = progressMap)
    }
}


@Composable
fun LevelCircleItem(
    levelNumber: String,
    isCompleted: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isCompleted) {
        Color(0xFFADE8F4)
    } else if (isLocked) {
        Color(0xFFFFF0F2) // Very soft, light, whitish red
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isCompleted) {
        Color(0xFF0077B6)
    } else if (isLocked) {
        Color(0xFFE57373) // Soft muted red for lock icon
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    val borderColor = if (isCompleted) {
        Color(0xFF90E0EF)
    } else if (isLocked) {
        Color(0xFFFFCDD2) // Soft pinkish-red border
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .shadow(
                elevation = if (isCompleted) 4.dp else if (isLocked) 0.dp else 2.dp,
                shape = CircleShape
            )
            .background(containerColor, CircleShape)
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(onClick = {
                SoundManager.playClick()
                onClick()
            }),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "حل شده",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = levelNumber.toPersianDigits(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = "حل شده",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f),
                        fontSize = 9.sp
                    )
                )
            } else if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "قفل شده",
                    tint = contentColor,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = levelNumber.toPersianDigits(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                // Active level
                Text(
                    text = levelNumber.toPersianDigits(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor
                    )
                )
                Text(
                    text = "شروع",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}




@Composable
fun GameScreen(viewModel: PuzzleViewModel) {
    val puzzle = viewModel.activePuzzle ?: return
    val activeClue = viewModel.getActiveClue()
    val coroutineScope = rememberCoroutineScope()
    var showHintMenu by remember { mutableStateOf(false) }
    var showAddCoinsDialog by remember { mutableStateOf(false) }

    val rows = puzzle.rows
    val cols = puzzle.cols
    val isLargeGrid = cols > 10 || rows > 10
    var isMinimapMode by remember(puzzle) { mutableStateOf(isLargeGrid) }

    // Auto check dialog
    if (viewModel.showCompletedDialog) {
        GameCompletionDialog(viewModel)
    }

    if (viewModel.showIncorrectCompletionDialog) {
        GameIncorrectCompletionDialog(viewModel)
    }

    if (viewModel.showHintResultDialog) {
        HintResultDialog(viewModel, onShowAddCoins = { showAddCoinsDialog = true })
    }

    if (viewModel.showRatingDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { viewModel.dismissRatingDialog() },
            title = {
                Text(
                    text = "حمایت از ما ⭐",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                    fontFamily = PersianFontFamily
                )
            },
            text = {
                Text(
                    text = "برای حمایت از ما و بهبود برنامه، لطفاً نظر و امتیاز بدهید.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = PersianFontFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.rateApp(context) },
                    modifier = Modifier.testTag("rate_app_confirm_button")
                ) {
                    Text(
                        text = "نظر دادن",
                        fontFamily = PersianFontFamily
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissRatingDialog() },
                    modifier = Modifier.testTag("rate_app_dismiss_button")
                ) {
                    Text(
                        text = "بستن",
                        fontFamily = PersianFontFamily
                    )
                }
            }
        )
    }

    if (showAddCoinsDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { showAddCoinsDialog = false },
            title = {
                Text(
                    text = "دریافت سکه رایگان 🪙",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                    fontFamily = PersianFontFamily
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "شما می‌توانید با مشاهده یک تبلیغ کوتاه، ۱۰۰ سکه رایگان دریافت کنید و برای آشکار کردن خانه‌های جدول از آن استفاده کنید.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (viewModel.adCooldownSecondsLeft > 0L) {
                        Text(
                            text = "زمان باقیمانده تا امکان مشاهده مجدد تبلیغ:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                            textAlign = TextAlign.Center
                        )
                        val mins = viewModel.adCooldownSecondsLeft / 60
                        val secs = viewModel.adCooldownSecondsLeft % 60
                        val timeStr = String.format("%02d:%02d", mins, secs).toPersianDigits()
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 28.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                val isCooldown = viewModel.adCooldownSecondsLeft > 0L
                Button(
                    onClick = {
                        if (!isCooldown) {
                            viewModel.showInterstitialAd(context) {
                                android.widget.Toast.makeText(
                                    context,
                                    "تبلیغ هنوز آماده نیست. لطفاً چند لحظه دیگر دوباره تلاش کنید.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            showAddCoinsDialog = false
                        }
                    },
                    enabled = !isCooldown,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCooldown) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else (if (LocalDarkTheme.current) DarkPrimary else Color(0xFF00B4D8))
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isCooldown) "محدودیت ۵ دقیقه‌ای فعال است" else "مشاهده تبلیغ ـ ۱۰۰ سکه رایگان",
                        fontWeight = FontWeight.Bold,
                        color = if (isCooldown) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color(0xFFCAF0F8)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCoinsDialog = false }) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // First-time gameplay onboarding dialog (Requirement 5)
    if (viewModel.showOnboarding) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOnboarding() },
            title = {
                Text(
                    text = "راهنمای حل جدول بزرگسال 🧩",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "به بازی خوش آمدید! قبل از شروع حل جدول، لطفاً راهنمای کوتاه زیر را بخوانید:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Right
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = "تعیین جهت نوشتن",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = "با انتخاب هر خانه، دو دکمه «افقی» و «عمودی» بالای آن ظاهر می‌شوند. با زدن آن‌ها جهت نوشتن شما مشخص می‌شود (حرکت افقی به چپ و عمودی به پایین تا رسیدن به اولین خانه سیاه پیش می‌رود). این دکمه‌ها با اولین تایپ شما ناپدید می‌شوند.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Right,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text("↕️", fontSize = 22.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissOnboarding() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "بسیار خب، شروع بازی!",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = {
                        viewModel.handleBackPress()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, shape = CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت"
                    )
                }

                // Middle section of TopBar: Single prominent Hint button
                Button(
                    onClick = { showHintMenu = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("hint_menu_button"),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "راهنمایی",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "راهنمایی",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 15.sp
                        )
                    )
                }

                // Coin section on the right with "+" button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(
                                    color = if (LocalDarkTheme.current) DarkSurface else Color(0xFFADE8F4), // Light/dark green background
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.5.dp,
                                    if (LocalDarkTheme.current) DarkPrimary else Color(0xFF00B4D8),
                                    RoundedCornerShape(16.dp)
                                ) // Green border
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "سکه",
                                tint = if (LocalDarkTheme.current) DarkOnPrimary else Color(0xFF0096C7), // Green color
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = viewModel.getTotalCoins().toString().toPersianDigits(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = if (LocalDarkTheme.current) DarkPrimary else Color(0xFF0077B6) // Dark/light green text
                                )
                            )

                            // Vertical divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(16.dp)
                                    .background((if (LocalDarkTheme.current) DarkPrimary else Color(0xFF00B4D8)).copy(alpha = 0.5f))
                            )

                            // "+" Clickable icon to get coins or countdown badge
                            val isCooldown = viewModel.adCooldownSecondsLeft > 0L
                            val context = androidx.compose.ui.platform.LocalContext.current
                            if (isCooldown) {
                                val mins = viewModel.adCooldownSecondsLeft / 60
                                val secs = viewModel.adCooldownSecondsLeft % 60
                                val timeStr = String.format("%02d:%02d", mins, secs).toPersianDigits()
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF90E0EF).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .clickable {
                                            SoundManager.playClick()
                                            android.widget.Toast.makeText(
                                                context,
                                                "امکان دریافت سکه رایگان پس از پایان محدودیت (${timeStr}) وجود دارد.",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF90E0EF),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(if (LocalDarkTheme.current) DarkPrimary else Color(0xFF00B4D8), CircleShape)
                                        .clickable {
                                            SoundManager.playClick()
                                            showAddCoinsDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "دریافت سکه رایگان",
                                        tint = Color(0xFFCAF0F8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Score change floating animation
                        val scoreChangeTriggerId = viewModel.scoreChangeTriggerId
                        val scoreChangeAmount = viewModel.scoreChangeAmount
                        if (scoreChangeTriggerId > 0 && scoreChangeAmount != 0) {
                            var animY by remember { mutableStateOf(0f) }
                            var animAlpha by remember { mutableStateOf(1f) }
                            
                            LaunchedEffect(scoreChangeTriggerId) {
                                animY = 0f
                                animAlpha = 1f
                                animate(
                                    initialValue = 0f,
                                    targetValue = -50f,
                                    animationSpec = tween(1200, easing = LinearOutSlowInEasing)
                                ) { value, _ ->
                                    animY = value
                                }
                            }
                            LaunchedEffect(scoreChangeTriggerId) {
                                animate(
                                    initialValue = 1f,
                                    targetValue = 0f,
                                    animationSpec = tween(1200, easing = LinearOutSlowInEasing)
                                ) { value, _ ->
                                    animAlpha = value
                                }
                            }

                            if (animAlpha > 0f) {
                                val textStr = if (scoreChangeAmount > 0) "+$scoreChangeAmount" else "$scoreChangeAmount"
                                val textColor = if (scoreChangeAmount > 0) Color(0xFF0077B6) else Color(0xFF0096C7)
                                Text(
                                    text = textStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        color = textColor
                                    ),
                                    modifier = Modifier
                                        .graphicsLayer(
                                            translationY = animY,
                                            alpha = animAlpha
                                        )
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
                            // Simple Hint Dropdown Dialog
                            if (showHintMenu) {
                                AlertDialog(
                                    onDismissRequest = { showHintMenu = false },
                                    title = {
                                        Text(
                                            text = "انتخاب نوع راهنمایی",
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = PersianFontFamily
                                        )
                                    },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = {
                                                    viewModel.useHintRevealLetter()
                                                    showHintMenu = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("آشکار کردن حرف خانه فعال (-۳۰ سکه)", fontWeight = FontWeight.Bold)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    viewModel.useHintRevealWord()
                                                    showHintMenu = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("آشکار کردن کل کلمه فعال (-۱۰۰ سکه)", fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.useHintClearWrong()
                                                    showHintMenu = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    "حذف تمام حروف غلط جدول (-۳۰ سکه)",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            val isCooldown = viewModel.adCooldownSecondsLeft > 0L
                                            
                                            Button(
                                                onClick = {
                                                    if (!isCooldown) {
                                                        viewModel.showInterstitialAd(context) {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "تبلیغ هنوز آماده نیست. لطفاً چند لحظه دیگر دوباره تلاش کنید.",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                        showHintMenu = false
                                                    }
                                                },
                                                enabled = !isCooldown,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCooldown) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else (if (LocalDarkTheme.current) DarkPrimary else Color(0xFF00B4D8))
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "مشاهده تبلیغ",
                                                    tint = if (isCooldown) Color(0xFF90E0EF) else Color(0xFFCAF0F8),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isCooldown) {
                                                        val minutes = viewModel.adCooldownSecondsLeft / 60
                                                        val seconds = viewModel.adCooldownSecondsLeft % 60
                                                        "محدودیت تماشا: ${String.format("%02d:%02d", minutes, seconds).toPersianDigits()}"
                                                    } else {
                                                        "دریافت ۱۰۰ سکه رایگان (با تماشای تبلیغ)"
                                                    },
                                                    color = if (isCooldown) Color(0xFF90E0EF) else Color(0xFFCAF0F8),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showHintMenu = false }) {
                                            Text("انصراف")
                                        }
                                    }
                                )
                            }

                    // Native Ad (تبلیغ همسان) placed between the buttons row and the grid
                    NativeAdBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    // 2. Crossword Grid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CrosswordGrid(
                            viewModel = viewModel,
                            isMinimapMode = isMinimapMode,
                            onMinimapModeChange = { isMinimapMode = it },
                            onCellClicked = { r, c ->
                                viewModel.onCellClicked(r, c)
                            }
                        )
                    }

                    // 3. Selected Clue Panel (where the actions toolbar was)
                    // Displayed beautifully, particularly when direction (horizontal/vertical) is selected
                    val activeClue = viewModel.getActiveClue()
                    val activeDirection = viewModel.activeDirection

                    // Animation states for clue transition effect
                    val flashAnim = remember { Animatable(0f) }
                    val contentAlpha = remember { Animatable(1f) }

                    LaunchedEffect(activeClue?.number, activeClue?.direction, activeDirection) {
                        if (activeClue != null) {
                            launch {
                                flashAnim.snapTo(1f)
                                flashAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                )
                            }
                            launch {
                                contentAlpha.snapTo(0.2f)
                                contentAlpha.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                                )
                            }
                        }
                    }

                    val flashValue = flashAnim.value
                    val clueBgColor = if (flashValue > 0f) {
                        lerp(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), // Glow flash
                            flashValue
                        )
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    }

                    val clueBorderColor = if (flashValue > 0f) {
                        lerp(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), // Glowing border highlight
                            flashValue
                        )
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    }

                    val clueBorderWidth = (1f + 1f * flashValue).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = clueBgColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = clueBorderWidth,
                                color = clueBorderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    alpha = contentAlpha.value,
                                    translationY = (8f * (1f - contentAlpha.value)) // Slide in from bottom
                                ),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Direction pill
                                if (activeClue != null) {
                                    val dirLabel = if (viewModel.activeDirection == "across") "افقی ⬅️" else "عمودی ⬇️"
                                    val dirColor = if (viewModel.activeDirection == "across") Color(0xFF0096C7) else Color(0xFF0077B6)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = dirColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = dirLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = dirColor,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = activeClue?.clueText ?: "یک خانه سفید را در جدول انتخاب کنید و جهت آن (افقی/عمودی) را مشخص نمایید.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 20.sp, // Increased by 2sp
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 25.sp,
                                    textAlign = TextAlign.Right,
                                    color = if (activeClue != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 4. Custom Persian On-Screen Keyboard
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp)
                    ) {
                        PersianOnScreenKeyboard(
                            activeClue = activeClue,
                            onCharTyped = { viewModel.onKeyPressed(it) },
                            onBackspace = { viewModel.onBackspacePressed() }
                        )
                    }
        }
    }
}


@Composable
fun ClueListItem(
    clue: Clue,
    viewModel: PuzzleViewModel,
    pagerState: androidx.compose.foundation.pager.PagerState,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val activeClue = viewModel.getActiveClue()
    val isCurrentlyActive = activeClue?.number == clue.number && viewModel.activeDirection == clue.direction

    // Determined color theme based on direction
    val isAcross = clue.direction == "across"
    
    val activeBg = if (isAcross) {
        if (LocalDarkTheme.current) SelectedDark else SelectedLight
    } else {
        if (LocalDarkTheme.current) HighlightedDark else HighlightedLight
    }
    
    val activeBorderColor = if (isAcross) {
        if (LocalDarkTheme.current) DarkPrimary else Color(0xFF0077B6)
    } else {
        if (LocalDarkTheme.current) DarkOnPrimary else Color(0xFF0096C7)
    }
    
    val activeIndicatorColor = if (isAcross) {
        if (LocalDarkTheme.current) DarkPrimary else Color(0xFF0077B6)
    } else {
        if (LocalDarkTheme.current) DarkOnPrimary else Color(0xFF0096C7)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.onCellClicked(clue.startRow, clue.startCol)
                viewModel.selectDirection(clue.direction)
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyActive) {
                activeBg
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (isCurrentlyActive) {
            androidx.compose.foundation.BorderStroke(1.5.dp, activeBorderColor)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badged Clue Number
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (isCurrentlyActive) activeIndicatorColor else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = clue.number.toString().toPersianDigits(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCurrentlyActive) Color(0xFFCAF0F8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            // Clue Text & Category
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = clue.clueText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrentlyActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                        textAlign = TextAlign.Right,
                        fontSize = 16.5.sp // Increased by 2sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (clue.category.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "دسته‌بندی: ${clue.category}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
            
            // Clue length badge
            Box(
                modifier = Modifier
                    .background(
                        color = if (isCurrentlyActive) {
                            activeIndicatorColor.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${clue.length} حرفی",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCurrentlyActive) activeIndicatorColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}


@Composable
fun CrosswordCell(
    row: Int,
    col: Int,
    cellSize: Dp,
    isBlock: Boolean,
    userChar: Char,
    cellNumber: Int,
    isSelected: Boolean,
    isHighlighted: Boolean,
    showErrors: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    isMinimapMode: Boolean,
    isPartOfCorrectCompletedClue: Boolean,
    sharedInteractionSource: MutableInteractionSource,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current

    // Determine colors based on correctness & selection
    val cellBg = when {
        isBlock -> Color.Transparent // Background is handled via drawing
        isSelected -> if (isDark) SelectedDark else SelectedLight // Soft Green Selection
        userChar != ' ' && !isCorrect -> if (isDark) IncorrectDark else IncorrectLight // Very light red
        userChar != ' ' && isCorrect -> if (isDark) CorrectDark else CorrectLight // Light green
        isHighlighted -> if (isDark) HighlightedDark else HighlightedLight // Soft Blue Clue Highlight
        else -> if (isDark) EmptyCellDark else EmptyCellLight // Cream empty cell
    }

    val textColor = when {
        isBlock -> Color.Transparent
        userChar != ' ' && !isCorrect -> if (isDark) IncorrectTextDark else IncorrectTextLight // Red text
        userChar != ' ' && isCorrect -> if (isDark) CorrectTextDark else CorrectTextLight // Green text
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .background(cellBg)
            .then(
                if (isBlock) {
                    val blockBgColor = if (isDark) DarkBlock else LightBlock // Blue background
                    val hatchColor = if (isDark) DarkBlock else LightBlock // Darker blue hatches
                    Modifier.drawBehind {
                        drawRect(color = blockBgColor)
                        val strokeWidth = 2.dp.toPx()
                        val spacing = 6.dp.toPx()
                        val maxD = size.width + size.height
                        var d = 0f
                        while (d < maxD) {
                            drawLine(
                                color = hatchColor,
                                start = androidx.compose.ui.geometry.Offset(0f, d),
                                end = androidx.compose.ui.geometry.Offset(d, 0f),
                                strokeWidth = strokeWidth
                            )
                            d += spacing
                        }
                    }
                } else {
                    Modifier
                }
            )
            .border(
                width = when {
                    isSelected -> 2.dp
                    isHighlighted -> 1.5.dp
                    else -> 0.5.dp
                },
                color = when {
                    isSelected -> if (isDark) DarkPrimary else Color(0xFF0077B6) // Soft Green selected border
                    isHighlighted -> if (isDark) HighlightedDark else Color(0xFF0096C7) // Soft Blue highlighted border
                    else -> if (isDark)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                }
            )
            .clickable(
                enabled = !isBlock,
                onClick = onClick,
                indication = null,
                interactionSource = sharedInteractionSource
            )
            .testTag("cell_${row}_${col}"),
        contentAlignment = Alignment.Center
    ) {
        // User entered character
        if (!isBlock && userChar != ' ') {
            val charFontSize = (cellSize.value * 0.58f).coerceAtMost(25f).sp
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userChar.toString(),
                    fontSize = charFontSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        textAlign = TextAlign.Center,
                        lineHeight = charFontSize
                    )
                )
            }
        }
    }
}


@Composable
fun CrosswordGrid(
    viewModel: PuzzleViewModel,
    isMinimapMode: Boolean,
    onMinimapModeChange: (Boolean) -> Unit,
    onCellClicked: (Int, Int) -> Unit
) {
    val puzzle = viewModel.activePuzzle ?: return
    val cellNumbers = remember(puzzle) { puzzle.getCellNumbers() }

    val rows = puzzle.rows
    val cols = puzzle.cols
    val isLargeGrid = cols > 10 || rows > 10
    val isVeryLargeGrid = cols >= 14 || rows >= 14

    var zoomPercent by remember(puzzle.id) { mutableIntStateOf(100) }
    var hasZoomedInThisPuzzle by remember(puzzle.id) { mutableStateOf(false) }
    var isZoomFlashing by remember(puzzle.id) { mutableStateOf(false) }

    LaunchedEffect(puzzle.id, hasZoomedInThisPuzzle) {
        if (!hasZoomedInThisPuzzle && (cols >= 8 || rows >= 8)) {
            delay(10000L)
            if (zoomPercent == 100 && !hasZoomedInThisPuzzle) {
                isZoomFlashing = true
            }
        }
    }

    val zoomInfiniteTransition = rememberInfiniteTransition(label = "zoomFlash")
    val flashAlpha by zoomInfiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashAlpha"
    )

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Direction tracking for scroll edge highlights (updated inside detectDragGestures to avoid rebuilds)
    var scrollDirectionX by remember { mutableStateOf(0) } // -1 for left, 1 for right, 0 for idle
    var scrollDirectionY by remember { mutableStateOf(0) } // -1 for up, 1 for down, 0 for idle

    val isScrollingX = horizontalScrollState.isScrollInProgress
    val isScrollingY = verticalScrollState.isScrollInProgress

    // Read edges in derivedStateOf to prevent parent recomposition on every pixel scroll
    val isAtLeftEdge by remember { derivedStateOf { horizontalScrollState.value == 0 } }
    val isAtRightEdge by remember { derivedStateOf { horizontalScrollState.value == horizontalScrollState.maxValue } }
    val isAtTopEdge by remember { derivedStateOf { verticalScrollState.value == 0 } }
    val isAtBottomEdge by remember { derivedStateOf { verticalScrollState.value == verticalScrollState.maxValue } }

    // Smoothly animate the alpha for each of the 4 glows
    val leftGlowAlpha by animateFloatAsState(
        targetValue = if (isScrollingX && (scrollDirectionX == -1 || isAtLeftEdge)) 0.8f else 0f,
        animationSpec = tween(300),
        label = "leftGlow"
    )

    val rightGlowAlpha by animateFloatAsState(
        targetValue = if (isScrollingX && (scrollDirectionX == 1 || isAtRightEdge)) 0.8f else 0f,
        animationSpec = tween(300),
        label = "rightGlow"
    )

    val topGlowAlpha by animateFloatAsState(
        targetValue = if (isScrollingY && (scrollDirectionY == -1 || isAtTopEdge)) 0.8f else 0f,
        animationSpec = tween(300),
        label = "topGlow"
    )

    val bottomGlowAlpha by animateFloatAsState(
        targetValue = if (isScrollingY && (scrollDirectionY == 1 || isAtBottomEdge)) 0.8f else 0f,
        animationSpec = tween(300),
        label = "bottomGlow"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom controls bar above grid (top-left aligned) for grids size 8x8 and above
        if (cols >= 8 || rows >= 8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    val boxBgColor = if (isZoomFlashing) {
                        Color(0xFFFFEBEE).copy(alpha = 0.85f)
                    } else if (LocalDarkTheme.current) {
                        DarkSurface.copy(alpha = 0.95f)
                    } else {
                        Color.White.copy(alpha = 0.95f)
                    }

                    val borderColor = if (isZoomFlashing) {
                        Color(0xFFE53935).copy(alpha = 0.3f + 0.7f * flashAlpha)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    }

                    Box(
                        modifier = Modifier
                            .shadow(if (isZoomFlashing) 6.dp else 3.dp, RoundedCornerShape(16.dp))
                            .background(
                                color = boxBgColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = if (isZoomFlashing) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    hasZoomedInThisPuzzle = true
                                    isZoomFlashing = false
                                    if (zoomPercent > 100) {
                                        zoomPercent = (zoomPercent - 20).coerceAtLeast(100)
                                    }
                                },
                                enabled = zoomPercent > 100,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text(
                                    text = "-",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (zoomPercent > 100) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }

                            Text(
                                text = "${zoomPercent.toString().toPersianDigits()}٪",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isZoomFlashing) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    hasZoomedInThisPuzzle = true
                                    isZoomFlashing = false
                                    if (zoomPercent < 200) {
                                        zoomPercent = (zoomPercent + 20).coerceAtMost(200)
                                    }
                                },
                                enabled = zoomPercent < 200,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isZoomFlashing) Color(0xFFC62828) else if (zoomPercent < 200) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            val zoomFactor = zoomPercent / 100f
            val maxCellWidth = maxWidth / cols
            val maxCellHeight = maxHeight / rows
            val baseCellSize = minOf(maxCellWidth, maxCellHeight)
            val cellSize = baseCellSize * zoomFactor
            val gridWidth = cellSize * cols
            val gridHeight = cellSize * rows

            val isScrollableActive = gridWidth > maxWidth || gridHeight > maxHeight || zoomPercent > 100

            val activeRow = viewModel.activeRow
            val activeCol = viewModel.activeCol

            // Requirement: Prevent jump/lag when zooming. Calculate bounds and animate together.
            val maxScrollX = with(density) { (gridWidth - maxWidth).toPx().coerceAtLeast(0f) }
            val maxScrollY = with(density) { (gridHeight - maxHeight).toPx().coerceAtLeast(0f) }

            // Auto-scroll to selected cell if grid is large and we are NOT in minimap mode
            if (isLargeGrid) {
                LaunchedEffect(activeRow, activeCol, isMinimapMode) {
                    if (activeRow != -1 && activeCol != -1 && !isMinimapMode) {
                        kotlinx.coroutines.delay(50)
                        val cellPx = with(density) { cellSize.toPx() }
                        val viewportWidthPx = with(density) { maxWidth.toPx() }
                        val viewportHeightPx = with(density) { maxHeight.toPx() }

                        val cellLeftPx = cellPx * activeCol
                        val cellTopPx = cellPx * activeRow

                        val targetScrollX = (cellLeftPx - (viewportWidthPx / 2f) + (cellPx / 2f))
                            .coerceIn(0f, maxScrollX)
                            .toInt()
                        val targetScrollY = (cellTopPx - (viewportHeightPx / 2f) + (cellPx / 2f))
                            .coerceIn(0f, maxScrollY)
                            .toInt()

                        // Animate horizontally and vertically simultaneously for beautiful diagonal motion (snappy 350ms)
                        launch {
                            horizontalScrollState.animateScrollTo(targetScrollX, animationSpec = tween(350, easing = FastOutSlowInEasing))
                        }
                        launch {
                            verticalScrollState.animateScrollTo(targetScrollY, animationSpec = tween(350, easing = FastOutSlowInEasing))
                        }
                    }
                }
            }

            // Tracking cell clicks to manage overlay fade timers
            var lastClickTime by remember { mutableStateOf(0L) }
            LaunchedEffect(activeRow, activeCol) {
                if (activeRow != -1 && activeCol != -1) {
                    lastClickTime = System.currentTimeMillis()
                }
            }

            // Cell clicked flash highlight overlay animation state
            val clickHighlightAlpha = remember { Animatable(0f) }
            LaunchedEffect(lastClickTime) {
                if (activeRow != -1 && activeCol != -1 && !isMinimapMode) {
                    clickHighlightAlpha.snapTo(0.85f)
                    clickHighlightAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(550, easing = FastOutSlowInEasing)
                    )
                }
            }

            // 2. Direction picker state and fade control (shown in both minimap and zoom modes)
            var showDirectionPickerState by remember { mutableStateOf(false) }
            val directionPickerAlpha = remember { Animatable(0f) }

            LaunchedEffect(lastClickTime, viewModel.showDirectionPicker) {
                if (activeRow != -1 && activeCol != -1 && viewModel.showDirectionPicker) {
                    showDirectionPickerState = true
                    directionPickerAlpha.snapTo(0f)
                    directionPickerAlpha.animateTo(1f, animationSpec = tween(300))
                    delay(3000)
                    directionPickerAlpha.animateTo(0f, animationSpec = tween(500))
                    showDirectionPickerState = false
                } else {
                    showDirectionPickerState = false
                    directionPickerAlpha.snapTo(0f)
                }
            }

            // 3. Floating Zoom (بزرگنمایی) button
            var showZoomButton by remember { mutableStateOf(false) }
            val zoomButtonAlpha = remember { Animatable(0f) }

            LaunchedEffect(lastClickTime) {
                if (isMinimapMode && activeRow != -1 && activeCol != -1) {
                    showZoomButton = true
                    zoomButtonAlpha.snapTo(0f)
                    zoomButtonAlpha.animateTo(1f, animationSpec = tween(300))
                    delay(3500)
                    zoomButtonAlpha.animateTo(0f, animationSpec = tween(500))
                    showZoomButton = false
                } else {
                    showZoomButton = false
                    zoomButtonAlpha.snapTo(0f)
                }
            }

            // 1. Clipped Cell Grid Container (keeps grid cells inside elegant rounded border)
            val containerModifier = if (isScrollableActive) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .size(width = gridWidth, height = gridHeight)
                    .align(Alignment.Center)
            }

            // Force LTR for the scroll container: all horizontal math in this composable
            // (drag deltas, auto-scroll target, overlay offsets, edge glows) assumes
            // "col 0 = leftmost". In the app-wide RTL context, horizontal scroll values are
            // mirrored (value 0 = right edge), which inverted left/right drag in zoomed mode.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = containerModifier
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Scrollable view port
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isScrollableActive) {
                                Modifier
                                    // Custom pointerInput for buttery-smooth diagonal drag scrolling with zero allocations
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = {
                                                // User manually dragged! Hide overlays immediately.
                                                showDirectionPickerState = false
                                                showZoomButton = false
                                                coroutineScope.launch { directionPickerAlpha.snapTo(0f) }
                                                coroutineScope.launch { zoomButtonAlpha.snapTo(0f) }
                                            },
                                            onDragEnd = {
                                                scrollDirectionX = 0
                                                scrollDirectionY = 0
                                            },
                                            onDragCancel = {
                                                scrollDirectionX = 0
                                                scrollDirectionY = 0
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            scrollDirectionX = if (dragAmount.x > 0.5f) -1 else if (dragAmount.x < -0.5f) 1 else scrollDirectionX
                                            scrollDirectionY = if (dragAmount.y > 0.5f) -1 else if (dragAmount.y < -0.5f) 1 else scrollDirectionY
                                            horizontalScrollState.dispatchRawDelta(-dragAmount.x)
                                            verticalScrollState.dispatchRawDelta(-dragAmount.y)
                                        }
                                    }
                                    .horizontalScroll(horizontalScrollState, enabled = false) // Handled by gesture detector
                                    .verticalScroll(verticalScrollState, enabled = false)     // Handled by gesture detector
                            } else {
                                Modifier
                            }
                        )
                ) {
                    // Precompute highlighted cell indices to avoid expensive search in inner loop (reduces lookup from O(N*C) to O(1))
                    val activeClue = remember(activeRow, activeCol, viewModel.activeDirection, puzzle) {
                        viewModel.getActiveClue()
                    }
                    val highlightedCells = remember(activeClue, viewModel.activeDirection, cols, puzzle) {
                        if (activeClue == null) {
                            emptySet<Int>()
                        } else {
                            val set = mutableSetOf<Int>()
                            for (i in 0 until activeClue.length) {
                                val r = if (viewModel.activeDirection == "across") activeClue.startRow else activeClue.startRow + i
                                val c = puzzle.clueCol(activeClue, i)
                                set.add(r * cols + c)
                            }
                            set
                        }
                    }

                    // Shared interaction source to prevent creating 625 separate objects
                    val sharedInteractionSource = remember { MutableInteractionSource() }

                    // Outer column that has size of entire grid (gridWidth, gridHeight)
                    // Force LTR layout direction for the grid so columns render left-to-right
                    // (col 0 = leftmost, col N = rightmost). Without this, the app-wide RTL
                    // direction reverses each Row, flipping horizontal words 180°.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        modifier = Modifier
                            .size(width = gridWidth, height = gridHeight)
                            .align(Alignment.Center)
                    ) {
                        for (row in 0 until rows) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until cols) {
                                    val index = row * cols + col
                                    val isBlock = puzzle.gridSolutions[index] == "■"
                                    val userChar = viewModel.userGridInputs.getOrNull(index) ?: ' '
                                    val solutionChar = puzzle.gridSolutions[index]
                                    val isSelected = activeRow == row && activeCol == col
                                    val isHighlighted = index in highlightedCells
                                    val cellNumber = cellNumbers.getOrNull(index) ?: 0

                                    val showErrors = viewModel.isInstantCheckEnabled || viewModel.manualCheckMode
                                    val isCorrect = userChar != ' ' && viewModel.normalizeCharForComparison(userChar) == viewModel.normalizeCharForComparison(solutionChar[0])
                                    val isWrong = userChar != ' ' && viewModel.normalizeCharForComparison(userChar) != viewModel.normalizeCharForComparison(solutionChar[0])

                                    // Check if the cell is part of a fully completed correct clue
                                    var isPartOfCorrectCompletedClue = false
                                    if (isCorrect) {
                                        val allClues = puzzle.acrossClues + puzzle.downClues
                                        for (clue in allClues) {
                                            val covers = if (clue.direction == "across") {
                                                row == clue.startRow && col <= clue.startCol && col > clue.startCol - clue.length
                                            } else {
                                                col == clue.startCol && row >= clue.startRow && row < clue.startRow + clue.length
                                            }
                                            
                                            if (covers) {
                                                var isClueCompleteAndCorrect = true
                                                for (i in 0 until clue.length) {
                                                    val r = if (clue.direction == "across") clue.startRow else clue.startRow + i
                                                    val c = puzzle.clueCol(clue, i)
                                                    val idx = r * cols + c
                                                    val uChar = viewModel.userGridInputs.getOrNull(idx) ?: ' '
                                                    val sChar = puzzle.gridSolutions.getOrNull(idx) ?: "■"
                                                    if (uChar == ' ' || viewModel.normalizeCharForComparison(uChar) != viewModel.normalizeCharForComparison(sChar[0])) {
                                                        isClueCompleteAndCorrect = false
                                                        break
                                                    }
                                                }
                                                if (isClueCompleteAndCorrect) {
                                                    isPartOfCorrectCompletedClue = true
                                                    break
                                                }
                                            }
                                        }
                                    }

                                    key(index) {
                                        CrosswordCell(
                                            row = row,
                                            col = col,
                                            cellSize = cellSize,
                                            isBlock = isBlock,
                                            userChar = userChar,
                                            cellNumber = cellNumber,
                                            isSelected = isSelected,
                                            isHighlighted = isHighlighted,
                                            showErrors = showErrors,
                                            isCorrect = isCorrect,
                                            isWrong = isWrong,
                                            isMinimapMode = isMinimapMode,
                                            isPartOfCorrectCompletedClue = isPartOfCorrectCompletedClue,
                                            sharedInteractionSource = sharedInteractionSource,
                                            onClick = {
                                                onCellClicked(row, col)
                                                lastClickTime = System.currentTimeMillis()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    } // end CompositionLocalProvider (LTR grid)

                    // Active cell click highlight flash overlay moved outside the loops to avoid high-frequency recomposition of the full grid
                    if (activeRow != -1 && activeCol != -1 && !isMinimapMode && clickHighlightAlpha.value > 0.01f) {
                        val flashX = cellSize * activeCol
                        val flashY = cellSize * activeRow
                        Box(
                            modifier = Modifier
                                .offset(x = flashX, y = flashY)
                                .size(cellSize)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF0096C7).copy(alpha = clickHighlightAlpha.value),
                                            Color(0xFF0096C7).copy(alpha = clickHighlightAlpha.value * 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    val showDirectionPicker = (showDirectionPickerState || directionPickerAlpha.value > 0.05f) && activeRow != -1 && activeCol != -1
                    if (showDirectionPicker) {
                        val r = activeRow
                        val c = activeCol
                        
                        // Design a glassy, larger direction picker
                        val pickerWidth = 120.dp
                        val pickerHeight = 40.dp
                        val padding = 4.dp
                        val isAcross = viewModel.activeDirection == "across"
                        
                        var rawX: androidx.compose.ui.unit.Dp
                        var rawY: androidx.compose.ui.unit.Dp
                        
                        if (isAcross) {
                            // Active line is horizontal. Place picker above or below the cell.
                            val cellCenter = (cellSize * c) + (cellSize / 2f)
                            rawX = cellCenter - (pickerWidth / 2f)
                            
                            // Check if there is enough space above, otherwise put below
                            val spaceAbove = (cellSize * r)
                            if (spaceAbove >= pickerHeight + 12.dp) {
                                rawY = (cellSize * r) - pickerHeight - 8.dp
                            } else {
                                rawY = (cellSize * (r + 1)) + 8.dp
                            }
                        } else {
                            // Active line is vertical. Place picker to the left or right of the cell.
                            val cellCenter = (cellSize * r) + (cellSize / 2f)
                            rawY = cellCenter - (pickerHeight / 2f)
                            
                            // Check if there is enough space to the left, otherwise put right
                            val spaceLeft = (cellSize * c)
                            val spaceRight = gridWidth - (cellSize * (c + 1))
                            
                            if (spaceLeft >= pickerWidth + 12.dp) {
                                rawX = spaceLeft - pickerWidth - 8.dp
                            } else if (spaceRight >= pickerWidth + 12.dp) {
                                rawX = (cellSize * (c + 1)) + 8.dp
                            } else {
                                // Not enough space on either side (very narrow grid), just offset it slightly and let it overlap partially
                                rawX = spaceLeft - (pickerWidth / 2f) + (cellSize / 2f)
                            }
                        }
                        
                        val clampedXOffset = rawX.coerceIn(padding, gridWidth - pickerWidth - padding)
                        val clampedYOffset = rawY.coerceIn(padding, gridHeight - pickerHeight - padding)

                        Box(
                            modifier = Modifier
                                .offset(x = clampedXOffset, y = clampedYOffset)
                                .graphicsLayer(alpha = directionPickerAlpha.value)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    clip = false
                                )
                                .background(
                                    color = if (LocalDarkTheme.current) DarkSurface.copy(alpha=0.95f) else Color(0xF2ADE8F4), // Glassy Slate look
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (LocalDarkTheme.current) Color(0x33CAF0F8) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .width(pickerWidth)
                                .height(pickerHeight)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Vertical button ("عمودی")
                                val isVerticalActive = viewModel.activeDirection == "down"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isVerticalActive) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            } else Color.Transparent
                                        )
                                        .clickable(enabled = directionPickerAlpha.value > 0.5f) { viewModel.selectDirection("down") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "عمودی",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isVerticalActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                // Separation Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight(0.5f)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                                )

                                // Horizontal button ("افقی")
                                val isHorizontalActive = viewModel.activeDirection == "across"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isHorizontalActive) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            } else Color.Transparent
                                        )
                                        .clickable(enabled = directionPickerAlpha.value > 0.5f) { viewModel.selectDirection("across") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "افقی",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isHorizontalActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Glowing Edge Indicator overlays for scrolling feedback
                if (isScrollableActive) {
                    // 3.1 Left edge glow overlay
                    if (leftGlowAlpha > 0f) {
                        val leftColor = if (horizontalScrollState.value == 0) Color(0xFF00B4D8) else Color(0xFF00B4D8)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(24.dp)
                                .align(Alignment.CenterStart)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(leftColor.copy(alpha = leftGlowAlpha * 0.45f), Color.Transparent)
                                    )
                                )
                        )
                    }

                    // 3.2 Right edge glow overlay
                    if (rightGlowAlpha > 0f) {
                        val rightColor = if (horizontalScrollState.value == horizontalScrollState.maxValue) Color(0xFF00B4D8) else Color(0xFF00B4D8)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(24.dp)
                                .align(Alignment.CenterEnd)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, rightColor.copy(alpha = rightGlowAlpha * 0.45f))
                                    )
                                )
                        )
                    }

                    // 3.3 Top edge glow overlay
                    if (topGlowAlpha > 0f) {
                        val topColor = if (verticalScrollState.value == 0) Color(0xFF00B4D8) else Color(0xFF00B4D8)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(topColor.copy(alpha = topGlowAlpha * 0.45f), Color.Transparent)
                                    )
                                )
                        )
                    }

                    // 3.4 Bottom edge glow overlay
                    if (bottomGlowAlpha > 0f) {
                        val bottomColor = if (verticalScrollState.value == verticalScrollState.maxValue) Color(0xFF00B4D8) else Color(0xFF00B4D8)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, bottomColor.copy(alpha = bottomGlowAlpha * 0.45f))
                                    )
                                )
                        )
                    }
                }
            }
            } // end CompositionLocalProvider (LTR scroll container)
        }
    }
}


@Composable
fun PersianOnScreenKeyboard(
    activeClue: Clue? = null,
    onCharTyped: (Char) -> Unit,
    onBackspace: () -> Unit = {}
) {
    val allPersianChars = remember {
        listOf(
            'ا', 'ب', 'پ', 'ت', 'ث', 'ج', 'چ', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'ژ',
            'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ک', 'گ', 'ل', 'م', 'ن', 'و', 'ه', 'ی'
        )
    }

    // حروف خاص (همزه/اعراب/فاصله) که معادل نرمال ندارند؛ فقط اگر داخل جواب باشند کلید می‌گیرند.
    val specialAnswerChars = remember { setOf('ء', 'ّ', 'ً', ' ') }

    // Extract unique solution letters for current active clue/line.
    // حروف با همان نرمال‌سازیِ مقایسهٔ جواب یکدست می‌شوند تا تضمین شود:
    // هر حرفی که سلول‌های گریدِ این سرنخ لازم دارند، حتماً روی کیبورد هست.
    // (مثلاً جواب «هیئت» → کلید «ی» که با نرمال‌سازی «ئ» گرید را می‌پوشاند؛
    //  جواب «آبکش» → کلید «ا»؛ جواب «امتلاء» → کلید «ء»)
    val answerChars = remember(activeClue?.number, activeClue?.direction, activeClue?.answer) {
        activeClue?.answer
            ?.map { char -> normalizePersianChar(char) }
            ?.filter { char -> char in allPersianChars || char in specialAnswerChars }
            ?.toSet() ?: emptySet()
    }

    // Generate exactly 15 letters guaranteed to contain all characters of current clue's solution
    val keyboardLetters = remember(activeClue?.number, activeClue?.direction, activeClue?.answer) {
        val list = mutableListOf<Char>()
        list.addAll(answerChars)

        val seed = (activeClue?.answer?.hashCode() ?: 42) + (activeClue?.number ?: 0)
        val random = kotlin.random.Random(seed.toLong())

        val distractors = allPersianChars.filter { it !in list }.shuffled(random)
        for (char in distractors) {
            if (list.size >= 15) break
            list.add(char)
        }

        while (list.size < 15) {
            list.add(allPersianChars.random(random))
        }

        val final15 = list.take(15)
        final15.shuffled(random)
    }

    val row1 = keyboardLetters.take(8)
    val row2Letters = keyboardLetters.drop(8).take(7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1 (8 Keys)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (char in row1) {
                KeyboardKey(char = char, onClick = { onCharTyped(char) }, modifier = Modifier.weight(1f))
            }
        }

        // Row 2 (7 Letters + 1 Close/Cross Action Key = 8 Keys)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (char in row2Letters) {
                KeyboardKey(char = char, onClick = { onCharTyped(char) }, modifier = Modifier.weight(1f))
            }
            // Cross / Delete Action Key
            KeyboardActionKey(
                icon = Icons.Default.Close,
                onClick = onBackspace,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            )
        }
    }
}


@Composable
fun KeyboardKey(char: Char, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "KeyScale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(65)
            isPressed = false
        }
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                }
            )
            .testTag("key_$char"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            // فاصله را با علامت ␣ نمایش بده تا کلید خالی دیده نشود (برای جواب‌هایی مثل «نیم بسمل»)
            text = if (char == ' ') "␣" else char.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun HintResultDialog(viewModel: PuzzleViewModel, onShowAddCoins: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { viewModel.dismissHintResultDialog() },
        title = {
            Text(
                text = viewModel.hintDialogTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isSuccess = !viewModel.hintDialogTitle.contains("امتیاز") && !viewModel.hintDialogTitle.contains("کافی")
                
                if (isSuccess) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.hintDialogContent,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                fontSize = 24.sp
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.hintDialogContent,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                        )
                    }
                }
                
                if (isSuccess) {
                    Text(
                        text = "راهنمای مورد نظر در خانه‌های جدول نیز با موفقیت جایگذاری شد.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        },
        confirmButton = {
            if (viewModel.hintDialogTitle.contains("کافی") || viewModel.hintDialogTitle.contains("سکه")) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val isCooldown = viewModel.adCooldownSecondsLeft > 0L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isCooldown) {
                                viewModel.dismissHintResultDialog()
                                viewModel.showInterstitialAd(context) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "تبلیغ هنوز آماده نیست. لطفاً چند لحظه دیگر دوباره تلاش کنید.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        enabled = !isCooldown,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCooldown) Color(0xFF90E0EF).copy(alpha = 0.5f) else (if (LocalDarkTheme.current) DarkPrimary else Color(0xFF00B4D8))
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isCooldown) {
                                val mins = viewModel.adCooldownSecondsLeft / 60
                                val secs = viewModel.adCooldownSecondsLeft % 60
                                "محدودیت (${String.format("%02d:%02d", mins, secs)})"
                            } else {
                                "تماشای تبلیغ (+100 سکه)"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCAF0F8)
                            )
                        )
                    }

                    Button(
                        onClick = { viewModel.dismissHintResultDialog() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "انصراف",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.dismissHintResultDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "بستن",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCAF0F8)
                        )
                    )
                }
            }
        }
    )
}


@Composable
fun GameCompletionDialog(viewModel: PuzzleViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.closeCompletedDialog() },
        title = {
            Text(
                text = "تبریک! 🎉 جدول کامل شد",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0077B6),
                fontWeight = FontWeight.Bold,
                fontFamily = PersianFontFamily
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "شما موفق شدید این جدول را با موفقیت حل کنید.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PersianFontFamily
                    )
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "100 امتیاز بابت حل جدول",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (LocalDarkTheme.current) DarkOnPrimary else Color(0xFF0096C7),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+100 🪙",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (LocalDarkTheme.current) DarkOnPrimary else Color(0xFF0096C7),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            val currentPuzzle = viewModel.activePuzzle
            val nextPuzzle = if (currentPuzzle != null) {
                val currentIndex = viewModel.currentPuzzleData().puzzlesList.indexOfFirst { it.id == currentPuzzle.id }
                if (currentIndex != -1 && currentIndex + 1 < viewModel.currentPuzzleData().puzzlesList.size) {
                    viewModel.currentPuzzleData().puzzlesList[currentIndex + 1]
                } else null
            } else null

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (nextPuzzle != null) {
                    Button(
                        onClick = {
                            viewModel.closeCompletedDialog()
                            viewModel.startPuzzle(nextPuzzle)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "جدول بعدی ➔",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = PersianFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.closeCompletedDialog()
                        viewModel.selectedSectionSize = viewModel.activePuzzle?.rows
                        viewModel.navigateTo(Screen.DIFFICULTY_SELECT)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "بازگشت به مراحل",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PersianFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.closeCompletedDialog()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "بستن",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PersianFontFamily
                    )
                )
            }
        }
    )
}


@Composable
fun GameIncorrectCompletionDialog(viewModel: PuzzleViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.closeIncorrectCompletionDialog() },
        title = {
            Text(
                text = "توجه! ⚠️ جدول اشتباه پر شده است",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "جدول را کامل کرده‌اید اما پاسخ‌ها اشتباه هستند. روی دکمه «بررسی جدول» کلیک کنید تا خانه‌های اشتباه مشخص شوند.",
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.triggerCheckFromIncorrectDialog()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("بررسی جدول")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.closeIncorrectCompletionDialog()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("بستن")
            }
        }
    )
}


@Composable
fun SettingsScreen(viewModel: PuzzleViewModel) {
    var showConfirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    SoundManager.playClick()
                    viewModel.navigateTo(Screen.HOME)
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت"
                )
            }
            
            Text(
                text = "تنظیمات بازی",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Box(modifier = Modifier.size(48.dp))
        }

        // Settings items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Sound
            SettingsToggleItem(
                title = "صداهای بازی",
                desc = "پخش صداهای راهنما، بررسی جدول و پیام تبریک",
                checked = viewModel.isSoundEnabled,
                onCheckedChange = { viewModel.toggleSound() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Reset progress
            Button(
                onClick = { showConfirmReset = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("reset_progress_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("پاک کردن تمام پیشرفت‌ها", fontWeight = FontWeight.Bold)
            }
        }

        if (showConfirmReset) {
            AlertDialog(
                onDismissRequest = { showConfirmReset = false },
                title = { Text("آیا مطمئن هستید؟", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = { Text("با تایید این گزینه، تمام امتیازها، زمان‌ها و پیشرفت بازی‌های شما به طور کامل پاک خواهند شد و قابل بازگشت نیستند.", textAlign = TextAlign.Right) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllProgress()
                            showConfirmReset = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6))
                    ) {
                        Text("پاک کردن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmReset = false }) {
                        Text("انصراف")
                    }
                }
            )
        }

        // Footer version info
        Text(
            text = "جدول بزرگسال • نسخه ۱.۰.۰",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}


@Composable
fun SettingsToggleItem(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = checked,
                onCheckedChange = {
                    SoundManager.playClick()
                    onCheckedChange(it)
                }
            )
            
            Column(
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textAlign = TextAlign.Right,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}


@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
)

fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}


@Composable
fun CellCluesDialog(
    row: Int,
    col: Int,
    viewModel: PuzzleViewModel,
    onDismiss: () -> Unit
) {
    val puzzle = viewModel.activePuzzle ?: return
    
    // Find clues for this cell
    val acrossClue = puzzle.acrossClues.find { clue ->
        clue.startRow == row && col in (clue.startCol - clue.length + 1)..clue.startCol
    }
    
    val downClue = puzzle.downClues.find { clue ->
        clue.startCol == col && row in clue.startRow..(clue.startRow + clue.length - 1)
    }

    if (acrossClue == null && downClue == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "راهنمای خانه انتخابی",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "سرنخ‌های عبوری از این خانه را مشاهده کنید و جهت دلخواه را انتخاب نمایید:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Right
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (acrossClue != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.onCellClicked(row, col)
                                if (viewModel.activeDirection != "across") {
                                    viewModel.onCellClicked(row, col) // toggle to across
                                }
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.activeDirection == "across" && viewModel.activeRow == row && viewModel.activeCol == col)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (viewModel.activeDirection == "across" && viewModel.activeRow == row && viewModel.activeCol == col)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "افقی",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = "سرنخ شماره ${acrossClue.number}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = acrossClue.clueText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.5.sp // Increased by 2sp
                                ),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (downClue != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.onCellClicked(row, col)
                                if (viewModel.activeDirection != "down") {
                                    viewModel.onCellClicked(row, col) // toggle to down
                                }
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.activeDirection == "down" && viewModel.activeRow == row && viewModel.activeCol == col)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (viewModel.activeDirection == "down" && viewModel.activeRow == row && viewModel.activeCol == col)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "عمودی",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = "سرنخ شماره ${downClue.number}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = downClue.clueText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.5.sp // Increased by 2sp
                                ),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("تایید و بستن")
            }
        }
    )
}


@Composable
fun HelpScreen(viewModel: PuzzleViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun formatPersianTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        val englishStr = String.format("%02d:%02d", mins, secs)
        val persianDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return englishStr.map { char ->
            if (char.isDigit()) persianDigits[char - '0'] else char
        }.joinToString("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Custom Header Bar with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Space holder to keep title perfectly centered
            Spacer(modifier = Modifier.width(44.dp))

            Text(
                text = "راهنمای بازی و سکه رایگان",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            IconButton(
                onClick = {
                    SoundManager.playClick()
                    viewModel.navigateBack()
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Scrollable help content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card 1: Watch Ads for Free Coins (کسب سکه رایگان)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) DarkSurface else Color(0xFFADE8F4)), // Green/mint background
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, if (LocalDarkTheme.current) DarkPrimary else Color(0xFF00B4D8))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = if (LocalDarkTheme.current) DarkOnPrimary else Color(0xFF0096C7),
                        modifier = Modifier.size(56.dp)
                    )

                    Text(
                        text = "دریافت ۱۰۰ سکه رایگان هدیه",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (LocalDarkTheme.current) DarkPrimary else Color(0xFF0077B6)
                        )
                    )

                    Text(
                        text = "با تماشای هر ویدیوی تبلیغاتی کوتاه، ۱۰۰ سکه طلایی رایگان دریافت کنید و در حل جدول‌ها از آن‌ها استفاده نمایید.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (LocalDarkTheme.current) DarkPrimary else Color(0xFF0077B6),
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    if (viewModel.adCooldownSecondsLeft > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val progress = viewModel.adCooldownSecondsLeft.toFloat() / 300f
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (LocalDarkTheme.current) DarkOnPrimary else Color(0xFF0096C7),
                            trackColor = if (LocalDarkTheme.current) DarkSurface else Color(0xFFADE8F4)
                        )
                        Text(
                            text = "محدودیت زمانی فعال است: ${formatPersianTime(viewModel.adCooldownSecondsLeft)} دیگر",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0096C7)
                            )
                        )
                    }

                    Button(
                        onClick = {
                            SoundManager.playClick()
                            viewModel.showInterstitialAd(context) {
                                Toast.makeText(context, "تبلیغ در حال بارگذاری است، لطفا لحظاتی بعد دوباره کلیک کنید.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = viewModel.adCooldownSecondsLeft <= 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (LocalDarkTheme.current) DarkPrimary else Color(0xFF00B4D8),
                            disabledContainerColor = Color(0xFFADE8F4)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (viewModel.adCooldownSecondsLeft <= 0) Color(0xFFCAF0F8) else Color(0xFF90E0EF)
                            )
                            Text(
                                text = "مشاهده ویدیو و کسب سکه",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewModel.adCooldownSecondsLeft <= 0) Color(0xFFCAF0F8) else Color(0xFF90E0EF)
                                )
                            )
                        }
                    }
                }
            }

            // Card 2: How to Play (راهنمای گام به گام حل جدول)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "آموزش گام به گام حل جدول",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        HelpStepRow(
                            stepNumber = "۱",
                            title = "انتخاب خانه جدول",
                            description = "بر روی هر کدام از خانه‌های جدول که کلیک کنید، آن خانه و سرنخ مربوط به آن فعال می‌شود."
                        )
                        HelpStepRow(
                            stepNumber = "۲",
                            title = "تغییر جهت نوشتن",
                            description = "با زدن مجدد روی همان خانه جدول، جهت نوشتن بین «افقی» و «عمودی» تغییر می‌کند."
                        )
                        HelpStepRow(
                            stepNumber = "۳",
                            title = "وارد کردن کلمات",
                            description = "با استفاده از دکمه‌های حروف کیبورد پایین صفحه، جدول را پر کنید."
                        )
                        HelpStepRow(
                            stepNumber = "۴",
                            title = "بررسی درستی کلمات",
                            description = "با کلیک بر روی دکمه «بررسی کلمات» می‌توانید کلمات وارد شده را چک کنید."
                        )
                    }
                }
            }

            // Card 3: Coin Scoring System (سیستم امتیازدهی سکه‌ها)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "قوانین کسب و خرج سکه",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "• شروع هر جدول جدید: دارای ۲۰۰ سکه اولیه",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "• نوشتن کلمات صحیح: ۵ سکه بابت هر کلمه درست",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "• جایزه اتمام موفق جدول: تا ۲۰۰ سکه اضافی بر اساس درجه سختی جدول",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "• راهنمای آشکارسازی حرف فعال: کسر ۳۰ سکه",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "• راهنمای آشکارسازی کل کلمه: کسر ۱۰۰ سکه",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "• پاک کردن حروف اشتباه در جدول: کسر ۳۰ سکه",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Native Ad Banner at the bottom of HelpScreen as requested
        NativeAdBanner(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}


@Composable
fun HelpStepRow(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                textAlign = TextAlign.Right
            )
        }
        
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}


@Composable
fun NativeAdBanner(modifier: Modifier = Modifier) {
    var showRedFlash by remember { mutableStateOf(false) }
    val adViewRef = remember { arrayOfNulls<com.adivery.sdk.AdiveryNativeAdView>(1) }

    // Scale pulse: 3% bounce every 30s
    var scaleTarget by remember { mutableFloatStateOf(1f) }
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "ScalePulse"
    )
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L); scaleTarget = 1.03f; delay(600); scaleTarget = 1f
        }
    }

    // Red flash alpha
    val redAlpha by animateFloatAsState(
        targetValue = if (showRedFlash) 0.55f else 0f,
        animationSpec = tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "RedFlash"
    )

    // Red alarm every 3 minutes — 3 pulses (ad is NOT reloaded anymore)
    LaunchedEffect(Unit) {
        while (true) {
            delay(180_000L)
            repeat(3) {
                showRedFlash = true; delay(350)
                showRedFlash = false; delay(250)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.White, MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))))
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .padding(10.dp)
            ) {
                // Ad loads exactly ONCE when this composable enters composition.
                // No adReloadKey / periodic rebuild — the 3-minute reload was removed.
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val inflater = android.view.LayoutInflater.from(context)
                        val adView = inflater.inflate(
                            com.jadval.shahr.R.layout.native_ad_container, null
                        ) as com.adivery.sdk.AdiveryNativeAdView
                        adViewRef[0] = adView
                        adView.post {
                            try {
                                val wrapper = adView.findViewById<android.view.View>(com.jadval.shahr.R.id.adivery_wrapper)
                                wrapper?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                val headline = adView.findViewById<android.widget.TextView>(com.jadval.shahr.R.id.adivery_headline)
                                headline?.setTextColor(android.graphics.Color.BLACK)
                                headline?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                                try { headline?.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, com.jadval.shahr.R.font.b_ziba_0) } catch (_: Exception) {}
                                val description = adView.findViewById<android.widget.TextView>(com.jadval.shahr.R.id.adivery_description)
                                description?.setTextColor(android.graphics.Color.BLACK)
                                description?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                                try { description?.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, com.jadval.shahr.R.font.b_ziba_0) } catch (_: Exception) {}
                                val cta = adView.findViewById<android.widget.Button>(com.jadval.shahr.R.id.adivery_call_to_action)
                                cta?.setTextColor(android.graphics.Color.BLACK)
                                cta?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                                try {
                                    cta?.background = android.graphics.drawable.GradientDrawable().apply {
                                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                                        cornerRadius = 16f
                                        setColor(android.graphics.Color.parseColor("#FFB300"))
                                    }
                                } catch (_: Exception) {}
                                try { cta?.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, com.jadval.shahr.R.font.b_ziba_0) } catch (_: Exception) {}
                            } catch (e: Exception) {
                                android.util.Log.e("Adivery", "Error styling native ad views", e)
                            }
                        }
                        adView.setListener(object : com.adivery.sdk.AdiveryAdListener() {
                            override fun onAdLoaded() {}
                            override fun onError(reason: String) {
                                android.util.Log.e("Adivery", "Native Ad Error: $reason")
                            }
                        })
                        adView.loadAd()
                        adView
                    }
                )
            }
            // Red flash overlay — full card, 3 strong pulses every 3 minutes
            if (redAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Red.copy(alpha = redAlpha))
                )
            }
        }
    }
}

@Composable
fun KeyboardActionKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "KeyScale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(65)
            isPressed = false
        }
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "عملگر",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}
