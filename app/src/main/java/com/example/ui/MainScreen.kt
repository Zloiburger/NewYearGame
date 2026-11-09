package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// ==========================================
// МАТЕМАТИКА И ФОРМАТИРОВАНИЕ ЧИСЕЛ
// ==========================================
fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> String.format("%.1fB", number / 1_000_000_000.0)
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 10_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

fun calculateUpgradeCost(baseCost: Long, level: Int): Long {
    return (baseCost * 1.5.pow(level)).toLong()
}

// ==========================================
// СОХРАНЕНИЕ ДАННЫХ
// ==========================================
data class GameSaveData(
    val snowflakes: Long,
    val treeLevel: Int,
    val incomeRate: Int,
    val catSatiety: Int,
    val hasWinterCoat: Boolean,
    val lastUpdateTime: Long,
    val lastDailyRewardDay: String,
    val bowlLevel: Int,
    val clickPower: Int
)

object GameStorage {
    private const val PREFS_NAME = "cozy_new_year_prefs"
    fun load(context: Context): GameSaveData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return GameSaveData(
            snowflakes = prefs.getLong("snowflakes", 0L),
            treeLevel = prefs.getInt("tree_level", 0),
            incomeRate = prefs.getInt("income_rate", 0),
            catSatiety = prefs.getInt("cat_satiety", 80),
            hasWinterCoat = prefs.getBoolean("has_winter_coat", false),
            lastUpdateTime = prefs.getLong("last_update_time", 0L),
            lastDailyRewardDay = prefs.getString("last_daily_reward_day", "") ?: "",
            bowlLevel = prefs.getInt("bowl_level", 0),
            clickPower = prefs.getInt("click_power", 1)
        )
    }
    fun save(context: Context, data: GameSaveData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("snowflakes", data.snowflakes)
            .putInt("tree_level", data.treeLevel)
            .putInt("income_rate", data.incomeRate)
            .putInt("cat_satiety", data.catSatiety)
            .putBoolean("has_winter_coat", data.hasWinterCoat)
            .putLong("last_update_time", data.lastUpdateTime)
            .putString("last_daily_reward_day", data.lastDailyRewardDay)
            .putInt("bowl_level", data.bowlLevel)
            .putInt("click_power", data.clickPower)
            .apply()
    }
}

// Оставили только 2 вкладки
enum class GameTab(val label: String, val icon: ImageVector) {
    ROOM("Комната", Icons.Filled.Home),
    STREET("Улица", Icons.Filled.AcUnit)
}

enum class MaidenOutfit(val title: String, val emoji: String) {
    DEFAULT("Обычный наряд", "👧"),
    WINTER_COAT("Зимняя шуба", "👩‍🎤")
}

// ==========================================
// ЭКРАН ЗАГРУЗКИ
// ==========================================
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) { delay(2000); onTimeout() }
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1321), Color(0xFF131D31)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🎄", fontSize = 80.sp, modifier = Modifier.scale(1.2f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Новогодняя Сказка", color = GoldAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Загрузка волшебства...", color = SnowSoft, fontSize = 14.sp)
        }
    }
}

