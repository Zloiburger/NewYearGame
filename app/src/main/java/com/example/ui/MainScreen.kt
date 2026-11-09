package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> String.format("%.1fB", number / 1_000_000_000.0)
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 10_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

fun calculateStreetTreeCost(level: Int): Long = (50.0 * Math.pow(1.15, level.toDouble())).toLong()
fun calculateRoomTreeCost(level: Int): Long = (30.0 * Math.pow(1.12, level.toDouble())).toLong()

enum class CurrencyType { SNOWFLAKES, DONATE }

data class Outfit(
    val id: Int, val title: String, val emoji: String, val currency: CurrencyType, 
    val price: Long, val incomeBonus: Int, val isDefault: Boolean = false,
    @DrawableRes val imageRes: Int? = null
)

val ALL_OUTFITS = listOf(
    Outfit(0, "Обычный наряд", "👧", CurrencyType.SNOWFLAKES, 0L, 0, isDefault = true),
    Outfit(1, "Праздничное платье", "🎀", CurrencyType.SNOWFLAKES, 10000L, 15),
    Outfit(2, "Снежный свитер", "🧥", CurrencyType.SNOWFLAKES, 500L, 2),
    Outfit(3, "Зимняя шуба", "👩‍🎤", CurrencyType.SNOWFLAKES, 2500L, 8),
    Outfit(4, "Ледяное платье", "👗", CurrencyType.SNOWFLAKES, 8000L, 20),
    Outfit(5, "Шапка-ушанка", "🧣", CurrencyType.SNOWFLAKES, 20000L, 45),
    Outfit(6, "Серебряный пуховик", "✨", CurrencyType.SNOWFLAKES, 50000L, 90),
    Outfit(7, "Платье Метелицы", "🌪️", CurrencyType.SNOWFLAKES, 120000L, 200),
    Outfit(8, "Костюм Звездочета", "🔭", CurrencyType.SNOWFLAKES, 300000L, 450),
    Outfit(9, "Мантия Вьюги", "❄️", CurrencyType.SNOWFLAKES, 750000L, 1000),
    Outfit(10, "Королевская мантия", "👑", CurrencyType.DONATE, 99L, 2500),
    Outfit(11, "Сияние севера", "🌌", CurrencyType.DONATE, 149L, 4000),
    Outfit(12, "Изумрудный наряд", "💚", CurrencyType.DONATE, 199L, 6000),
    Outfit(13, "Алый костюм", "❤️", CurrencyType.DONATE, 299L, 10000),
    Outfit(14, "Костюм Золотой Зимы", "🏆", CurrencyType.DONATE, 399L, 18000),
    Outfit(15, "Мантия Снежной Королевы", "🧊", CurrencyType.DONATE, 499L, 30000),
    Outfit(16, "Неоновый костюм", "💡", CurrencyType.DONATE, 599L, 50000),
    Outfit(17, "Космический скафандр", "🚀", CurrencyType.DONATE, 799L, 85000),
    Outfit(18, "Костюм Дракона", "🐉", CurrencyType.DONATE, 999L, 150000),
    Outfit(19, "Легенда Нового Года", "🔥", CurrencyType.DONATE, 1499L, 300000)
)

data class ShopItem(val name: String, val emoji: String, val cost: Long, val description: String)

data class RoomBackground(val id: Int, val title: String, val price: Long, val wallColor: Color, @DrawableRes val imageRes: Int? = null)

val ALL_BACKGROUNDS = listOf(
    RoomBackground(0, "Уютный вечер", 0L, Color(0xFF2B1A14)),
    RoomBackground(1, "Зимний лес", 5000L, Color(0xFF1E3A5F)),
    RoomBackground(2, "Красная изба", 15000L, Color(0xFF5E1914)),
    RoomBackground(3, "Тёмный чердак", 40000L, Color(0xFF1A1A1A))
)

enum class GameTab(val label: String, val icon: ImageVector) {
    ROOM("Комната", Icons.Filled.Home),
    STREET("Улица", Icons.Filled.AcUnit)
}

@Composable
fun EmojiOrImage(emoji: String, imageRes: Int? = null, modifier: Modifier = Modifier, fontSize: androidx.compose.ui.unit.TextUnit = 28.sp) {
    if (imageRes != null) {
        Image(painter = painterResource(id = imageRes), contentDescription = null, modifier = modifier)
    } else {
        Text(text = emoji, fontSize = fontSize, modifier = modifier)
    }
}

data class GameSaveData(
    val snowflakes: Long, val streetTreeLevel: Int, val roomTreeLevel: Int, val catSatiety: Int,
    val lastUpdateTime: Long, val lastDailyRewardDay: String, val bowlLevel: Int, val clickPower: Int,
    val purchasedOutfitIds: Set<Int>, val currentOutfitId: Int, val donateBalance: Long,
    val isMusicEnabled: Boolean, val isSfxEnabled: Boolean, val isVibrationEnabled: Boolean,
    val musicVolume: Float, val sfxVolume: Float, val snowIntensity: Float,
    val currentBackgroundId: Int, val purchasedBackgroundIds: Set<Int>
)

