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

// ФУНКЦИЯ ДЛЯ КРАСИВОГО ОТОБРАЖЕНИЯ БОЛЬШИХ ЧИСЕЛ (МАТЕМАТИКА)
fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> String.format("%.1fB", number / 1_000_000_000.0)
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 10_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

// ФУНКЦИЯ РАСЧЕТА ЦЕНЫ УЛУЧШЕНИЯ (ЭКСПОНЕНЦИАЛЬНЫЙ РОСТ)
fun calculateUpgradeCost(baseCost: Long, level: Int): Long {
    return (baseCost * 1.5.pow(level)).toLong()
}

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

enum class GameTab(val label: String, val icon: ImageVector) {
    ROOM("Комната", Icons.Filled.Home),
    STREET("Улица", Icons.Filled.AcUnit)
}

enum class MaidenOutfit(val title: String, val emoji: String) {
    DEFAULT("Обычный наряд", "👧"),
    WINTER_COAT("Зимняя шуба", "👩‍🎤")
}

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

        // КНОПКА УЛУЧШЕНИЯ ЕЛКИ (С МАТЕМАТИКОЙ ЦЕНЫ)
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (isTablet) 115.dp else 102.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1C1335).copy(alpha = 0.94f), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.7f)), shadowElevation = 4.dp, modifier = Modifier.padding(end = 6.dp)) {
                Text(text = if (treeLevel == 0) "Ур. 0" else "Ур. $treeLevel", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }

            val upgradeCost = calculateUpgradeCost(50L, treeLevel) // МАТЕМАТИКА
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

        // ВЕРХНЯЯ ПАНЕЛЬ
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { isWardrobeOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, GoldAccent.copy(alpha = 0.5f), CircleShape)) { Text(text = "👗", fontSize = 16.sp) }
            IconButton(onClick = { isShopOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, GoldAccent.copy(alpha = 0.5f), CircleShape)) { Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = "Магазин", tint = GoldAccent, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { isSettingsOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, IceCyan.copy(alpha = 0.3f), CircleShape)) { Icon(imageVector = Icons.Filled.Settings, contentDescription = "Настройки", tint = GoldAccent, modifier = Modifier.size(18.dp)) }
        }

        GameToast(message = toastMessage, modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp))

        // НИЖНЯЯ ПАНЕЛЬ (КОМПАКТНАЯ)
        GameBottomNavigationBar(currentTab = currentTab, onTabSelected = { tab -> currentTab = tab }, modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars))

        // ДИАЛОГ МАГАЗИНА (С МАТЕМАТИКОЙ)
        if (isShopOpen) {
            ShopDialog(snowflakes = snowflakes, clickPower = clickPower, onDismiss = { isShopOpen = false }, onBuyItem = { itemIndex, cost ->
                when (itemIndex) {
                    0 -> if (snowflakes >= 15) { snowflakes -= 15; catSatiety = (catSatiety + 30).coerceAtMost(100); persistGameState(); showToast("Мур! Кот покормлен 🐟") } else showToast("Мало снежинок!")
                    1 -> {
                        val costTree = calculateUpgradeCost(50L, treeLevel)
                        if (snowflakes >= costTree) { snowflakes -= costTree; treeLevel += 1; incomeRate += 1; persistGameState(); showToast("Звезда установлена! ⭐") } else showToast("Мало снежинок!")
                    }
                    2 -> if (snowflakes >= 100) { snowflakes -= 100; lightsMode = 1; persistGameState(); showToast("Гирлянда установлена! 💡✨") } else showToast("Мало снежинок!")
                    3 -> { // Прокачка силы клика
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

// ... (Остальные функции можно оставить как были, они не меняются) ...
// ВАЖНО: Скопируй остальные функции (HolidayHostsBackground, ChristmasTree25D, InteractiveCat и т.д.) из своего текущего файла, 
// и замени в них только отображение чисел на formatNumber(), а также передай clickPower в InteractiveCat и InteractiveSnowman.