// ==========================================
// ГЛАВНЫЙ ЭКРАН
// ==========================================
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedState = remember { GameStorage.load(context) }

    var showSplash by remember { mutableStateOf(true) }
    var snowflakes by remember { mutableLongStateOf(savedState.snowflakes) }
    var treeLevel by remember { mutableIntStateOf(savedState.treeLevel) }
    var incomeRate by remember { mutableIntStateOf(savedState.incomeRate) }
    var catSatiety by remember { mutableIntStateOf(savedState.catSatiety) }
    var hasWinterCoat by remember { mutableStateOf(savedState.hasWinterCoat) }
    var bowlLevel by remember { mutableIntStateOf(savedState.bowlLevel) }
    var clickPower by remember { mutableIntStateOf(savedState.clickPower) }
    var lastDailyRewardDay by remember { mutableStateOf(savedState.lastDailyRewardDay) }

    val effectiveIncomeRate = maxOf(incomeRate, treeLevel + (if (hasWinterCoat) 2 else 0) + bowlLevel)
    val todayDateStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
    val isDailyRewardClaimedToday = lastDailyRewardDay == todayDateStr

    var currentMaidenOutfit by remember { mutableStateOf(if (savedState.hasWinterCoat) MaidenOutfit.WINTER_COAT else MaidenOutfit.DEFAULT) }
    
    // Состояния для диалогов
    var isWardrobeOpen by remember { mutableStateOf(false) }
    var isShopOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isFeedCatDialogOpen by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf(GameTab.ROOM) }
    var lightsMode by remember { mutableIntStateOf(0) }
    var snowIntensity by remember { mutableFloatStateOf(1.0f) }
    var isMusicEnabled by remember { mutableStateOf(true) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    fun showToast(msg: String) { toastMessage = msg }
    LaunchedEffect(toastMessage) { if (toastMessage != null) { delay(3000); toastMessage = null } }

    fun persistGameState() {
        GameStorage.save(context, GameSaveData(
            snowflakes, treeLevel, effectiveIncomeRate, catSatiety, hasWinterCoat,
            System.currentTimeMillis(), lastDailyRewardDay, bowlLevel, clickPower
        ))
    }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val lastTime = savedState.lastUpdateTime
        val offlineRate = maxOf(savedState.incomeRate, savedState.treeLevel + (if (savedState.hasWinterCoat) 2 else 0) + savedState.bowlLevel)
        if (lastTime > 0L && offlineRate > 0) {
            val elapsedSeconds = ((now - lastTime) / 1000).coerceAtLeast(0)
            if (elapsedSeconds >= 60) {
                val cappedSeconds = minOf(elapsedSeconds, 24 * 3600)
                val offlineSnowflakes = (cappedSeconds * offlineRate).toLong()
                if (offlineSnowflakes > 0) { snowflakes += offlineSnowflakes; showToast("Пока вас не было, накоплено ${formatNumber(offlineSnowflakes)} ❄️!") }
            }
        }
        persistGameState()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) persistGameState() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); persistGameState() }
    }

    LaunchedEffect(Unit) {
        var tickCounter = 0
        while (true) {
            delay(1000); tickCounter++
            val currentIncome = maxOf(incomeRate, treeLevel + (if (hasWinterCoat) 2 else 0) + bowlLevel)
            if (currentIncome > 0) { snowflakes += currentIncome }
            if (tickCounter % 10 == 0 && catSatiety > 0) { catSatiety = (catSatiety - 1).coerceAtLeast(0) }
            if (tickCounter % 10 == 0) { persistGameState() }
        }
    }

    var isCatAwake by remember { mutableStateOf(false) }
    var catPlusOneTrigger by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var snowmanPlusOneTrigger by remember { mutableIntStateOf(0) }
    val isRoomTab = currentTab == GameTab.ROOM

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = if (isRoomTab) listOf(Color(0xFF240E14), Color(0xFF1D0B10), Color(0xFF17090D), Color(0xFF110609)) else listOf(Color(0xFF060914), Color(0xFF0C182B), Color(0xFF161B3B), Color(0xFF22133D)))).windowInsetsPadding(WindowInsets.statusBars)) {
        val isTablet = maxWidth > 600.dp
        val tabletScale = if (isTablet) 1.4f else 1.0f

        if (isRoomTab) RoomInteriorScenery() else StreetExteriorScenery()
        SnowfallLayer(isForeground = false, intensity = if (isRoomTab) snowIntensity * 0.40f else snowIntensity * 1.5f)

        // ДЕД МОРОЗ И СНЕГУРОЧКА
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = if (isTablet) 72.dp else 56.dp, bottom = if (isTablet) 96.dp else 84.dp).scale(tabletScale)) {
            HolidayHostsBackground(currentOutfit = currentMaidenOutfit, onSnegurochkaClick = { isWardrobeOpen = true })
        }

        // ЕЛКА
        Box(modifier = Modifier.align(Alignment.Center).fillMaxHeight(if (isTablet) 0.92f else 0.88f).padding(bottom = 20.dp).scale((if (isRoomTab) 0.70f else 1.0f) * tabletScale), contentAlignment = Alignment.BottomCenter) {
            ChristmasTree25D(isRoom = isRoomTab, treeLevel = treeLevel, garlandMode = lightsMode, onTreeClick = {
                if (treeLevel == 0) showToast("Ёлочка пока без украшений! Улучши её ✨")
                else { lightsMode = (lightsMode + 1) % 3; snowflakes += clickPower.toLong(); persistGameState(); showToast("+${clickPower} ❄️ за украшение елки!") }
            })
        }

        // КНОПКА УЛУЧШЕНИЯ ЕЛКИ
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (isTablet) 115.dp else 102.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1C1335).copy(alpha = 0.94f), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.7f)), shadowElevation = 4.dp, modifier = Modifier.padding(end = 6.dp)) {
                Text(text = if (treeLevel == 0) "Ур. 0" else "Ур. $treeLevel", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }

            val upgradeCost = calculateUpgradeCost(50L, treeLevel)
            val canUpgrade = snowflakes >= upgradeCost
            Surface(shape = RoundedCornerShape(16.dp), color = if (canUpgrade) GoldAccent else NightSurfaceVariant.copy(alpha = 0.95f), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent), shadowElevation = 6.dp, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                if (canUpgrade) { snowflakes -= upgradeCost; treeLevel += 1; incomeRate += 1; persistGameState(); showToast("Ёлка стала краше! ✨ Ур. $treeLevel") }
                else showToast("Нужно ${formatNumber(upgradeCost)} ❄️!")
            }) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⭐", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "Улучшить ёлку (${formatNumber(upgradeCost)} ❄️)", color = if (canUpgrade) Color(0xFF1C1200) else SnowWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // КОТ
        if (isRoomTab) {
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = if (isTablet) 36.dp else 24.dp, bottom = if (isTablet) 86.dp else 76.dp).scale(tabletScale)) {
                InteractiveCat(isRoom = true, satiety = catSatiety, isAwake = isCatAwake, plusOneTrigger = catPlusOneTrigger, bowlLevel = bowlLevel, canUpgradeBowl = snowflakes >= calculateUpgradeCost(30L, bowlLevel), onUpgradeBowl = {
                    val cost = calculateUpgradeCost(30L, bowlLevel)
                    if (snowflakes >= cost) { snowflakes -= cost; bowlLevel += 1; incomeRate += 1; persistGameState(); showToast("Миска улучшена! 🥣✨") }
                    else showToast("Недостаточно снежинок! Нужно ${formatNumber(cost)} ❄️")
                }, onBowlClick = { isFeedCatDialogOpen = true }, onCatClick = {
                    if (catSatiety > 0) { snowflakes += clickPower.toLong(); catPlusOneTrigger++; isCatAwake = true; persistGameState(); coroutineScope.launch { delay(1000); isCatAwake = false } }
                    else isFeedCatDialogOpen = true
                }, clickPower = clickPower)
            }
        }

        // СНЕГОВИК
        if (!isRoomTab) {
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = if (isTablet) 36.dp else 28.dp, bottom = if (isTablet) 86.dp else 76.dp).scale(tabletScale)) {
                InteractiveSnowman(plusOneTrigger = snowmanPlusOneTrigger, onSnowmanClick = { snowflakes += clickPower.toLong(); snowmanPlusOneTrigger++; persistGameState() }, clickPower = clickPower)
            }
        }

        if (!isRoomTab) SnowfallLayer(isForeground = true, intensity = snowIntensity * 1.4f)

        // ЕЖЕДНЕВНЫЙ ПОДАРОК
        DailyRewardButton(isClaimedToday = isDailyRewardClaimedToday, onClaimReward = {
            if (!isDailyRewardClaimedToday) { snowflakes += 100; lastDailyRewardDay = todayDateStr; persistGameState(); showToast("Подарок получен! +100 ❄️ 🎁") }
            else showToast("Вы уже забрали подарок сегодня! 🎁")
        }, modifier = Modifier.align(Alignment.TopStart).padding(top = 10.dp, start = 16.dp))

        // СЧЕТЧИК
        SnowflakeCounterTop(snowflakes = snowflakes, incomeRate = effectiveIncomeRate, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))

        // ВЕРХНЯЯ ПАНЕЛЬ СПРАВА
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { isWardrobeOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, GoldAccent.copy(alpha = 0.5f), CircleShape)) { Text(text = "👗", fontSize = 16.sp) }
            IconButton(onClick = { isShopOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, GoldAccent.copy(alpha = 0.5f), CircleShape)) { Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = "Магазин", tint = GoldAccent, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { isSettingsOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, IceCyan.copy(alpha = 0.3f), CircleShape)) { Icon(imageVector = Icons.Filled.Settings, contentDescription = "Настройки", tint = GoldAccent, modifier = Modifier.size(18.dp)) }
        }

        GameToast(message = toastMessage, modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp))

        // НИЖНЯЯ ПАНЕЛЬ (КОМПАКТНАЯ)
        GameBottomNavigationBar(currentTab = currentTab, onTabSelected = { tab -> currentTab = tab }, modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars))

        // ДИАЛОГ МАГАЗИНА
        if (isShopOpen) {
            ShopDialog(snowflakes = snowflakes, clickPower = clickPower, treeLevel = treeLevel, onDismiss = { isShopOpen = false }, onBuyItem = { itemIndex, cost ->
                when (itemIndex) {
                    0 -> if (snowflakes >= 15) { snowflakes -= 15; catSatiety = (catSatiety + 30).coerceAtMost(100); persistGameState(); showToast("Мур! Кот покормлен 🐟") } else showToast("Мало снежинок!")
                    1 -> {
                        val costTree = calculateUpgradeCost(50L, treeLevel)
                        if (snowflakes >= costTree) { snowflakes -= costTree; treeLevel += 1; incomeRate += 1; persistGameState(); showToast("Звезда установлена! ⭐") } else showToast("Мало снежинок!")
                    }
                    2 -> if (snowflakes >= 100) { snowflakes -= 100; lightsMode = 1; persistGameState(); showToast("Гирлянда установлена! 💡✨") } else showToast("Мало снежинок!")
                    3 -> {
                        val costClick = calculateUpgradeCost(20L, clickPower)
                        if (snowflakes >= costClick) { snowflakes -= costClick; clickPower += 1; persistGameState(); showToast("Сила клика увеличена! 👆✨") } else showToast("Мало снежинок!")
                    }
                }
            })
        }

        if (isFeedCatDialogOpen) {
            FeedCatDialog(snowflakes = snowflakes, currentSatiety = catSatiety, onDismiss = { isFeedCatDialogOpen = false }, onConfirmFeed = {
                if (snowflakes >= 15) { snowflakes -= 15; catSatiety = (catSatiety + 30).coerceAtMost(100); persistGameState(); showToast("Кот покормлен 🐟") } else showToast("Мало снежинок!")
                isFeedCatDialogOpen = false
            })
        }

        if (isWardrobeOpen) {
            WardrobeDialog(snowflakes = snowflakes, currentOutfit = currentMaidenOutfit, hasWinterCoat = hasWinterCoat, onSelectDefault = { currentMaidenOutfit = MaidenOutfit.DEFAULT; persistGameState(); isWardrobeOpen = false }, onBuyOrEquipCoat = {
                if (hasWinterCoat) { currentMaidenOutfit = MaidenOutfit.WINTER_COAT; persistGameState(); isWardrobeOpen = false }
                else if (snowflakes >= 100) { snowflakes -= 100; hasWinterCoat = true; currentMaidenOutfit = MaidenOutfit.WINTER_COAT; incomeRate += 2; persistGameState(); isWardrobeOpen = false; showToast("Шуба куплена! +2 ❄️/сек") }
                else showToast("Мало снежинок! Нужно 100 ❄️")
            }, onDismiss = { isWardrobeOpen = false })
        }

        if (isSettingsOpen) {
            SettingsDialog(snowIntensity = snowIntensity, onIntensityChange = { snowIntensity = it }, isMusicEnabled = isMusicEnabled, onMusicToggle = { isMusicEnabled = it }, onDismiss = { isSettingsOpen = false })
        }

        if (showSplash) { SplashScreen(onTimeout = { showSplash = false }) }
    }
}