object GameStorage {
    private const val PREFS_NAME = "cozy_new_year_prefs"
    fun load(context: Context): GameSaveData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return GameSaveData(
            prefs.getLong("snowflakes", 0L), prefs.getInt("street_tree_level", 0), prefs.getInt("room_tree_level", 0),
            prefs.getInt("cat_satiety", 100), prefs.getLong("last_update_time", 0L),
            prefs.getString("last_daily_reward_day", "") ?: "", prefs.getInt("bowl_level", 0),
            prefs.getInt("click_power", 1), prefs.getStringSet("purchased_outfits", setOf("0"))?.map { it.toInt() }?.toSet() ?: setOf(0),
            prefs.getInt("current_outfit_id", 0), prefs.getLong("donate_balance", 0L),
            isMusicEnabled = prefs.getBoolean("is_music_enabled", true),
            isSfxEnabled = prefs.getBoolean("is_sfx_enabled", true),
            isVibrationEnabled = prefs.getBoolean("is_vibration_enabled", true),
            musicVolume = prefs.getFloat("music_volume", 0.5f),
            sfxVolume = prefs.getFloat("sfx_volume", 0.8f),
            snowIntensity = prefs.getFloat("snow_intensity", 1.0f),
            currentBackgroundId = prefs.getInt("current_background_id", 0),
            purchasedBackgroundIds = prefs.getStringSet("purchased_backgrounds", setOf("0"))?.map { it.toInt() }?.toSet() ?: setOf(0)
        )
    }
    fun save(context: Context, data: GameSaveData) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong("snowflakes", data.snowflakes).putInt("street_tree_level", data.streetTreeLevel)
            .putInt("room_tree_level", data.roomTreeLevel).putInt("cat_satiety", data.catSatiety)
            .putLong("last_update_time", data.lastUpdateTime).putString("last_daily_reward_day", data.lastDailyRewardDay)
            .putInt("bowl_level", data.bowlLevel).putInt("click_power", data.clickPower)
            .putStringSet("purchased_outfits", data.purchasedOutfitIds.map { it.toString() }.toSet())
            .putInt("current_outfit_id", data.currentOutfitId).putLong("donate_balance", data.donateBalance)
            .putBoolean("is_music_enabled", data.isMusicEnabled).putBoolean("is_sfx_enabled", data.isSfxEnabled)
            .putBoolean("is_vibration_enabled", data.isVibrationEnabled).putFloat("music_volume", data.musicVolume)
            .putFloat("sfx_volume", data.sfxVolume).putFloat("snow_intensity", data.snowIntensity)
            .putInt("current_background_id", data.currentBackgroundId)
            .putStringSet("purchased_backgrounds", data.purchasedBackgroundIds.map { it.toString() }.toSet())
            .apply()
    }
}