// ==========================================
// КОМПОНЕНТЫ ИНТЕРФЕЙСА
// ==========================================
@Composable
fun HolidayHostsBackground(currentOutfit: MaidenOutfit, onSnegurochkaClick: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "HostsIdle")
    val sway by transition.animateFloat(initialValue = -1.5f, targetValue = 1.5f, animationSpec = infiniteRepeatable(animation = tween(2200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "HostsSway")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(shape = RoundedCornerShape(10.dp), color = NightSurface.copy(alpha = 0.88f), border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)), modifier = Modifier.padding(bottom = 4.dp)) {
            Text(text = "Хозяева праздника ✨", color = GoldAccentLight, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.offset(y = sway.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🎅", fontSize = 38.sp)
                Canvas(modifier = Modifier.size(36.dp, 8.dp)) { drawOval(color = Color.Black.copy(alpha = 0.45f), size = Size(size.width, size.height)) }
                Text(text = "Дед Мороз", color = HolidayRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSnegurochkaClick() }.testTag("snegurochka_host")) {
                if (currentOutfit == MaidenOutfit.WINTER_COAT) Text(text = "👑", fontSize = 11.sp)
                Text(text = currentOutfit.emoji, fontSize = 35.sp)
                Canvas(modifier = Modifier.size(32.dp, 8.dp)) { drawOval(color = Color.Black.copy(alpha = 0.45f), size = Size(size.width, size.height)) }
                Surface(shape = RoundedCornerShape(6.dp), color = if (currentOutfit == MaidenOutfit.WINTER_COAT) GoldAccent.copy(alpha = 0.25f) else NightSurfaceVariant.copy(alpha = 0.8f), border = androidx.compose.foundation.BorderStroke(1.dp, if (currentOutfit == MaidenOutfit.WINTER_COAT) GoldAccent else IceCyan.copy(alpha = 0.5f))) {
                    Text(text = if (currentOutfit == MaidenOutfit.WINTER_COAT) "В шубе 👗" else "Снегурочка 👗", color = if (currentOutfit == MaidenOutfit.WINTER_COAT) GoldAccent else IceCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
        }
    }
}

@Composable
fun ChristmasTree25D(isRoom: Boolean, treeLevel: Int, garlandMode: Int, onTreeClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "TreeAnimation")
    val starGlow by infiniteTransition.animateFloat(initialValue = 0.6f, targetValue = 1.0f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "StarGlow")
    val lightBlink by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1.0f, animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "LightBlink")
    val treeBoxWidth = if (isRoom) 210.dp else 290.dp
    BoxWithConstraints(modifier = modifier.width(treeBoxWidth).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTreeClick() }.testTag("christmas_tree"), contentAlignment = Alignment.BottomCenter) {
        val totalHeight = maxHeight
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(if (treeLevel > 0) 44.dp else 28.dp).scale(if (treeLevel > 0) (0.9f + starGlow * 0.2f + (treeLevel - 1) * 0.04f) else 1f), contentAlignment = Alignment.Center) {
                if (treeLevel > 0) {
                    Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(brush = Brush.radialGradient(colors = listOf(GoldAccent.copy(alpha = 0.75f * starGlow), Color.Transparent)), radius = size.width / 1.4f) }
                    Text(text = "⭐", fontSize = if (isRoom) 28.sp else 34.sp)
                } else {
                    Canvas(modifier = Modifier.size(24.dp, 20.dp)) {
                        val path = Path().apply { moveTo(size.width / 2f, 2.dp.toPx()); lineTo(size.width - 2.dp.toPx(), size.height); lineTo(2.dp.toPx(), size.height); close() }
                        drawPath(path = path, color = PineGreenDark)
                        drawCircle(color = SnowWhite.copy(alpha = 0.8f), radius = 3.dp.toPx(), center = Offset(size.width / 2f, 3.dp.toPx()))
                    }
                }
            }
            val canvasTreeWidth = if (isRoom) 195.dp else 265.dp
            Canvas(modifier = Modifier.width(canvasTreeWidth).height(totalHeight - (if (treeLevel > 0) 74.dp else 48.dp))) {
                val w = size.width; val h = size.height; val midX = w / 2f
                val shadowW = w * 0.86f; val shadowH = 20.dp.toPx(); val shadowY = h - 4.dp.toPx()
                drawOval(brush = Brush.radialGradient(colors = listOf(Color.Black.copy(alpha = 0.70f), Color.Black.copy(alpha = 0.30f), Color.Transparent), center = Offset(midX, shadowY), radius = shadowW / 2f), topLeft = Offset(midX - shadowW / 2f, shadowY - shadowH / 2f), size = Size(shadowW, shadowH))
                val boxW = if (isRoom) 56.dp.toPx() else 70.dp.toPx(); val boxH = if (isRoom) 22.dp.toPx() else 28.dp.toPx(); val boxY = h - boxH - 4.dp.toPx()
                drawRoundRect(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF8B1E32), HolidayRed, Color(0xFF5B1220)), startX = midX - boxW / 2f, endX = midX + boxW / 2f), topLeft = Offset(midX - boxW / 2f, boxY), size = Size(boxW, boxH), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
                drawRect(color = GoldAccent, topLeft = Offset(midX - 3.dp.toPx(), boxY), size = Size(6.dp.toPx(), boxH))
                drawRect(color = GoldAccent, topLeft = Offset(midX - boxW / 2f, boxY + boxH * 0.42f), size = Size(boxW, 4.dp.toPx()))
                drawCircle(color = GoldAccent, radius = 3.5.dp.toPx(), center = Offset(midX, boxY))
                val smallBoxW = if (isRoom) 28.dp.toPx() else 34.dp.toPx(); val smallBoxH = if (isRoom) 17.dp.toPx() else 22.dp.toPx()
                drawRoundRect(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF1E3A5F), IceCyan, Color(0xFF132742))), topLeft = Offset(midX - boxW / 2f - smallBoxW * 0.7f, h - smallBoxH - 4.dp.toPx()), size = Size(smallBoxW, smallBoxH), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()))
                drawRoundRect(brush = Brush.horizontalGradient(colors = listOf(GoldAccentDark, GoldAccent, GoldAccentDark)), topLeft = Offset(midX + boxW / 2f - smallBoxW * 0.3f, h - smallBoxH - 4.dp.toPx()), size = Size(smallBoxW, smallBoxH), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()))
                val tiers = if (isRoom) 3 else 4; val pineBodyHeight = h - boxH - 8.dp.toPx(); val tierBaseHeight = pineBodyHeight / tiers
                for (i in 0 until tiers) {
                    val tierProgress = (i + 1).toFloat() / tiers; val tierTop = i * (tierBaseHeight * 0.72f); val tierBottom = tierTop + tierBaseHeight * 1.35f; val tierHalfWidth = (w * 0.23f) + (tierProgress * w * 0.27f)
                    val pinePath = Path().apply { moveTo(midX, tierTop); lineTo(midX + tierHalfWidth, tierBottom); lineTo(midX + tierHalfWidth * 0.65f, tierBottom - 10.dp.toPx()); lineTo(midX, tierBottom - 2.dp.toPx()); lineTo(midX - tierHalfWidth * 0.65f, tierBottom - 10.dp.toPx()); lineTo(midX - tierHalfWidth, tierBottom); close() }
                    val branchColors = if (treeLevel == 0) listOf(Color(0xFF14361E), Color(0xFF0F2B18), Color(0xFF091F11)) else listOf(Color(0xFF40916C), Color(0xFF2D6A4F), Color(0xFF1B4332))
                    drawPath(path = pinePath, brush = Brush.linearGradient(colors = branchColors, start = Offset(midX - tierHalfWidth, tierBottom), end = Offset(midX + tierHalfWidth, tierTop)))
                    val snowPath = Path().apply { moveTo(midX + tierHalfWidth, tierBottom); lineTo(midX + tierHalfWidth * 0.5f, tierBottom - 11.dp.toPx()); lineTo(midX, tierBottom - 3.dp.toPx()); lineTo(midX - tierHalfWidth * 0.5f, tierBottom - 11.dp.toPx()); lineTo(midX - tierHalfWidth, tierBottom); lineTo(midX - tierHalfWidth * 0.85f, tierBottom - 4.dp.toPx()); lineTo(midX, tierBottom); lineTo(midX + tierHalfWidth * 0.85f, tierBottom - 4.dp.toPx()); close() }
                    drawPath(path = snowPath, color = SnowWhite.copy(alpha = 0.88f))
                    if (treeLevel > 0) {
                        val garlandY = tierBottom - tierBaseHeight * 0.45f
                        val garlandCurve = Path().apply { moveTo(midX - tierHalfWidth * 0.75f, garlandY - 8f); quadraticTo(midX, garlandY + 14f, midX + tierHalfWidth * 0.75f, garlandY - 8f) }
                        drawPath(path = garlandCurve, color = GoldAccentDark.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx()))
                        val lightColors = when (garlandMode) { 1 -> listOf(HolidayRed, IceCyan, GoldAccent, SnowWhite); 2 -> listOf(GoldAccent, WarmGlow, Color(0xFFFFB703), GoldAccentLight); else -> listOf(GoldAccent, HolidayRed, IceCyan, Color(0xFF52B788)) }
                        val points = 5
                        for (p in 0..points) {
                            val t = p.toFloat() / points; val lx = midX - tierHalfWidth * 0.75f + t * (tierHalfWidth * 1.5f); val ly = garlandY - 8f + (sin(t * PI.toFloat()) * 22f); val lightColor = lightColors[(p + i) % lightColors.size]; val blinkFactor = if ((p + i) % 2 == 0) lightBlink else (1.2f - lightBlink)
                            drawCircle(color = lightColor.copy(alpha = 0.45f * blinkFactor), radius = 8.dp.toPx(), center = Offset(lx, ly))
                            drawCircle(color = lightColor.copy(alpha = (0.75f * blinkFactor).coerceIn(0f, 1f)), radius = 3.8.dp.toPx(), center = Offset(lx, ly))
                        }
                        val baubleColors = listOf(HolidayRed, GoldAccent, IceCyan, Color(0xFFC77DFF))
                        val bx = if (i % 2 == 0) midX - tierHalfWidth * 0.58f else midX + tierHalfWidth * 0.58f; val by = tierBottom - 13.dp.toPx(); val bColor = baubleColors[i % baubleColors.size]
                        drawRect(color = GoldAccentDark, topLeft = Offset(bx - 2.dp.toPx(), by - 10.dp.toPx()), size = Size(4.dp.toPx(), 4.dp.toPx()))
                        drawCircle(brush = Brush.radialGradient(colors = listOf(Color.White, bColor, Color.Black.copy(alpha = 0.4f)), center = Offset(bx - 2.dp.toPx(), by - 5.dp.toPx()), radius = 6.5.dp.toPx()), radius = 6.dp.toPx(), center = Offset(bx, by - 4.dp.toPx()))
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingClickParticle(trigger: Int, text: String = "+1 ❄️", color: Color = GoldAccent) {
    var activeTrigger by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) { activeTrigger = trigger; progress.snapTo(0f); progress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)) }
    }
    if (activeTrigger > 0 && progress.value < 1f) {
        val p = progress.value
        Text(text = text, color = color.copy(alpha = (1f - p).coerceIn(0f, 1f)), fontSize = (14 + (1f - p) * 3).sp, fontWeight = FontWeight.Black, modifier = Modifier.offset(y = (-p * 32).dp).scale(0.85f + p * 0.35f))
    }
}

@Composable
fun InteractiveCat(isRoom: Boolean, satiety: Int, isAwake: Boolean, plusOneTrigger: Int, bowlLevel: Int = 0, canUpgradeBowl: Boolean = false, onUpgradeBowl: () -> Unit = {}, onBowlClick: () -> Unit = {}, onCatClick: () -> Unit, clickPower: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "CatBreathing")
    val breathScale by transition.animateFloat(initialValue = 1.0f, targetValue = 1.06f, animationSpec = infiniteRepeatable(animation = tween(1800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "CatBreath")
    val zzzOffset by transition.animateFloat(initialValue = 0f, targetValue = -24f, animationSpec = infiniteRepeatable(animation = tween(2200, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "ZzzOffset")
    val zzzAlpha by transition.animateFloat(initialValue = 0.85f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(2200, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "ZzzAlpha")
    val catEmoji = when { isAwake -> "😻"; satiety <= 0 -> "😿"; else -> "🐈‍⬛" }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.Center) {
            FloatingClickParticle(trigger = plusOneTrigger, text = "+$clickPower ❄️", color = GoldAccent)
            if (!isAwake && plusOneTrigger == 0 && satiety > 0) { Text(text = "z Z z", color = IceCyan.copy(alpha = zzzAlpha), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(x = 10.dp, y = zzzOffset.dp)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(NightSurface.copy(alpha = 0.88f)).border(1.dp, if (satiety > 0) GoldAccent.copy(alpha = 0.4f) else HolidayRed.copy(alpha = 0.6f), RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(text = "🐟", fontSize = 10.sp); Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.width(48.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF261D3B))) {
                val fillRatio = (satiety / 100f).coerceIn(0f, 1f); val barColor = when { satiety > 50 -> Color(0xFF52B788); satiety > 20 -> CozyAmber; else -> HolidayRed }
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fillRatio).background(barColor))
            }
            Spacer(modifier = Modifier.width(4.dp)); Text(text = "$satiety%", color = if (satiety > 0) SnowSoft else HolidayRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onCatClick() }.testTag("interactive_cat"), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(86.dp, 36.dp)) {
                    val w = size.width; val h = size.height
                    drawOval(brush = Brush.radialGradient(colors = listOf(Color.Black.copy(alpha = 0.68f), Color.Black.copy(alpha = 0.30f), Color.Transparent), radius = w / 2f), size = Size(w, h))
                    if (isRoom) {
                        drawRoundRect(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF5B1E28), Color(0xFF8C2E3C), Color(0xFF5B1E28))), topLeft = Offset(4.dp.toPx(), 6.dp.toPx()), size = Size(w - 8.dp.toPx(), h - 10.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
                        drawRoundRect(color = GoldAccent.copy(alpha = 0.65f), topLeft = Offset(6.dp.toPx(), 8.dp.toPx()), size = Size(w - 12.dp.toPx(), h - 14.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()), style = Stroke(width = 1.2.dp.toPx()))
                    } else { drawOval(brush = Brush.radialGradient(colors = listOf(SnowWhite, Color(0xFFC7D7E8))), topLeft = Offset(4.dp.toPx(), 6.dp.toPx()), size = Size(w - 8.dp.toPx(), h - 10.dp.toPx())) }
                }
                Text(text = catEmoji, fontSize = if (isAwake) 36.sp else 32.sp, modifier = Modifier.scale(if (isAwake) 1.15f else breathScale).padding(bottom = 6.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 2.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = if (canUpgradeBowl) GoldAccent else NightSurfaceVariant.copy(alpha = 0.95f), border = androidx.compose.foundation.BorderStroke(1.dp, if (canUpgradeBowl) GoldAccentDark else GoldAccent.copy(alpha = 0.35f)), shadowElevation = 3.dp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onUpgradeBowl() }.testTag("upgrade_bowl_button")) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🥣+", fontSize = 11.sp); Spacer(modifier = Modifier.width(3.dp)); Text(text = "30 ❄️", color = if (canUpgradeBowl) Color(0xFF1C1200) else SnowWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onBowlClick() }.testTag("cat_bowl"), contentAlignment = Alignment.Center) {
                    if (bowlLevel > 0) { Canvas(modifier = Modifier.size(28.dp, 28.dp)) { drawCircle(brush = Brush.radialGradient(colors = listOf(GoldAccent.copy(alpha = 0.65f), GoldAccent.copy(alpha = 0.20f), Color.Transparent))) } }
                    Text(text = if (bowlLevel == 0) "🥣" else "🥣✨", fontSize = if (bowlLevel == 0) 22.sp else 24.sp)
                }
                Canvas(modifier = Modifier.size(26.dp, 6.dp)) { drawOval(color = if (bowlLevel > 0) GoldAccent.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.55f), size = Size(size.width, size.height)) }
            }
        }
    }
}