@Composable
fun MainScreen(onExitToMenu: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedState = remember { GameStorage.load(context) }
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    val snowflakesState = remember { mutableLongStateOf(savedState.snowflakes) }
    var donateBalance by remember { mutableLongStateOf(savedState.donateBalance) }
    var streetTreeLevel by remember { mutableIntStateOf(savedState.streetTreeLevel) }
    var roomTreeLevel by remember { mutableIntStateOf(savedState.roomTreeLevel) }
    var catSatiety by remember { mutableIntStateOf(savedState.catSatiety) }
    var bowlLevel by remember { mutableIntStateOf(savedState.bowlLevel) }
    var clickPower by remember { mutableIntStateOf(savedState.clickPower) }
    var lastDailyRewardDay by remember { mutableStateOf(savedState.lastDailyRewardDay) }
    var purchasedOutfits by remember { mutableStateOf(savedState.purchasedOutfitIds) }
    var currentOutfitId by remember { mutableIntStateOf(savedState.currentOutfitId) }
    var currentBackgroundId by remember { mutableIntStateOf(savedState.currentBackgroundId) }
    var purchasedBackgrounds by remember { mutableStateOf(savedState.purchasedBackgroundIds) }

    var isMusicEnabled by remember { mutableStateOf(savedState.isMusicEnabled) }
    var isSfxEnabled by remember { mutableStateOf(savedState.isSfxEnabled) }
    var isVibrationEnabled by remember { mutableStateOf(savedState.isVibrationEnabled) }
    var musicVolume by remember { mutableFloatStateOf(savedState.musicVolume) }
    var sfxVolume by remember { mutableFloatStateOf(savedState.sfxVolume) }
    var snowIntensity by remember { mutableFloatStateOf(savedState.snowIntensity) }

    val outfitsIncome = purchasedOutfits.mapNotNull { id -> ALL_OUTFITS.find { it.id == id } }.sumOf { it.incomeBonus }
    val baseIncome = roomTreeLevel + streetTreeLevel + bowlLevel
    val effectiveIncomeRate = baseIncome + outfitsIncome

    var currentDateStr by remember { mutableStateOf(getTodayDateStr()) }

    var isWardrobeOpen by remember { mutableStateOf(false) }
    var isShopOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isFeedCatDialogOpen by remember { mutableStateOf(false) }
    var isTvOpen by remember { mutableStateOf(false) }
    var isBackgroundsOpen by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(GameTab.ROOM) }
    var lightsMode by remember { mutableIntStateOf(0) }
    
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val soundPool = remember { 
        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build() 
    }
    // ИСПРАВЛЕНИЕ: Звук покупки берем из файла fireplace (там лежит звук кассы)
    val cashSoundId = remember { soundPool.load(context, R.raw.fireplace, 1) }
    
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var fireplacePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val musicPlaylist = remember { listOf(R.raw.music1, R.raw.music2, R.raw.music3, R.raw.music4, R.raw.music5) }
    var currentSongIndex by remember { mutableIntStateOf(0) }

    fun vibrate() {
        if (isVibrationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }

    fun persistGameState() {
        GameStorage.save(context, GameSaveData(
            snowflakesState.longValue, streetTreeLevel, roomTreeLevel, catSatiety,
            System.currentTimeMillis(), lastDailyRewardDay, bowlLevel, clickPower,
            purchasedOutfits, currentOutfitId, donateBalance,
            isMusicEnabled, isSfxEnabled, isVibrationEnabled, musicVolume, sfxVolume, snowIntensity,
            currentBackgroundId, purchasedBackgrounds
        ))
    }

    LaunchedEffect(isMusicEnabled, currentSongIndex) {
        if (isMusicEnabled && musicPlaylist.isNotEmpty()) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, musicPlaylist[currentSongIndex])?.apply {
                setVolume(musicVolume, musicVolume)
                setOnCompletionListener { currentSongIndex = (currentSongIndex + 1) % musicPlaylist.size }
                start()
            }
        } else {
            mediaPlayer?.pause()
        }
    }

    LaunchedEffect(musicVolume) { mediaPlayer?.setVolume(musicVolume, musicVolume) }

    LaunchedEffect(currentTab, isSfxEnabled) {
        if (currentTab == GameTab.ROOM && isSfxEnabled) {
            if (fireplacePlayer == null) {
                // ИСПРАВЛЕНИЕ: Звук камина берем из файла cash_register (там лежит треск дров)
                fireplacePlayer = MediaPlayer.create(context, R.raw.cash_register)?.apply {
                    isLooping = true; setVolume(sfxVolume * 0.3f, sfxVolume * 0.3f); start()
                }
            } else { fireplacePlayer?.start() }
        } else { fireplacePlayer?.pause() }
    }

    LaunchedEffect(sfxVolume) { fireplacePlayer?.setVolume(sfxVolume * 0.3f, sfxVolume * 0.3f) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> 
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                persistGameState()
                mediaPlayer?.pause(); fireplacePlayer?.pause()
                soundPool.autoPause() // ФИКС ОТ АЛИСЫ
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (isMusicEnabled) mediaPlayer?.start()
                if (currentTab == GameTab.ROOM && isSfxEnabled) fireplacePlayer?.start()
                soundPool.autoResume() // ФИКС ОТ АЛИСЫ
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { 
            lifecycleOwner.lifecycle.removeObserver(observer)
            mediaPlayer?.stop(); mediaPlayer?.release()
            fireplacePlayer?.stop(); fireplacePlayer?.release()
            soundPool.release() 
        }
    }

    fun showToast(msg: String) { toastMessage = msg }
    LaunchedEffect(toastMessage) { if (toastMessage != null) { delay(3000); toastMessage = null } }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val lastTime = savedState.lastUpdateTime
        val offlineRate = savedState.roomTreeLevel + savedState.streetTreeLevel + savedState.bowlLevel + 
                          savedState.purchasedOutfitIds.mapNotNull { id -> ALL_OUTFITS.find { it.id == id } }.sumOf { it.incomeBonus }
        if (lastTime > 0L && offlineRate > 0) {
            val elapsedSeconds = ((now - lastTime) / 1000).coerceAtLeast(0)
            if (elapsedSeconds >= 60) {
                val cappedSeconds = minOf(elapsedSeconds, 24 * 3600)
                val offlineSnowflakes = (cappedSeconds * offlineRate).toLong()
                if (offlineSnowflakes > 0) { snowflakesState.longValue += offlineSnowflakes; showToast("Пока вас не было, накоплено ${formatNumber(offlineSnowflakes)} ❄️!") }
            }
        }
        persistGameState()
    }

    LaunchedEffect(Unit) {
        var tickCounter = 0
        while (true) {
            delay(1000); tickCounter++
            val currentIncome = roomTreeLevel + streetTreeLevel + bowlLevel + purchasedOutfits.mapNotNull { id -> ALL_OUTFITS.find { it.id == id } }.sumOf { it.incomeBonus }
            if (currentIncome > 0) { snowflakesState.longValue += currentIncome }

            if (tickCounter % 144 == 0 && catSatiety > 50) {
                catSatiety = (catSatiety - 1).coerceAtLeast(50)
            } else if (tickCounter % 576 == 0 && catSatiety <= 50 && catSatiety > 0) {
                catSatiety = (catSatiety - 1).coerceAtLeast(0)
            }
            
            if (tickCounter % 60 == 0) {
                val newDate = getTodayDateStr()
                if (newDate != currentDateStr) currentDateStr = newDate
            }
            if (tickCounter % 10 == 0) { persistGameState() }
        }
    }

    var isDayTime by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { delay(30 * 60 * 1000L); isDayTime = !isDayTime } }

    var isCatAwake by remember { mutableStateOf(false) }
    var catPlusOneTrigger by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var snowmanPlusOneTrigger by remember { mutableIntStateOf(0) }
    val isRoomTab = currentTab == GameTab.ROOM

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = if (isRoomTab) listOf(Color(0xFF240E14), Color(0xFF1D0B10), Color(0xFF17090D), Color(0xFF110609))
                else if (isDayTime) listOf(Color(0xFF4A90E2), Color(0xFFFFE4B5))
                else listOf(Color(0xFF060914), Color(0xFF0C182B))
            )
        )
    ) {
        val isTablet = maxWidth > 600.dp
        val tabletScale = if (isTablet) 1.4f else 1.0f
        val w = maxWidth
        val h = maxHeight

        if (isRoomTab) {
            val currentBg = ALL_BACKGROUNDS.find { it.id == currentBackgroundId } ?: ALL_BACKGROUNDS[0]
            RoomInteriorScenery(wallColor = currentBg.wallColor)
        } else {
            StreetExteriorScenery(isDay = isDayTime)
        }
        
        SnowfallLayer(isForeground = false, intensity = if (isRoomTab) snowIntensity * 0.40f else snowIntensity * 1.5f)

        if (!isRoomTab) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = w * 0.12f, bottom = h * 0.18f).scale(tabletScale)) {
                HolidayHostsBackground(currentOutfit = ALL_OUTFITS.find { it.id == currentOutfitId } ?: ALL_OUTFITS[0], onHostClick = { vibrate(); showToast("С Новым Годом! 🎄") })
            }

            Box(modifier = Modifier.align(Alignment.Center).fillMaxHeight(if (isTablet) 0.85f else 0.75f).padding(bottom = 20.dp).scale(0.8f * tabletScale), contentAlignment = Alignment.BottomCenter) {
                ChristmasTree25D(isRoom = false, treeLevel = streetTreeLevel, garlandMode = lightsMode, onTreeClick = {
                    if (streetTreeLevel == 0) showToast("Ёлочка пока без украшений! Улучши её ✨")
                    else { vibrate(); lightsMode = (lightsMode + 1) % 3; snowflakesState.longValue += clickPower.toLong(); persistGameState(); showToast("+${clickPower} ❄️ за украшение елки!") }
                })
            }

            Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = h * 0.22f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1C1335).copy(alpha = 0.94f), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.7f)), shadowElevation = 4.dp, modifier = Modifier.padding(end = 6.dp)) {
                    Text(text = "Ур. $streetTreeLevel / 30", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                val upgradeCost = calculateStreetTreeCost(streetTreeLevel)
                val canUpgrade = snowflakesState.longValue >= upgradeCost && streetTreeLevel < 30
                Surface(shape = RoundedCornerShape(16.dp), color = if (canUpgrade) GoldAccent else NightSurfaceVariant.copy(alpha = 0.95f), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent), shadowElevation = 6.dp, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                    if (canUpgrade) { 
                        vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                        snowflakesState.longValue -= upgradeCost; streetTreeLevel += 1; persistGameState(); showToast("Уличная ёлка улучшена! ✨ Ур. $streetTreeLevel") 
                    } else if (streetTreeLevel >= 30) showToast("Ёлка максимального уровня!") else showToast("Нужно ${formatNumber(upgradeCost)} ❄️!")
                }) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⭐", fontSize = 12.sp); Spacer(modifier = Modifier.width(5.dp))
                        Text(text = if (streetTreeLevel >= 30) "Макс. Уровень" else "Улучшить (${formatNumber(upgradeCost)} ❄️)", color = if (canUpgrade) Color(0xFF1C1200) else SnowWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isRoomTab) {
            Box(modifier = Modifier.align(Alignment.Center).fillMaxHeight(if (isTablet) 0.92f else 0.88f).padding(bottom = 20.dp).scale(0.70f * tabletScale), contentAlignment = Alignment.BottomCenter) {
                ChristmasTree25D(isRoom = true, treeLevel = roomTreeLevel, garlandMode = lightsMode, onTreeClick = {
                    vibrate(); snowflakesState.longValue += clickPower.toLong(); persistGameState(); showToast("+${clickPower} ❄️")
                })
            }

            Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = h * 0.22f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                val roomCost = calculateRoomTreeCost(roomTreeLevel)
                val canUpgradeRoom = snowflakesState.longValue >= roomCost && roomTreeLevel < 15
                Surface(shape = RoundedCornerShape(16.dp), color = if (canUpgradeRoom) GoldAccent else NightSurfaceVariant.copy(alpha = 0.95f), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent), shadowElevation = 6.dp, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                    if (canUpgradeRoom) { 
                        vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                        snowflakesState.longValue -= roomCost; roomTreeLevel += 1; persistGameState(); showToast("Комнатная ёлка улучшена! ✨ Ур. $roomTreeLevel") 
                    } else showToast("Нужно ${formatNumber(roomCost)} ❄️!")
                }) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🪴", fontSize = 12.sp); Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "Улучшить ёлку (${formatNumber(roomCost)} ❄️)", color = if (canUpgradeRoom) Color(0xFF1C1200) else SnowWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = w * 0.05f, bottom = h * 0.18f).scale(tabletScale)) {
                InteractiveCat(isRoom = true, satiety = catSatiety, isAwake = isCatAwake, plusOneTrigger = catPlusOneTrigger, bowlLevel = bowlLevel, canUpgradeBowl = snowflakesState.longValue >= calculateStreetTreeCost(bowlLevel), onUpgradeBowl = {
                    val cost = calculateStreetTreeCost(bowlLevel)
                    if (snowflakesState.longValue >= cost) { 
                        vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                        snowflakesState.longValue -= cost; bowlLevel += 1; persistGameState(); showToast("Миска улучшена! 🥣✨") 
                    } else showToast("Мало снежинок! Нужно ${formatNumber(cost)} ❄️")
                }, onBowlClick = { vibrate(); isFeedCatDialogOpen = true }, onCatClick = {
                    if (catSatiety > 0) { 
                        vibrate(); snowflakesState.longValue += clickPower.toLong(); catPlusOneTrigger++; isCatAwake = true; persistGameState(); coroutineScope.launch { delay(1000); isCatAwake = false } 
                    } else { vibrate(); isFeedCatDialogOpen = true }
                }, clickPower = clickPower)
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = w * 0.06f, bottom = h * 0.20f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.Black, border = androidx.compose.foundation.BorderStroke(4.dp, Color.DarkGray), modifier = Modifier.size(70.dp, 50.dp).clickable { vibrate(); isTvOpen = true }) { Box(contentAlignment = Alignment.Center) { Text("📺", fontSize = 30.sp) } }
                    Box(modifier = Modifier.width(100.dp).height(15.dp).background(Color(0xFF5C3A21), RoundedCornerShape(4.dp)))
                }
            }

            Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = w * 0.03f, top = h * 0.15f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color.White, modifier = Modifier.size(40.dp, 50.dp).clickable { vibrate(); showToast("Скоро Новый Год! 🎄") }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(Color.Red))
                            Text("31", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(100.dp).height(70.dp).background(Color(0xFF4A2511), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)), contentAlignment = Alignment.BottomCenter) {
                        Text("🔥", fontSize = 30.sp, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
            }
        }

        if (!isRoomTab) {
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = w * 0.05f, bottom = h * 0.18f).scale(tabletScale)) {
                InteractiveSnowman(plusOneTrigger = snowmanPlusOneTrigger, onSnowmanClick = { vibrate(); snowflakesState.longValue += clickPower.toLong(); snowmanPlusOneTrigger++; persistGameState() }, clickPower = clickPower)
            }
            SnowfallLayer(isForeground = true, intensity = snowIntensity * 1.4f)
        }

        DailyRewardButton(isClaimedToday = lastDailyRewardDay == currentDateStr, onClaimReward = {
            if (lastDailyRewardDay != currentDateStr) { 
                vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                snowflakesState.longValue += 100; lastDailyRewardDay = currentDateStr; persistGameState(); showToast("Подарок получен! +100 ❄️ 🎁") 
            } else showToast("Вы уже забрали подарок сегодня! 🎁")
        }, modifier = Modifier.align(Alignment.TopStart).padding(top = 10.dp, start = 16.dp))

        SnowflakeCounterTop(snowflakes = snowflakesState.longValue, incomeRate = effectiveIncomeRate, donateBalance = donateBalance, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))

        Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isRoomTab) {
                IconButton(onClick = { vibrate(); isBackgroundsOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, IceCyan.copy(alpha = 0.5f), CircleShape)) { Text(text = "🖼️", fontSize = 16.sp) }
            }
            IconButton(onClick = { vibrate(); isWardrobeOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, GoldAccent.copy(alpha = 0.5f), CircleShape)) { Text(text = "👗", fontSize = 16.sp) }
            IconButton(onClick = { vibrate(); isShopOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, GoldAccent.copy(alpha = 0.5f), CircleShape)) { Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = "Магазин", tint = GoldAccent, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { vibrate(); isSettingsOpen = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(NightSurfaceVariant.copy(alpha = 0.85f)).border(1.dp, IceCyan.copy(alpha = 0.3f), CircleShape)) { Icon(imageVector = Icons.Filled.Settings, contentDescription = "Настройки", tint = GoldAccent, modifier = Modifier.size(18.dp)) }
        }

        GameToast(message = toastMessage, modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp))
        GameBottomNavigationBar(currentTab = currentTab, onTabSelected = { tab -> vibrate(); currentTab = tab }, modifier = Modifier.align(Alignment.BottomCenter))

        if (isShopOpen) {
            ShopDialog(snowflakes = snowflakesState.longValue, clickPower = clickPower, onDismiss = { isShopOpen = false }, onBuyItem = { itemIndex, cost ->
                when (itemIndex) {
                    0 -> if (snowflakesState.longValue >= 15) { vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f); snowflakesState.longValue -= 15; catSatiety = (catSatiety + 30).coerceAtMost(100); persistGameState(); showToast("Мур! Кот покормлен 🐟") } else showToast("Мало снежинок!")
                    1 -> if (snowflakesState.longValue >= cost) { vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f); snowflakesState.longValue -= cost; clickPower += 1; persistGameState(); showToast("Сила клика +1! 👆✨") } else showToast("Мало снежинок!")
                }
            })
        }

        if (isFeedCatDialogOpen) {
            FeedCatDialog(snowflakes = snowflakesState.longValue, currentSatiety = catSatiety, onDismiss = { isFeedCatDialogOpen = false }, onConfirmFeed = {
                if (snowflakesState.longValue >= 15) { vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f); snowflakesState.longValue -= 15; catSatiety = (catSatiety + 30).coerceAtMost(100); persistGameState(); showToast("Кот покормлен 🐟") } else showToast("Мало снежинок!")
                isFeedCatDialogOpen = false
            })
        }

        if (isWardrobeOpen) {
            WardrobeDialog(snowflakesState.longValue, donateBalance, purchasedOutfits, currentOutfitId, onDismiss = { isWardrobeOpen = false }, onBuyOutfit = { outfit ->
                    if (outfit.currency == CurrencyType.SNOWFLAKES && snowflakesState.longValue >= outfit.price) {
                        vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                        snowflakesState.longValue -= outfit.price; purchasedOutfits = purchasedOutfits + outfit.id; currentOutfitId = outfit.id
                        persistGameState(); showToast("Куплен и надет: ${outfit.title}!")
                    } else if (outfit.currency == CurrencyType.DONATE) {
                        vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                        showToast("Покупка за донат будет в RuStore! (Тест: списание бесплатно)")
                        purchasedOutfits = purchasedOutfits + outfit.id; currentOutfitId = outfit.id; persistGameState()
                    } else showToast("Мало снежинок!")
                }, onEquipOutfit = { outfit -> vibrate(); currentOutfitId = outfit.id; persistGameState(); showToast("Надет: ${outfit.title}") }
            )
        }

        if (isBackgroundsOpen) {
            BackgroundDialog(snowflakes = snowflakesState.longValue, purchasedBackgrounds = purchasedBackgrounds, currentBackgroundId = currentBackgroundId, onDismiss = { isBackgroundsOpen = false }, onBuyBackground = { bg ->
                    if (snowflakesState.longValue >= bg.price) { vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f); snowflakesState.longValue -= bg.price; purchasedBackgrounds = purchasedBackgrounds + bg.id; currentBackgroundId = bg.id; persistGameState(); showToast("Куплен фон: ${bg.title}!") } else showToast("Мало снежинок!")
                }, onEquipBackground = { bg -> vibrate(); currentBackgroundId = bg.id; persistGameState(); showToast("Выбран фон: ${bg.title}") }
            )
        }

        if (isSettingsOpen) {
            SettingsDialog(snowIntensity, onIntensityChange = { snowIntensity = it }, isMusicEnabled, onMusicToggle = { isMusicEnabled = it }, isSfxEnabled, onSfxToggle = { isSfxEnabled = it }, isVibrationEnabled, onVibrationToggle = { isVibrationEnabled = it }, musicVolume, onMusicVolumeChange = { musicVolume = it }, sfxVolume, onSfxVolumeChange = { sfxVolume = it }, onDismiss = { isSettingsOpen = false; persistGameState() }, onExitToMenu = onExitToMenu)
        }

        if (isTvOpen) {
            Dialog(onDismissRequest = { isTvOpen = false }) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(2.dp, GoldAccent)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📺 Мини-игры", color = GoldAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Тут скоро появится «Волк ловит яйца» и другие советские игры!", color = SnowWhite, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { isTvOpen = false }, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)) { Text("Закрыть", color = Color.Black) }
                    }
                }
            }
        }
    }
}