@Composable
fun InteractiveSnowman(plusOneTrigger: Int, onSnowmanClick: () -> Unit, clickPower: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "SnowmanWiggle")
    val snowmanBounce by transition.animateFloat(initialValue = 0f, targetValue = -3f, animationSpec = infiniteRepeatable(animation = tween(1400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "SnowmanBounce")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) { FloatingClickParticle(trigger = plusOneTrigger, text = "+$clickPower ❄️", color = IceCyan) }
        Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSnowmanClick() }.testTag("interactive_snowman"), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(80.dp, 28.dp).offset(y = 26.dp)) {
                val w = size.width; val h = size.height
                drawOval(brush = Brush.radialGradient(colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.20f), Color.Transparent), radius = w / 2f), size = Size(w, h))
                drawOval(brush = Brush.radialGradient(colors = listOf(SnowWhite, Color(0xFFB5CCE3))), topLeft = Offset(6.dp.toPx(), 4.dp.toPx()), size = Size(w - 12.dp.toPx(), h - 8.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = snowmanBounce.dp)) {
                Text(text = "⛄", fontSize = 42.sp, modifier = Modifier.padding(bottom = 2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(NightSurface.copy(alpha = 0.88f)).border(1.dp, IceCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                    Text(text = "Снеговик (+$clickPower ❄️)", color = IceCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WardrobeDialog(snowflakes: Long, currentOutfit: MaidenOutfit, hasWinterCoat: Boolean, onSelectDefault: () -> Unit, onBuyOrEquipCoat: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth(0.90f).padding(8.dp).testTag("wardrobe_dialog")) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(text = "👗", fontSize = 18.sp); Spacer(modifier = Modifier.width(6.dp)); Text(text = "Гардероб Снегурочки", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(imageVector = Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val isDefaultEquipped = currentOutfit == MaidenOutfit.DEFAULT
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(NightSurfaceVariant).border(1.5.dp, if (isDefaultEquipped) GoldAccent else Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "👧", fontSize = 34.sp); Spacer(modifier = Modifier.height(6.dp)); Text(text = "Обычный наряд", color = SnowWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(4.dp)); Text(text = "Лёгкое платьице", color = SnowSoft.copy(alpha = 0.7f), fontSize = 9.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(8.dp))
                        if (isDefaultEquipped) { Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF52B788).copy(alpha = 0.2f), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF52B788))) { Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF52B788), modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(3.dp)); Text(text = "Надет", color = Color(0xFF52B788), fontSize = 10.sp, fontWeight = FontWeight.Bold) } } }
                        else { Button(onClick = onSelectDefault, colors = ButtonDefaults.buttonColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(28.dp)) { Text(text = "Надеть", fontSize = 10.sp, color = IceCyan) } }
                    }
                    val isCoatEquipped = currentOutfit == MaidenOutfit.WINTER_COAT
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(NightSurfaceVariant).border(1.5.dp, if (isCoatEquipped) GoldAccent else if (hasWinterCoat) IceCyan else Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "👩‍🎤", fontSize = 34.sp); Spacer(modifier = Modifier.height(6.dp)); Text(text = "Зимняя шуба", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(4.dp)); Text(text = "+2 ❄️/сек дохода", color = Color(0xFF52B788), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(8.dp))
                        if (isCoatEquipped) { Surface(shape = RoundedCornerShape(8.dp), color = GoldAccent.copy(alpha = 0.2f), border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)) { Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Check, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(3.dp)); Text(text = "Надета", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } }
                        else if (hasWinterCoat) { Button(onClick = onBuyOrEquipCoat, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(28.dp)) { Text(text = "Надеть", fontSize = 10.sp, color = Color(0xFF1C1200), fontWeight = FontWeight.Bold) } }
                        else { val canAfford = snowflakes >= 100; Button(onClick = onBuyOrEquipCoat, colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) GoldAccent else Color(0xFF332026)), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(28.dp).testTag("buy_winter_coat_button")) { Text(text = "100 ❄️", fontSize = 10.sp, color = if (canAfford) Color(0xFF1C1200) else SnowSoft.copy(alpha = 0.6f), fontWeight = FontWeight.Bold) } }
                    }
                }
            }
        }
    }
}

@Composable
fun SnowflakeCounterTop(snowflakes: Long, incomeRate: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.shadow(10.dp, RoundedCornerShape(22.dp)).testTag("snowflake_counter"), shape = RoundedCornerShape(22.dp), color = Color(0xFF141A33).copy(alpha = 0.94f), border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.65f))) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = "❄️", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp)); Text(text = formatNumber(snowflakes), color = GoldAccent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp); Spacer(modifier = Modifier.width(10.dp)); Text(text = if (incomeRate > 0) "+$incomeRate/сек" else "0/сек", color = if (incomeRate > 0) Color(0xFF52B788) else SnowSoft.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ShopDialog(snowflakes: Long, clickPower: Int, treeLevel: Int, onDismiss: () -> Unit, onBuyItem: (itemIndex: Int, cost: Long) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth(0.94f).padding(8.dp).testTag("shop_dialog")) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Новогодняя Лавка 🛍️", color = GoldAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(10.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = NightSurfaceVariant, border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))) { Text(text = "Баланс: ${formatNumber(snowflakes)} ❄️", color = GoldAccentLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp).testTag("shop_close_button")) { Icon(imageVector = Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
                }
                Spacer(modifier = Modifier.height(14.dp))
                
                val costTree = calculateUpgradeCost(50L, treeLevel)
                val costClick = calculateUpgradeCost(20L, clickPower)
                
                val items = listOf(
                    Triple("Корм для кота", "🐟", 15L) to "+30% к сытости котика",
                    Triple("Золотая звезда", "⭐", costTree) to "Ур. ёлки +1, доход +1 ❄️/с",
                    Triple("Ледяная гирлянда", "💡", 100L) to "Волшебное сияние огней",
                    Triple("Сила клика", "👆", costClick) to "+1 к снежинкам за клик"
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEachIndexed { index, (itemData, description) ->
                        val (name, emoji, cost) = itemData
                        val canAfford = snowflakes >= cost
                        Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(NightSurfaceVariant).border(1.2.dp, if (canAfford) GoldAccent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).padding(8.dp).testTag("shop_item_$index"), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = emoji, fontSize = 24.sp); Spacer(modifier = Modifier.height(4.dp)); Text(text = name, color = SnowWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1); Spacer(modifier = Modifier.height(2.dp)); Text(text = description, color = SnowSoft.copy(alpha = 0.75f), fontSize = 8.sp, textAlign = TextAlign.Center, minLines = 2, maxLines = 2); Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onBuyItem(index, cost) }, colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) GoldAccent else Color(0xFF332947)), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), modifier = Modifier.fillMaxWidth().height(34.dp).testTag("shop_buy_$index")) { Text(text = "${formatNumber(cost)} ❄️", color = if (canAfford) Color(0xFF1C1200) else SnowSoft.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedCatDialog(snowflakes: Long, currentSatiety: Int, onDismiss: () -> Unit, onConfirmFeed: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth(0.78f).padding(10.dp).testTag("feed_cat_dialog")) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🐟", fontSize = 34.sp); Spacer(modifier = Modifier.height(6.dp)); Text(text = "Покормить кота за 15 ❄️?", color = SnowWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(4.dp)); Text(text = "Сытость восстановится на +30%\n(текущая: $currentSatiety%)", color = SnowSoft.copy(alpha = 0.8f), fontSize = 11.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = NightSurfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(36.dp).testTag("feed_cat_cancel")) { Text(text = "Отмена", color = SnowSoft, fontSize = 11.sp) }
                    Button(onClick = onConfirmFeed, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(36.dp).testTag("feed_cat_confirm")) { Text(text = "Да, 15 ❄️", color = Color(0xFF1C1200), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}

@Composable
fun DailyRewardButton(isClaimedToday: Boolean, onClaimReward: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "DailyGiftGlow")
    val giftGlowScale by transition.animateFloat(initialValue = 1.0f, targetValue = 1.08f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "GiftScale")
    Surface(shape = RoundedCornerShape(16.dp), color = if (!isClaimedToday) HolidayRed.copy(alpha = 0.95f) else NightSurfaceVariant.copy(alpha = 0.85f), border = androidx.compose.foundation.BorderStroke(1.2.dp, if (!isClaimedToday) GoldAccent else SnowSoft.copy(alpha = 0.3f)), shadowElevation = if (!isClaimedToday) 8.dp else 2.dp, modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable { onClaimReward() }.testTag("daily_reward_button")) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🎁", fontSize = 16.sp, modifier = Modifier.scale(if (!isClaimedToday) giftGlowScale else 1.0f)); Spacer(modifier = Modifier.width(6.dp))
            Column { Text(text = if (!isClaimedToday) "Ежедневный подарок" else "Подарок получен", color = if (!isClaimedToday) SnowWhite else SnowSoft.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(text = if (!isClaimedToday) "+100 ❄️" else "Завтра в 00:00", color = if (!isClaimedToday) GoldAccent else SnowSoft.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun GameToast(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = message != null, enter = fadeIn() + scaleIn(initialScale = 0.85f) + slideInVertically { it / 2 }, exit = fadeOut() + scaleOut() + slideOutVertically { it / 2 }, modifier = modifier) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1B1B38).copy(alpha = 0.96f), border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.85f)), shadowElevation = 10.dp) {
            Row(modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Text(text = message ?: "", color = SnowWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
fun RoomInteriorScenery() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val floorY = h * 0.74f
        val winX = w * 0.08f; val winY = h * 0.16f; val winW = w * 0.26f; val winH = h * 0.48f
        drawRoundRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF060F24), Color(0xFF0D1B38), Color(0xFF192A4E)), startY = winY, endY = winY + winH), topLeft = Offset(winX, winY), size = Size(winW, winH), cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()))
        val hillInWin = Path().apply { moveTo(winX, winY + winH * 0.75f); quadraticTo(winX + winW * 0.45f, winY + winH * 0.60f, winX + winW, winY + winH * 0.70f); lineTo(winX + winW, winY + winH); lineTo(winX, winY + winH); close() }
        drawPath(path = hillInWin, color = Color(0xFF283A5E))
        for (treeIdx in 0..2) { val tx = winX + winW * (0.22f + treeIdx * 0.28f); val ty = winY + winH * 0.64f; val tH = 14.dp.toPx(); val tW = 10.dp.toPx(); val pineOutPath = Path().apply { moveTo(tx, ty - tH); lineTo(tx + tW / 2f, ty); lineTo(tx - tW / 2f, ty); close() }; drawPath(path = pineOutPath, color = Color(0xFF152238)) }
        drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFFFF7DC), Color(0xFFFFF7DC).copy(alpha = 0.4f), Color.Transparent), center = Offset(winX + winW * 0.78f, winY + winH * 0.25f), radius = 16.dp.toPx()), radius = 16.dp.toPx(), center = Offset(winX + winW * 0.78f, winY + winH * 0.25f))
        drawCircle(color = Color(0xFFFFF9E6), radius = 9.dp.toPx(), center = Offset(winX + winW * 0.78f, winY + winH * 0.25f))
        drawRoundRect(color = Color(0xFF4A2511), topLeft = Offset(winX - 6.dp.toPx(), winY - 6.dp.toPx()), size = Size(winW + 12.dp.toPx(), winH + 12.dp.toPx()), cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()), style = Stroke(width = 6.dp.toPx()))
        drawRoundRect(color = Color(0xFF5C2E15), topLeft = Offset(winX - 10.dp.toPx(), winY + winH - 2.dp.toPx()), size = Size(winW + 20.dp.toPx(), 10.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
        drawLine(color = Color(0xFF4A2511), start = Offset(winX + winW / 2f, winY), end = Offset(winX + winW / 2f, winY + winH), strokeWidth = 4.dp.toPx())
        drawLine(color = Color(0xFF4A2511), start = Offset(winX, winY + winH * 0.45f), end = Offset(winX + winW, winY + winH * 0.45f), strokeWidth = 4.dp.toPx())
        drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF381C0D), Color(0xFF261208), Color(0xFF190B04)), startY = floorY, endY = h), topLeft = Offset(0f, floorY), size = Size(w, h - floorY))
        drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF4A2612), Color(0xFF2E170A))), topLeft = Offset(0f, floorY - 6.dp.toPx()), size = Size(w, 8.dp.toPx()))
        drawLine(color = GoldAccentDark.copy(alpha = 0.3f), start = Offset(0f, floorY - 6.dp.toPx()), end = Offset(w, floorY - 6.dp.toPx()), strokeWidth = 2f)
        val plankCount = 7
        for (i in 0..plankCount) { val plankX = (i.toFloat() / plankCount) * w; drawLine(color = Color.Black.copy(alpha = 0.35f), start = Offset(plankX, floorY), end = Offset(plankX, h), strokeWidth = 1.5f) }
    }
}