fun getTodayDateStr(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

@Composable
fun BackgroundDialog(snowflakes: Long, purchasedBackgrounds: Set<Int>, currentBackgroundId: Int, onDismiss: () -> Unit, onBuyBackground: (RoomBackground) -> Unit, onEquipBackground: (RoomBackground) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.2.dp, IceCyan.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Фоны комнаты 🖼️", color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Баланс: $snowflakes ❄️", color = GoldAccentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ALL_BACKGROUNDS) { bg ->
                        val isPurchased = purchasedBackgrounds.contains(bg.id)
                        val isEquipped = currentBackgroundId == bg.id
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NightSurfaceVariant).border(1.dp, if (isEquipped) IceCyan else Color.Transparent, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(bg.wallColor), contentAlignment = Alignment.Center) { Text("🪟", fontSize = 24.sp) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) { Text(bg.title, color = SnowWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            if (isEquipped) Text("Выбран", color = IceCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            else if (isPurchased) Button(onClick = { onEquipBackground(bg) }, colors = ButtonDefaults.buttonColors(containerColor = IceCyan)) { Text("Выбрать", color = Color.Black, fontSize = 12.sp) }
                            else {
                                val canBuy = snowflakes >= bg.price
                                Button(onClick = { onBuyBackground(bg) }, colors = ButtonDefaults.buttonColors(containerColor = if (canBuy) GoldAccent else Color.DarkGray)) { Text("${formatNumber(bg.price)} ❄️", color = if (canBuy) Color.Black else SnowSoft, fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HolidayHostsBackground(currentOutfit: Outfit, onHostClick: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "HostsIdle")
    val sway by transition.animateFloat(initialValue = -1.5f, targetValue = 1.5f, animationSpec = infiniteRepeatable(animation = tween(2200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "HostsSway")
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.offset(y = sway.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onHostClick() }) { Text(text = "🎅", fontSize = 38.sp); Text(text = "Дед Мороз", color = HolidayRed, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onHostClick() }) {
            EmojiOrImage(emoji = currentOutfit.emoji, imageRes = currentOutfit.imageRes, fontSize = 35.sp)
            Surface(shape = RoundedCornerShape(6.dp), color = NightSurfaceVariant.copy(alpha = 0.8f), border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.5f))) { Text(text = currentOutfit.title, color = IceCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) }
        }
    }
}

@Composable
fun ChristmasTree25D(isRoom: Boolean, treeLevel: Int, garlandMode: Int, onTreeClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "TreeAnimation")
    val starGlow by infiniteTransition.animateFloat(initialValue = 0.6f, targetValue = 1.0f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "StarGlow")
    val lightBlink by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1.0f, animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "LightBlink")
    val treeBoxWidth = if (isRoom) 210.dp else 290.dp
    BoxWithConstraints(modifier = modifier.width(treeBoxWidth).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTreeClick() }, contentAlignment = Alignment.BottomCenter) {
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
    val catEmoji = when { isAwake -> "😻"; satiety <= 0 -> "😿"; else -> "🐈‍⬛" }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.Center) { FloatingClickParticle(trigger = plusOneTrigger, text = "+$clickPower ❄️", color = GoldAccent) }
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
            Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onCatClick() }, contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(86.dp, 36.dp)) {
                    val w = size.width; val h = size.height
                    drawOval(brush = Brush.radialGradient(colors = listOf(Color.Black.copy(alpha = 0.68f), Color.Black.copy(alpha = 0.30f), Color.Transparent), radius = w / 2f), size = Size(w, h))
                    drawRoundRect(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF5B1E28), Color(0xFF8C2E3C), Color(0xFF5B1E28))), topLeft = Offset(4.dp.toPx(), 6.dp.toPx()), size = Size(w - 8.dp.toPx(), h - 10.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
                }
                Text(text = catEmoji, fontSize = if (isAwake) 36.sp else 32.sp, modifier = Modifier.scale(if (isAwake) 1.15f else breathScale).padding(bottom = 6.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 2.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = if (canUpgradeBowl) GoldAccent else NightSurfaceVariant.copy(alpha = 0.95f), border = androidx.compose.foundation.BorderStroke(1.dp, if (canUpgradeBowl) GoldAccentDark else GoldAccent.copy(alpha = 0.35f)), shadowElevation = 3.dp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onUpgradeBowl() }) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🥣+", fontSize = 11.sp); Spacer(modifier = Modifier.width(3.dp)); Text(text = "Улучшить", color = if (canUpgradeBowl) Color(0xFF1C1200) else SnowWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onBowlClick() }, contentAlignment = Alignment.Center) {
                    Text(text = if (bowlLevel == 0) "🥣" else "🥣✨", fontSize = if (bowlLevel == 0) 22.sp else 24.sp)
                }
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
        Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSnowmanClick() }, contentAlignment = Alignment.Center) {
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
fun WardrobeDialog(snowflakes: Long, donateBalance: Long, purchasedOutfits: Set<Int>, currentOutfitId: Int, onDismiss: () -> Unit, onBuyOutfit: (Outfit) -> Unit, onEquipOutfit: (Outfit) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Гардероб Снегурочки 👗", color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Баланс: $snowflakes ❄️", color = GoldAccentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Донат: $donateBalance 💎", color = IceCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ALL_OUTFITS) { outfit ->
                        val isPurchased = purchasedOutfits.contains(outfit.id)
                        val isEquipped = currentOutfitId == outfit.id
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NightSurfaceVariant).border(1.dp, if (isEquipped) GoldAccent else Color.Transparent, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            EmojiOrImage(emoji = outfit.emoji, imageRes = outfit.imageRes); Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(outfit.title, color = SnowWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("+${outfit.incomeBonus} ❄️/сек", color = Color(0xFF52B788), fontSize = 12.sp)
                            }
                            if (isEquipped) Text("Надет", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            else if (isPurchased) Button(onClick = { onEquipOutfit(outfit) }, colors = ButtonDefaults.buttonColors(containerColor = IceCyan)) { Text("Надеть", color = Color.Black, fontSize = 12.sp) }
                            else {
                                val canBuy = (outfit.currency == CurrencyType.SNOWFLAKES && snowflakes >= outfit.price) || (outfit.currency == CurrencyType.DONATE)
                                Button(onClick = { onBuyOutfit(outfit) }, colors = ButtonDefaults.buttonColors(containerColor = if (canBuy) GoldAccent else Color.DarkGray)) {
                                    Text(if (outfit.currency == CurrencyType.DONATE) "${outfit.price} 💎" else "${formatNumber(outfit.price)} ❄️", color = if (canBuy) Color.Black else SnowSoft, fontSize = 12.sp)
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
fun SnowflakeCounterTop(snowflakes: Long, incomeRate: Int, donateBalance: Long, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.shadow(10.dp, RoundedCornerShape(22.dp)), shape = RoundedCornerShape(22.dp), color = Color(0xFF141A33).copy(alpha = 0.94f), border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.65f))) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = "❄️", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
            Text(text = formatNumber(snowflakes), color = GoldAccent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = if (incomeRate > 0) "+$incomeRate/сек" else "0/сек", color = if (incomeRate > 0) Color(0xFF52B788) else SnowSoft.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ShopDialog(snowflakes: Long, clickPower: Int, onDismiss: () -> Unit, onBuyItem: (itemIndex: Int, cost: Long) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth(0.94f).padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Новогодняя Лавка 🛍️", color = GoldAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(imageVector = Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
                }
                Spacer(modifier = Modifier.height(14.dp))
                val costClick = calculateStreetTreeCost(clickPower)
                val items = listOf(ShopItem("Корм для кота", "🐟", 15L, "+30% к сытости котика"), ShopItem("Сила клика", "👆", costClick, "+1 к снежинкам за клик"))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEachIndexed { index, item ->
                        val canAfford = snowflakes >= item.cost
                        Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(NightSurfaceVariant).border(1.2.dp, if (canAfford) GoldAccent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = item.emoji, fontSize = 24.sp); Spacer(modifier = Modifier.height(4.dp)); Text(text = item.name, color = SnowWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(2.dp)); Text(text = item.description, color = SnowSoft.copy(alpha = 0.75f), fontSize = 8.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onBuyItem(index, item.cost) }, colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) GoldAccent else Color(0xFF332947)), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(34.dp)) { Text(text = "${formatNumber(item.cost)} ❄️", color = if (canAfford) Color(0xFF1C1200) else SnowSoft.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
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
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth(0.78f).padding(10.dp)) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🐟", fontSize = 34.sp); Spacer(modifier = Modifier.height(6.dp)); Text(text = "Покормить кота за 15 ❄️?", color = SnowWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(4.dp)); Text(text = "Сытость восстановится на +30%\n(текущая: $currentSatiety%)", color = SnowSoft.copy(alpha = 0.8f), fontSize = 11.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = NightSurfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(36.dp)) { Text(text = "Отмена", color = SnowSoft, fontSize = 11.sp) }
                    Button(onClick = onConfirmFeed, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(36.dp)) { Text(text = "Да, 15 ❄️", color = Color(0xFF1C1200), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}

@Composable
fun DailyRewardButton(isClaimedToday: Boolean, onClaimReward: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "DailyGiftGlow")
    val giftGlowScale by transition.animateFloat(initialValue = 1.0f, targetValue = 1.08f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "GiftScale")
    Surface(shape = RoundedCornerShape(16.dp), color = if (!isClaimedToday) HolidayRed.copy(alpha = 0.95f) else NightSurfaceVariant.copy(alpha = 0.85f), border = androidx.compose.foundation.BorderStroke(1.2.dp, if (!isClaimedToday) GoldAccent else SnowSoft.copy(alpha = 0.3f)), shadowElevation = if (!isClaimedToday) 8.dp else 2.dp, modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable { onClaimReward() }) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🎁", fontSize = 16.sp, modifier = Modifier.scale(if (!isClaimedToday) giftGlowScale else 1.0f)); Spacer(modifier = Modifier.width(6.dp))
            Column { Text(text = if (!isClaimedToday) "Подарок" else "Завтра", color = if (!isClaimedToday) SnowWhite else SnowSoft.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(text = if (!isClaimedToday) "+100 ❄️" else "00:00", color = if (!isClaimedToday) GoldAccent else SnowSoft.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
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
fun RoomInteriorScenery(wallColor: Color = Color(0xFF2B1A14)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val floorY = h * 0.74f; val ceilingY = h * 0.1f
        drawRect(color = Color(0xFF1A0F0A), topLeft = Offset(0f, 0f), size = Size(w, ceilingY))
        drawRect(brush = Brush.verticalGradient(colors = listOf(wallColor, wallColor.copy(alpha = 0.6f))), topLeft = Offset(0f, ceilingY), size = Size(w, floorY - ceilingY))
        val winX = w * 0.08f; val winY = h * 0.16f; val winW = w * 0.26f; val winH = h * 0.48f
        drawRoundRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF060F24), Color(0xFF0D1B38), Color(0xFF192A4E)), startY = winY, endY = winY + winH), topLeft = Offset(winX, winY), size = Size(winW, winH), cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()))
        drawRoundRect(color = Color(0xFF4A2511), topLeft = Offset(winX - 6.dp.toPx(), winY - 6.dp.toPx()), size = Size(winW + 12.dp.toPx(), winH + 12.dp.toPx()), cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()), style = Stroke(width = 6.dp.toPx()))
        drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF4A2612), Color(0xFF2E170A)), startY = floorY, endY = h), topLeft = Offset(0f, floorY), size = Size(w, h - floorY))
        drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF5C2E15), Color(0xFF3E1C0D))), topLeft = Offset(0f, floorY - 10.dp.toPx()), size = Size(w, 12.dp.toPx()))
    }
}