@Composable
fun StreetExteriorScenery() {
    val transition = rememberInfiniteTransition(label = "StreetAnimation")
    val lanternGlow by transition.animateFloat(initialValue = 0.85f, targetValue = 1.0f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "LanternGlow")
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val slidePath = Path().apply { moveTo(w * 0.52f, h * 0.72f); quadraticTo(w * 0.72f, h * 0.65f, w * 0.84f, h * 0.36f); lineTo(w, h * 0.36f); lineTo(w, h * 0.76f); close() }
        drawPath(path = slidePath, brush = Brush.verticalGradient(colors = listOf(Color(0xFF8AAECB), Color(0xFF3B577D), Color(0xFF1E2D45)), startY = h * 0.36f, endY = h * 0.76f))
        val iceTrack = Path().apply { moveTo(w * 0.52f, h * 0.71f); quadraticTo(w * 0.72f, h * 0.64f, w * 0.84f, h * 0.36f) }
        drawPath(path = iceTrack, color = IceCyan.copy(alpha = 0.85f), style = Stroke(width = 5.dp.toPx()))
        drawPath(path = iceTrack, color = Color.White.copy(alpha = 0.9f), style = Stroke(width = 2.dp.toPx()))
        val kid1X = w * 0.85f; val kid1Y = h * 0.33f
        drawCircle(color = Color(0xFF0F1A2C), radius = 4.dp.toPx(), center = Offset(kid1X, kid1Y)); drawRect(color = Color(0xFF0F1A2C), topLeft = Offset(kid1X - 3.dp.toPx(), kid1Y + 4.dp.toPx()), size = Size(6.dp.toPx(), 9.dp.toPx())); drawLine(color = GoldAccentDark, start = Offset(kid1X - 5.dp.toPx(), kid1Y + 13.dp.toPx()), end = Offset(kid1X + 7.dp.toPx(), kid1Y + 13.dp.toPx()), strokeWidth = 2.dp.toPx())
        val kid2X = w * 0.70f; val kid2Y = h * 0.54f
        drawCircle(color = Color(0xFF0F1A2C), radius = 4.dp.toPx(), center = Offset(kid2X, kid2Y)); drawRect(color = Color(0xFF0F1A2C), topLeft = Offset(kid2X - 3.dp.toPx(), kid2Y + 4.dp.toPx()), size = Size(6.dp.toPx(), 8.dp.toPx())); drawLine(color = HolidayRed, start = Offset(kid2X - 6.dp.toPx(), kid2Y + 12.dp.toPx()), end = Offset(kid2X + 6.dp.toPx(), kid2Y + 12.dp.toPx()), strokeWidth = 2.dp.toPx())
        val kid3X = w * 0.56f; val kid3Y = h * 0.69f
        drawCircle(color = Color(0xFF0F1A2C), radius = 3.5.dp.toPx(), center = Offset(kid3X, kid3Y)); drawRect(color = Color(0xFF0F1A2C), topLeft = Offset(kid3X - 2.5.dp.toPx(), kid3Y + 3.5.dp.toPx()), size = Size(5.dp.toPx(), 7.dp.toPx()))
        val lampX = w * 0.20f; val lampBaseY = h * 0.76f; val lampTopY = h * 0.28f
        drawCircle(brush = Brush.radialGradient(colors = listOf(WarmGlow.copy(alpha = 0.55f * lanternGlow), WarmGlow.copy(alpha = 0.25f * lanternGlow), Color.Transparent), center = Offset(lampX, lampTopY + 14.dp.toPx()), radius = 85.dp.toPx()), radius = 85.dp.toPx(), center = Offset(lampX, lampTopY + 14.dp.toPx()))
        drawLine(color = Color(0xFF141923), start = Offset(lampX, lampBaseY), end = Offset(lampX, lampTopY), strokeWidth = 5.dp.toPx())
        drawRect(color = Color(0xFF141923), topLeft = Offset(lampX - 7.dp.toPx(), lampBaseY - 6.dp.toPx()), size = Size(14.dp.toPx(), 8.dp.toPx()))
        drawLine(color = Color(0xFF141923), start = Offset(lampX - 10.dp.toPx(), lampTopY + 8.dp.toPx()), end = Offset(lampX + 10.dp.toPx(), lampTopY + 8.dp.toPx()), strokeWidth = 3.dp.toPx())
        val lampHeadPath = Path().apply { moveTo(lampX - 9.dp.toPx(), lampTopY + 8.dp.toPx()); lineTo(lampX + 9.dp.toPx(), lampTopY + 8.dp.toPx()); lineTo(lampX + 6.dp.toPx(), lampTopY + 22.dp.toPx()); lineTo(lampX - 6.dp.toPx(), lampTopY + 22.dp.toPx()); close() }
        drawPath(path = lampHeadPath, color = Color(0xFF141923), style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = GoldAccent, radius = 5.5.dp.toPx() * lanternGlow, center = Offset(lampX, lampTopY + 15.dp.toPx()))
        drawArc(color = SnowWhite, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(lampX - 10.dp.toPx(), lampTopY + 2.dp.toPx()), size = Size(20.dp.toPx(), 8.dp.toPx()))
        val snowDrifts = Path().apply { moveTo(0f, h * 0.74f); quadraticTo(w * 0.25f, h * 0.70f, w * 0.5f, h * 0.75f); quadraticTo(w * 0.75f, h * 0.80f, w, h * 0.73f); lineTo(w, h); lineTo(0f, h); close() }
        drawPath(path = snowDrifts, brush = Brush.verticalGradient(colors = listOf(Color(0xFFE2EAF5), Color(0xFF90A8C5), Color(0xFF556F91)), startY = h * 0.70f, endY = h))
    }
}

@Composable
fun SnowfallLayer(isForeground: Boolean, intensity: Float = 1.0f, modifier: Modifier = Modifier) {
    val duration = if (isForeground) 5500 else 11000
    val transition = rememberInfiniteTransition(label = "SnowLayer_${if (isForeground) "FG" else "BG"}")
    val cycleProgress by transition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(durationMillis = duration, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "SnowFallingCycle")
    val flakes = remember(isForeground) {
        val count = if (isForeground) 28 else 55
        List(count) { if (isForeground) { Snowflake(xNorm = Random.nextFloat(), yOffsetNorm = Random.nextFloat(), speed = 0.8f + Random.nextFloat() * 0.7f, radius = 3.2f + Random.nextFloat() * 3.8f, swayFrequency = 1.2f + Random.nextFloat() * 2.0f, swayAmplitude = 12f + Random.nextFloat() * 18f, alpha = 0.65f + Random.nextFloat() * 0.35f) } else { Snowflake(xNorm = Random.nextFloat(), yOffsetNorm = Random.nextFloat(), speed = 0.25f + Random.nextFloat() * 0.45f, radius = 1.0f + Random.nextFloat() * 1.8f, swayFrequency = 0.8f + Random.nextFloat() * 1.5f, swayAmplitude = 5f + Random.nextFloat() * 10f, alpha = 0.25f + Random.nextFloat() * 0.35f) } }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val activeCount = (flakes.size * intensity.coerceIn(0.2f, 2.0f)).toInt().coerceIn(6, flakes.size)
        for (i in 0 until activeCount) {
            val flake = flakes[i]; val currentY = ((flake.yOffsetNorm + cycleProgress * flake.speed) % 1f) * h; val sway = sin((cycleProgress * 2 * PI.toFloat() * flake.swayFrequency) + flake.xNorm * 10f) * flake.swayAmplitude; val currentX = (flake.xNorm * w + sway).mod(w)
            drawCircle(color = Color.White.copy(alpha = flake.alpha), radius = flake.radius, center = Offset(currentX, currentY))
        }
    }
}
private data class Snowflake(val xNorm: Float, val yOffsetNorm: Float, val speed: Float, val radius: Float, val swayFrequency: Float, val swayAmplitude: Float, val alpha: Float)

@Composable
fun GameBottomNavigationBar(currentTab: GameTab, onTabSelected: (GameTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp).shadow(12.dp, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), color = NightSurface.copy(alpha = 0.94f), border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.wrapContentWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            GameTab.values().forEach { tab ->
                val isSelected = currentTab == tab; val interactionSource = remember { MutableInteractionSource() }
                val testTagId = when (tab) { GameTab.ROOM -> "nav_room"; GameTab.STREET -> "nav_street" }
                Box(modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (isSelected) GoldAccent.copy(alpha = 0.22f) else Color.Transparent).border(width = if (isSelected) 1.dp else 0.dp, color = if (isSelected) GoldAccent.copy(alpha = 0.7f) else Color.Transparent, shape = RoundedCornerShape(18.dp)).clickable(interactionSource = interactionSource, indication = null) { onTabSelected(tab) }.padding(horizontal = 24.dp, vertical = 8.dp).testTag(testTagId), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(imageVector = tab.icon, contentDescription = tab.label, tint = if (isSelected) GoldAccent else SnowSoft.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = tab.label, color = if (isSelected) GoldAccent else SnowSoft.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(snowIntensity: Float, onIntensityChange: (Float) -> Unit, isMusicEnabled: Boolean, onMusicToggle: (Boolean) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth(0.85f).padding(12.dp).testTag("settings_dialog")) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Настройки Игры ⚙️", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(imageVector = Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Интенсивность снегопада: ${(snowIntensity * 100).toInt()}%", color = SnowWhite, fontSize = 13.sp)
                Slider(value = snowIntensity, onValueChange = onIntensityChange, valueRange = 0.5f..2.0f, colors = SliderDefaults.colors(thumbColor = IceCyan, activeTrackColor = IceCyan, inactiveTrackColor = NightSurfaceVariant))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Уютные звуки и музыка", color = SnowWhite, fontSize = 13.sp)
                    Switch(checked = isMusicEnabled, onCheckedChange = onMusicToggle, colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = GoldAccentDark))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Версия 1.8 • Улучшенная экономика 💾✨", color = SnowSoft.copy(alpha = 0.6f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