@Composable
fun StreetExteriorScenery(isDay: Boolean) {
    val backgroundImage = if (isDay) R.drawable.bg_day else R.drawable.bg_night
    Image(painter = painterResource(id = backgroundImage), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}

@Composable
fun SnowfallLayer(isForeground: Boolean, intensity: Float = 1.0f, modifier: Modifier = Modifier) {
    val duration = if (isForeground) 5500 else 11000
    val transition = rememberInfiniteTransition(label = "SnowLayer")
    val cycleProgress by transition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(durationMillis = duration, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "SnowFallingCycle")
    val flakes = remember(isForeground) {
        val count = if (isForeground) 28 else 55
        List(count) { Snowflake(xNorm = Random.nextFloat(), yOffsetNorm = Random.nextFloat(), speed = 0.25f + Random.nextFloat() * 0.45f, radius = 1.0f + Random.nextFloat() * 1.8f, swayFrequency = 0.8f + Random.nextFloat() * 1.5f, swayAmplitude = 5f + Random.nextFloat() * 10f, alpha = 0.25f + Random.nextFloat() * 0.35f) }
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
    Surface(modifier = modifier.padding(bottom = 12.dp).shadow(12.dp, RoundedCornerShape(30.dp)), shape = RoundedCornerShape(30.dp), color = NightSurface.copy(alpha = 0.94f), border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.wrapContentWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            GameTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                Box(modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(if (isSelected) GoldAccent.copy(alpha = 0.22f) else Color.Transparent).border(width = if (isSelected) 1.dp else 0.dp, color = if (isSelected) GoldAccent.copy(alpha = 0.7f) else Color.Transparent, shape = RoundedCornerShape(24.dp)).clickable { onTabSelected(tab) }.padding(horizontal = 24.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(imageVector = tab.icon, contentDescription = tab.label, tint = if (isSelected) GoldAccent else SnowSoft.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = tab.label, color = if (isSelected) GoldAccent else SnowSoft.copy(alpha = 0.75f), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    snowIntensity: Float, onIntensityChange: (Float) -> Unit,
    isMusicEnabled: Boolean, onMusicToggle: (Boolean) -> Unit,
    isSfxEnabled: Boolean, onSfxToggle: (Boolean) -> Unit,
    isVibrationEnabled: Boolean, onVibrationToggle: (Boolean) -> Unit,
    musicVolume: Float, onMusicVolumeChange: (Float) -> Unit,
    sfxVolume: Float, onSfxVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NightSurface), border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth(0.9f).padding(12.dp)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Настройки ⚙️", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(imageVector = Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Музыка", color = SnowWhite, fontSize = 13.sp)
                    Switch(checked = isMusicEnabled, onCheckedChange = onMusicToggle, colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = GoldAccentDark))
                }
                Slider(value = musicVolume, onValueChange = onMusicVolumeChange, colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Звуки", color = SnowWhite, fontSize = 13.sp)
                    Switch(checked = isSfxEnabled, onCheckedChange = onSfxToggle, colors = SwitchDefaults.colors(checkedThumbColor = IceCyan, checkedTrackColor = IceCyanDark))
                }
                Slider(value = sfxVolume, onValueChange = onSfxVolumeChange, colors = SliderDefaults.colors(thumbColor = IceCyan, activeTrackColor = IceCyan))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Вибрация при клике", color = SnowWhite, fontSize = 13.sp)
                    Switch(checked = isVibrationEnabled, onCheckedChange = onVibrationToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF52B788), checkedTrackColor = Color(0xFF1B4332)))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Интенсивность снега: ${(snowIntensity * 100).toInt()}%", color = SnowWhite, fontSize = 12.sp)
                Slider(value = snowIntensity, onValueChange = onIntensityChange, valueRange = 0.5f..2.0f, colors = SliderDefaults.colors(thumbColor = SnowSoft, activeTrackColor = SnowSoft.copy(alpha = 0.5f)))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = { onDismiss(); onExitToMenu() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E32)), modifier = Modifier.fillMaxWidth()) {
                    Text("В главное меню", color = SnowWhite)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Версия 6.2 • Звуки и UI исправлены 💎", color = SnowSoft.copy(alpha = 0.6f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
