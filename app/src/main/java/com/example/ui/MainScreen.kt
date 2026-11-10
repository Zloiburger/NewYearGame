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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.random.Random

// ==========================================
// СИСТЕМА ЛОГИРОВАНИЯ
// ==========================================
object GameLogger {
    val logs = mutableStateListOf<String>()
    fun log(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add("[$time] $msg")
        if (logs.size > 100) logs.removeAt(0)
        println("GameLog: $msg")
    }
}

// ==========================================
// БЕЗОПАСНЫЙ ЗАПУСК МУЗЫКИ
// ==========================================
fun createSafeMediaPlayer(context: Context, resId: Int, name: String, isLooping: Boolean = false): MediaPlayer? {
    return try {
        val mp = MediaPlayer.create(context, resId)
        if (mp == null) {
            GameLogger.log("❌ ОШИБКА: Файл $name не найден (null)")
        } else {
            mp.isLooping = isLooping
            GameLogger.log("✅ УСПЕХ: $name загружен. Длительность: ${mp.duration} мс")
        }
        mp
    } catch (e: Exception) {
        GameLogger.log("💥 КРАХ $name: ${e.message}")
        null
    }
}

// ==========================================
// 1. МАТЕМАТИКА (НЕ ТРОГАТЬ!)
// ==========================================
fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> String.format("%.1fB", number / 1_000_000_000.0)
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 10_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

fun getRoomTreeCost(level: Int): Long = (50.0 * 1.22.pow((level.coerceIn(1, 15) - 1).toDouble())).toLong()
fun getRoomTreeCps(level: Int): Double = if (level <= 0) 0.0 else 2.0 * 1.34.pow((level.coerceIn(1, 15) - 1).toDouble())
fun getStreetTreeCost(level: Int): Long = (30000.0 * 1.48.pow((level.coerceIn(1, 30) - 1).toDouble())).toLong()
fun getStreetTreeCps(level: Int): Double = if (level <= 0) 0.0 else 250.0 * 1.45.pow((level.coerceIn(1, 30) - 1).toDouble())

fun getClickPercent(level: Int): Double {
    val percentages = listOf(0.02, 0.023, 0.027, 0.032, 0.038, 0.045, 0.053, 0.062, 0.072, 0.083, 0.095, 0.108, 0.122, 0.137, 0.15)
    return percentages[(level - 1).coerceIn(0, percentages.size - 1)]
}
fun getClickUpgradePrice(level: Int): Long {
    val prices = listOf(0L, 100L, 250L, 600L, 1500L, 4000L, 10000L, 28000L, 75000L, 200000L, 550000L, 1500000L, 4000000L, 11000000L, 30000000L)
    return prices[(level - 1).coerceIn(0, prices.size - 1)]
}
fun getClickPower(clickUpgradeLevel: Int, baseCps: Double): Double = 1.0 + (getClickPercent(clickUpgradeLevel) * baseCps)
fun getCatUpgradePrice(level: Int): Long = (500.0 * 1.28.pow((level - 1).toDouble())).toLong()
fun getCatCps(catLevel: Int, clickPower: Double): Double = if (catLevel <= 0) 0.0 else clickPower * (1.0 + (catLevel - 1) * 0.5)

data class GameStateSnapshot(val totalCps: Double, val clickPower: Double, val baseCps: Double, val catCps: Double, val outfitBonusPercent: Double)

fun calculateGameEconomy(roomLevel: Int, streetLevel: Int, clickLevel: Int, catLevel: Int, outfitIds: Set<Int>, isDonateActive: Boolean): GameStateSnapshot {
    val treeCps = getRoomTreeCps(roomLevel) + getStreetTreeCps(streetLevel)
    val outfitBonus = ALL_OUTFITS.filter { it.id in outfitIds }.sumOf { it.bonusPercent }
    val baseCps = treeCps * (1.0 + outfitBonus / 100.0)
    val clickPower = getClickPower(clickLevel, baseCps)
    val catCps = getCatCps(catLevel, clickPower)
    val totalCps = (baseCps + catCps) * (if (isDonateActive) 1.6 else 1.0)
    return GameStateSnapshot(totalCps, clickPower, baseCps, catCps, outfitBonus)
}

// ==========================================
// 2. МОДЕЛИ ДАННЫХ
// ==========================================
enum class CurrencyType { SNOWFLAKES, DONATE }

enum class GraphicsQuality {
    HIGH, MEDIUM, LOW;
    companion object {
        fun fromString(value: String?) = try { valueOf(value ?: "HIGH") } catch (e: Exception) { HIGH }
    }
}

data class Outfit(
    val id: Int, val title: String, val emoji: String, val currency: CurrencyType,
    val price: Long, val bonusPercent: Double, val isDefault: Boolean = false,
    @DrawableRes val imageRes: Int? = null
)

val ALL_OUTFITS = listOf(
    Outfit(0, "Обычный наряд", "👧", CurrencyType.SNOWFLAKES, 0L, 0.0, isDefault = true),
    Outfit(1, "Шапочка Санты", "🎅", CurrencyType.SNOWFLAKES, 300L, 0.5),
    Outfit(2, "Вязаный шарф", "🧣", CurrencyType.SNOWFLAKES, 800L, 0.8),
    Outfit(3, "Варежки", "🧤", CurrencyType.SNOWFLAKES, 5000L, 1.2),
    Outfit(4, "Зимний свитер", "🧶", CurrencyType.SNOWFLAKES, 20000L, 1.7),
    Outfit(5, "Валенки", "🥾", CurrencyType.SNOWFLAKES, 60000L, 2.3),
    Outfit(6, "Костюм Олененка", "🦌", CurrencyType.SNOWFLAKES, 180000L, 3.0),
    Outfit(7, "Маска", "🎭", CurrencyType.SNOWFLAKES, 450000L, 3.8),
    Outfit(8, "Наряд Снегурочки", "👸", CurrencyType.SNOWFLAKES, 1000000L, 4.7),
    Outfit(9, "Шуба Деда Мороза", "🧥", CurrencyType.SNOWFLAKES, 2500000L, 5.7),
    Outfit(10, "Костюм Снеговика", "⛄", CurrencyType.SNOWFLAKES, 6000000L, 6.8),
    Outfit(11, "Ледяные Доспехи", "🧊", CurrencyType.DONATE, 0L, 8.0),
    Outfit(12, "Мантия Ночи", "🌌", CurrencyType.DONATE, 0L, 9.3),
    Outfit(13, "Наряд Духа", "✨", CurrencyType.DONATE, 0L, 10.7),
    Outfit(14, "Костюм Метели", "🌪️", CurrencyType.DONATE, 0L, 12.2),
    Outfit(15, "Владыка Зимы", "👑", CurrencyType.DONATE, 0L, 14.0),
    Outfit(16, "Сияющий Северный", "🎇", CurrencyType.DONATE, 0L, 18.0),
    Outfit(17, "Легендарный Морозко", "❄️", CurrencyType.DONATE, 0L, 23.0),
    Outfit(18, "Космический Санта", "🚀", CurrencyType.DONATE, 0L, 29.0),
    Outfit(19, "Абсолютный Кристалл", "💎", CurrencyType.DONATE, 0L, 36.0),
    Outfit(20, "Божество Зимы", "🌟", CurrencyType.DONATE, 0L, 45.0)
)

data class ShopItem(val name: String, val cost: Long, val description: String)
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
fun EmojiOrImage(emoji: String, imageRes: Int? = null, modifier: Modifier = Modifier, fontSize: TextUnit = 28.sp) {
    if (imageRes != null) {
        Image(painter = painterResource(id = imageRes), contentDescription = null, modifier = modifier)
    } else {
        Text(text = emoji, fontSize = fontSize, modifier = modifier)
    }
}

// ==========================================
// 3. СОХРАНЕНИЯ
// ==========================================
data class GameSaveData(
    val snowflakes: Double, val roomTreeLevel: Int, val streetTreeLevel: Int, val catSatiety: Int,
    val lastUpdateTime: Long, val lastDailyRewardDay: String, val bowlLevel: Int,
    val purchasedOutfitIds: Set<Int>, val currentOutfitId: Int, val donateBalance: Long,
    val isMusicEnabled: Boolean, val isSfxEnabled: Boolean, val isVibrationEnabled: Boolean,
    val musicVolume: Float, val sfxVolume: Float, val snowIntensity: Float,
    val currentBackgroundId: Int, val purchasedBackgroundIds: Set<Int>,
    val clickUpgradeLevel: Int, val catLevel: Int, val snowmanCooldownEnd: Long,
    val graphicsQuality: GraphicsQuality
)

object GameStorage {
    private const val PREFS_NAME = "cozy_new_year_prefs"
    fun load(context: Context): GameSaveData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return GameSaveData(
            snowflakes = prefs.getFloat("snowflakes", 0f).toDouble(),
            roomTreeLevel = prefs.getInt("room_tree_level", 0),
            streetTreeLevel = prefs.getInt("street_tree_level", 0),
            catSatiety = prefs.getInt("cat_satiety", 100),
            lastUpdateTime = prefs.getLong("last_update_time", 0L),
            lastDailyRewardDay = prefs.getString("last_daily_reward_day", "") ?: "",
            bowlLevel = prefs.getInt("bowl_level", 0),
            purchasedOutfitIds = prefs.getStringSet("purchased_outfits", setOf("0"))?.map { it.toInt() }?.toSet() ?: setOf(0),
            currentOutfitId = prefs.getInt("current_outfit_id", 0),
            donateBalance = prefs.getLong("donate_balance", 0L),
            isMusicEnabled = prefs.getBoolean("is_music_enabled", true),
            isSfxEnabled = prefs.getBoolean("is_sfx_enabled", true),
            isVibrationEnabled = prefs.getBoolean("is_vibration_enabled", true),
            musicVolume = prefs.getFloat("music_volume", 1.0f),
            sfxVolume = prefs.getFloat("sfx_volume", 0.8f),
            snowIntensity = prefs.getFloat("snow_intensity", 1.0f),
            currentBackgroundId = prefs.getInt("current_background_id", 0),
            purchasedBackgroundIds = prefs.getStringSet("purchased_backgrounds", setOf("0"))?.map { it.toInt() }?.toSet() ?: setOf(0),
            clickUpgradeLevel = prefs.getInt("click_upgrade_level", 1),
            catLevel = prefs.getInt("cat_level", 0),
            snowmanCooldownEnd = prefs.getLong("snowman_cooldown", 0L),
            graphicsQuality = GraphicsQuality.fromString(prefs.getString("graphics_quality", "HIGH"))
        )
    }
    fun save(context: Context, data: GameSaveData) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putFloat("snowflakes", data.snowflakes.toFloat())
            .putInt("room_tree_level", data.roomTreeLevel)
            .putInt("street_tree_level", data.streetTreeLevel)
            .putInt("cat_satiety", data.catSatiety)
            .putLong("last_update_time", data.lastUpdateTime)
            .putString("last_daily_reward_day", data.lastDailyRewardDay)
            .putInt("bowl_level", data.bowlLevel)
            .putStringSet("purchased_outfits", data.purchasedOutfitIds.map { it.toString() }.toSet())
            .putInt("current_outfit_id", data.currentOutfitId)
            .putLong("donate_balance", data.donateBalance)
            .putBoolean("is_music_enabled", data.isMusicEnabled)
            .putBoolean("is_sfx_enabled", data.isSfxEnabled)
            .putBoolean("is_vibration_enabled", data.isVibrationEnabled)
            .putFloat("music_volume", data.musicVolume)
            .putFloat("sfx_volume", data.sfxVolume)
            .putFloat("snow_intensity", data.snowIntensity)
            .putInt("current_background_id", data.currentBackgroundId)
            .putStringSet("purchased_backgrounds", data.purchasedBackgroundIds.map { it.toString() }.toSet())
            .putInt("click_upgrade_level", data.clickUpgradeLevel)
            .putInt("cat_level", data.catLevel)
            .putLong("snowman_cooldown", data.snowmanCooldownEnd)
            .putString("graphics_quality", data.graphicsQuality.name)
            .apply()
    }
}

// ==========================================
// 4. ГЛАВНЫЙ ЭКРАН
// ==========================================
@Composable
fun MainScreen(onExitToMenu: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedState = remember { GameStorage.load(context) }
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    var snowflakes by remember { mutableStateOf(savedState.snowflakes) }
    var roomTreeLevel by remember { mutableIntStateOf(savedState.roomTreeLevel) }
    var streetTreeLevel by remember { mutableIntStateOf(savedState.streetTreeLevel) }
    var clickUpgradeLevel by remember { mutableIntStateOf(savedState.clickUpgradeLevel) }
    var catLevel by remember { mutableIntStateOf(savedState.catLevel) }
    var catSatiety by remember { mutableIntStateOf(savedState.catSatiety) }
    var boughtOutfitIds by remember { mutableStateOf(savedState.purchasedOutfitIds) }
    var currentOutfitId by remember { mutableIntStateOf(savedState.currentOutfitId) }

    var isDonateActive by remember {
        mutableStateOf(ALL_OUTFITS.any { it.id == savedState.currentOutfitId && it.currency == CurrencyType.DONATE })
    }

    var snowmanCooldownEnd by remember { mutableLongStateOf(savedState.snowmanCooldownEnd) }
    var currentRoomBackground by remember { mutableIntStateOf(savedState.currentBackgroundId) }
    var purchasedBackgrounds by remember { mutableStateOf(savedState.purchasedBackgroundIds) }
    var graphicsQuality by remember { mutableStateOf(savedState.graphicsQuality) }

    var isMusicEnabled by remember { mutableStateOf(savedState.isMusicEnabled) }
    var isSfxEnabled by remember { mutableStateOf(savedState.isSfxEnabled) }
    var isVibrationEnabled by remember { mutableStateOf(savedState.isVibrationEnabled) }
    var musicVolume by remember { mutableFloatStateOf(savedState.musicVolume) }
    var sfxVolume by remember { mutableFloatStateOf(savedState.sfxVolume) }
    var snowIntensity by remember { mutableFloatStateOf(savedState.snowIntensity) }
    var lastDailyRewardDay by remember { mutableStateOf(savedState.lastDailyRewardDay) }

    var isWardrobeOpen by remember { mutableStateOf(false) }
    var isShopOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isWheelOpen by remember { mutableStateOf(false) }
    var isAlbumOpen by remember { mutableStateOf(false) }
    var isDebugLogOpen by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf(GameTab.ROOM) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var treeClickTrigger by remember { mutableIntStateOf(0) }
    var raccoonClickTrigger by remember { mutableIntStateOf(0) }
    var snowmanClickTrigger by remember { mutableIntStateOf(0) }

    val currentSnapshot = remember(roomTreeLevel, streetTreeLevel, clickUpgradeLevel, catLevel, boughtOutfitIds, isDonateActive) {
        calculateGameEconomy(roomTreeLevel, streetTreeLevel, clickUpgradeLevel, catLevel, boughtOutfitIds, isDonateActive)
    }

    fun persistGameState() {
        GameStorage.save(context, GameSaveData(
            snowflakes, roomTreeLevel, streetTreeLevel, catSatiety,
            System.currentTimeMillis(), lastDailyRewardDay, 0,
            boughtOutfitIds, currentOutfitId, 0L,
            isMusicEnabled, isSfxEnabled, isVibrationEnabled, musicVolume, sfxVolume, snowIntensity,
            currentRoomBackground, purchasedBackgrounds,
            clickUpgradeLevel, catLevel, snowmanCooldownEnd, graphicsQuality
        ))
    }

    fun showToast(msg: String) { toastMessage = msg }
    LaunchedEffect(toastMessage) { if (toastMessage != null) { delay(3000); toastMessage = null } }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            snowflakes += currentSnapshot.totalCps
            if (System.currentTimeMillis() % 10000 < 1000) persistGameState()
        }
    }

    val soundPool = remember {
        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build()
    }
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

    LaunchedEffect(isMusicEnabled, currentSongIndex) {
        if (isMusicEnabled) {
            GameLogger.log("🎵 Попытка запуска трека №${currentSongIndex + 1}")
            mediaPlayer?.release()
            mediaPlayer = createSafeMediaPlayer(context, musicPlaylist[currentSongIndex], "music${currentSongIndex + 1}", isLooping = false)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(musicVolume, musicVolume)
                setOnCompletionListener { currentSongIndex = (currentSongIndex + 1) % musicPlaylist.size }
                start()
                GameLogger.log("▶️ Трек запущен!")
            }
        } else {
            GameLogger.log("⏸️ Музыка выключена пользователем")
            mediaPlayer?.pause()
        }
    }

    LaunchedEffect(musicVolume) {
        mediaPlayer?.setVolume(musicVolume, musicVolume)
    }

    LaunchedEffect(currentTab, isSfxEnabled) {
        if (currentTab == GameTab.ROOM && isSfxEnabled) {
            if (fireplacePlayer == null) {
                GameLogger.log("🔥 Запуск звука камина")
                fireplacePlayer = createSafeMediaPlayer(context, R.raw.cash_register, "fireplace", isLooping = true)?.apply {
                    setVolume(sfxVolume * 0.2f, sfxVolume * 0.2f)
                    start()
                }
            } else {
                fireplacePlayer?.start()
            }
        } else {
            fireplacePlayer?.pause()
        }
    }

    LaunchedEffect(sfxVolume) {
        fireplacePlayer?.setVolume(sfxVolume * 0.2f, sfxVolume * 0.2f)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                persistGameState()
                mediaPlayer?.pause()
                fireplacePlayer?.pause()
                soundPool.autoPause()
                GameLogger.log("💤 Приложение свернуто (пауза)")
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (isMusicEnabled) mediaPlayer?.start()
                if (currentTab == GameTab.ROOM && isSfxEnabled) fireplacePlayer?.start()
                soundPool.autoResume()
                GameLogger.log("☀️ Приложение развернуто (возврат)")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            fireplacePlayer?.stop()
            fireplacePlayer?.release()
            soundPool.release()
            GameLogger.log("🛑 Игра закрыта (выход)")
        }
    }

    val isRoomTab = currentTab == GameTab.ROOM
    val isSnowmanReady = System.currentTimeMillis() >= snowmanCooldownEnd

    val onSnowmanClick: () -> Unit = {
        val currentTime = System.currentTimeMillis()
        if (currentTime >= snowmanCooldownEnd) {
            vibrate()
            if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
            snowflakes += (currentSnapshot.clickPower * 5)
            snowmanCooldownEnd = currentTime + 3000L
            snowmanClickTrigger++
            persistGameState()
            showToast("Бонус Снеговика! +${formatNumber((currentSnapshot.clickPower * 5).toLong())}")
        }
    }

    val todayDateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    val isDailyRewardClaimedToday = lastDailyRewardDay == todayDateStr

    Box(modifier = Modifier.fillMaxSize()) {

        // ФОН
        Image(
            painter = painterResource(id = if (isRoomTab) R.drawable.bg_winter else R.drawable.bg_forest),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        SnowfallLayer(isForeground = false, quality = graphicsQuality, intensity = if (isRoomTab) snowIntensity * 0.40f else snowIntensity * 1.2f)

        // ВЕРХНЯЯ ПАНЕЛЬ (БАЛАНС)
        TopCurrencyPanel(
            snowflakes = snowflakes.toLong(),
            donateBalance = 0L,
            incomeRate = currentSnapshot.totalCps.toLong(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp)
        )

        // ПОДАРОК
        DailyRewardButton(
            isClaimedToday = isDailyRewardClaimedToday,
            onClaimReward = {
                if (!isDailyRewardClaimedToday) {
                    vibrate()
                    if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                    snowflakes += 100.0
                    lastDailyRewardDay = todayDateStr
                    persistGameState()
                    showToast("Подарок получен! +100 снежинок")
                } else {
                    showToast("Уже получен сегодня! Загляни завтра")
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 30.dp, start = 16.dp)
        )

        // КНОПКА УЛИЦА/КОМНАТА
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF141A33).copy(alpha = 0.92f),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp, end = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    vibrate()
                    currentTab = if (isRoomTab) GameTab.STREET else GameTab.ROOM
                }
        ) {
            Text(
                text = if (isRoomTab) "На улицу" else "В комнату",
                color = GoldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // ЛЕВЫЙ ПЕРСОНАЖ (КОЗЛИК)
        Image(
            painter = painterResource(id = R.drawable.goat_hero),
            contentDescription = "Козлик",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .width(100.dp) // Жестко задаем размер
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, GoldAccent, RoundedCornerShape(12.dp)) // Красивая рамка
                .clickable { vibrate(); showToast("С Новым Годом! Козлик 2027!") }
        )

        // ПРАВЫЕ ПЕРСОНАЖИ (СНЕГОВИК + ЕНОТ)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .width(100.dp), // Жестко задаем размер
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InteractiveSnowmanImage(
                plusOneTrigger = snowmanClickTrigger,
                onSnowmanClick = onSnowmanClick,
                clickPower = (currentSnapshot.clickPower * 5).toLong(),
                isReady = isSnowmanReady
            )
            InteractiveRaccoon(
                satiety = catSatiety,
                clickPower = currentSnapshot.clickPower.toLong(),
                plusOneTrigger = raccoonClickTrigger,
                onCatClick = {
                    vibrate()
                    if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                    snowflakes += currentSnapshot.clickPower
                    raccoonClickTrigger++
                    persistGameState()
                    showToast("+${formatNumber(currentSnapshot.clickPower.toLong())}")
                },
                onBowlClick = {
                    if (snowflakes >= 15 && catSatiety < 100) {
                        vibrate()
                        snowflakes -= 15
                        catSatiety = (catSatiety + 30).coerceAtMost(100)
                        persistGameState()
                        showToast("Енот покормлен!")
                    } else if (catSatiety >= 100) {
                        showToast("Енот уже сыт!")
                    } else {
                        showToast("Нужно 15 снежинок для корма!")
                    }
                }
            )
        }

        // ЦЕНТР (ЁЛКА И КНОПКА УЛУЧШЕНИЯ)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .offset(y = 10.dp), // Сдвигаем чуть вниз
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TreeCenter(
                isRoom = isRoomTab,
                treeLevel = if (isRoomTab) roomTreeLevel else streetTreeLevel,
                clickPower = currentSnapshot.clickPower,
                plusOneTrigger = treeClickTrigger,
                onTreeClick = {
                    vibrate()
                    if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                    snowflakes += currentSnapshot.clickPower
                    treeClickTrigger++
                    persistGameState()
                    showToast("+${formatNumber(currentSnapshot.clickPower.toLong())}")
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isRoomTab) {
                val roomCost = getRoomTreeCost(roomTreeLevel + 1)
                val canUpgradeRoom = snowflakes >= roomCost && roomTreeLevel < 15
                UpgradeButton(
                    text = if (roomTreeLevel >= 15) "Максимум" else "Улучшить (${formatNumber(roomCost)})",
                    canAfford = canUpgradeRoom,
                    onClick = {
                        if (canUpgradeRoom) {
                            vibrate()
                            if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                            snowflakes -= roomCost
                            roomTreeLevel += 1
                            persistGameState()
                            showToast("Ёлка улучшена! Ур. $roomTreeLevel")
                        } else if (roomTreeLevel >= 15) showToast("Максимальный уровень!")
                        else showToast("Нужно ${formatNumber(roomCost)} снежинок!")
                    }
                )
            } else {
                val streetCost = getStreetTreeCost(streetTreeLevel + 1)
                val canUpgradeStreet = snowflakes >= streetCost && streetTreeLevel < 30
                UpgradeButton(
                    text = if (streetTreeLevel >= 30) "Максимум" else "Улучшить (${formatNumber(streetCost)})",
                    canAfford = canUpgradeStreet,
                    onClick = {
                        if (canUpgradeStreet) {
                            vibrate()
                            if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                            snowflakes -= streetCost
                            streetTreeLevel += 1
                            persistGameState()
                            showToast("Уличная ёлка улучшена! Ур. $streetTreeLevel")
                        } else if (streetTreeLevel >= 30) showToast("Максимальный уровень!")
                        else showToast("Нужно ${formatNumber(streetCost)} снежинок!")
                    }
                )
            }
        }

        SnowfallLayer(isForeground = true, quality = graphicsQuality, intensity = snowIntensity * 1.4f)

        // НИЖНЯЯ ПАНЕЛЬ (КНОПКИ)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 24.dp), // Отступ от низа экрана
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavButton(iconRes = R.drawable.shop, label = "Магазин", onClick = { vibrate(); isShopOpen = true })
            NavButton(iconRes = R.drawable.icon_wardrobe, label = "Гардероб", onClick = { vibrate(); isWardrobeOpen = true })
            NavButton(iconRes = R.drawable.icon_wheel, label = "Фортуна", onClick = { vibrate(); isWheelOpen = true })
            NavButton(iconRes = R.drawable.icon_settings, label = "Настройки", onClick = { vibrate(); isSettingsOpen = true })
            NavButton(iconRes = R.drawable.icon_album, label = "Альбом", onClick = { vibrate(); isAlbumOpen = true })
        }

        GameToast(message = toastMessage, modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp))

        // ДИАЛОГИ
        if (isShopOpen) {
            ShopDialog(
                snowflakes = snowflakes.toLong(),
                catLevel = catLevel,
                canUpgradeCat = snowflakes >= getCatUpgradePrice(catLevel + 1),
                onUpgradeCat = {
                    val cost = getCatUpgradePrice(catLevel + 1)
                    if (snowflakes >= cost) {
                        vibrate()
                        if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                        snowflakes -= cost
                        catLevel += 1
                        persistGameState()
                        showToast("Енот улучшен! Ур. $catLevel")
                    } else {
                        showToast("Нужно ${formatNumber(cost)} снежинок!")
                    }
                },
                clickPower = clickUpgradeLevel,
                onDismiss = { isShopOpen = false },
                onBuyItem = { itemIndex, _ ->
                    if (itemIndex == 0) {
                        if (snowflakes >= 15) {
                            vibrate()
                            snowflakes -= 15
                            catSatiety = (catSatiety + 30).coerceAtMost(100)
                            persistGameState()
                            showToast("Корм куплен!")
                        } else showToast("Нужно 15 снежинок!")
                    } else if (itemIndex == 1) {
                        val clickCost = getClickUpgradePrice(clickUpgradeLevel)
                        if (snowflakes >= clickCost && clickUpgradeLevel < 15) {
                            vibrate()
                            snowflakes -= clickCost
                            clickUpgradeLevel += 1
                            persistGameState()
                            showToast("Сила клика улучшена! Ур. $clickUpgradeLevel")
                        } else if (clickUpgradeLevel >= 15) showToast("Максимум!")
                        else showToast("Нужно ${formatNumber(clickCost)} снежинок!")
                    }
                }
            )
        }

        if (isWardrobeOpen) {
            WardrobeDialog(
                snowflakes = snowflakes.toLong(),
                donateBalance = 0,
                purchasedOutfits = boughtOutfitIds,
                currentOutfitId = currentOutfitId,
                onDismiss = { isWardrobeOpen = false },
                onBuyOutfit = { outfit ->
                    if (outfit.currency == CurrencyType.SNOWFLAKES && snowflakes >= outfit.price) {
                        vibrate(); if (isSfxEnabled) soundPool.play(cashSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
                        snowflakes -= outfit.price; boughtOutfitIds = boughtOutfitIds + outfit.id; currentOutfitId = outfit.id
                        persistGameState(); showToast("Куплен: ${outfit.title}")
                    } else if (outfit.currency == CurrencyType.DONATE) {
                        vibrate()
                        showToast("Покупка за донат будет в RuStore!")
                        boughtOutfitIds = boughtOutfitIds + outfit.id; currentOutfitId = outfit.id; isDonateActive = true; persistGameState()
                    } else showToast("Мало снежинок!")
                },
                onEquipOutfit = { outfit ->
                    vibrate(); currentOutfitId = outfit.id
                    if (outfit.currency == CurrencyType.DONATE) isDonateActive = true else isDonateActive = false
                    persistGameState(); showToast("Надет: ${outfit.title}")
                }
            )
        }

        if (isSettingsOpen) {
            SettingsDialog(
                snowIntensity, onIntensityChange = { snowIntensity = it },
                isMusicEnabled, onMusicToggle = { isMusicEnabled = it },
                isSfxEnabled, onSfxToggle = { isSfxEnabled = it },
                isVibrationEnabled, onVibrationToggle = { isVibrationEnabled = it },
                musicVolume, onMusicVolumeChange = { musicVolume = it },
                sfxVolume, onSfxVolumeChange = { sfxVolume = it },
                onDismiss = { isSettingsOpen = false; persistGameState() },
                onExitToMenu = onExitToMenu,
                onOpenLogs = { isDebugLogOpen = true }
            )
        }

        if (isWheelOpen) {
            WheelDialog(onDismiss = { isWheelOpen = false })
        }

        if (isAlbumOpen) {
            AlbumDialog(onDismiss = { isAlbumOpen = false })
        }

        if (isDebugLogOpen) {
            DebugLogDialog(onDismiss = { isDebugLogOpen = false })
        }
    }
}

// ==========================================
// 5. СНЕГ
// ==========================================
data class SnowParticle(var x: Float, var y: Float, val speed: Float, val radius: Float)

@Composable
fun SnowfallLayer(isForeground: Boolean, quality: GraphicsQuality, intensity: Float = 1.0f, modifier: Modifier = Modifier) {
    val shouldRender = when (quality) {
        GraphicsQuality.LOW -> false
        GraphicsQuality.MEDIUM -> !isForeground
        GraphicsQuality.HIGH -> true
    }
    if (!shouldRender) return

    val baseCount = if (isForeground) 15 else 35
    val particleCount = (baseCount * intensity.coerceIn(0.5f, 2.0f)).toInt()

    val particles = remember(isForeground) {
        List(particleCount) {
            SnowParticle(
                x = Random.nextFloat(), y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.002f + 0.0005f,
                radius = if (isForeground) Random.nextFloat() * 5f + 3f else Random.nextFloat() * 2.5f + 1f
            )
        }
    }

    var frameTrigger by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                particles.forEach { p ->
                    p.y += p.speed
                    if (p.y > 1f) { p.y = 0f; p.x = Random.nextFloat() }
                }
                frameTrigger = time
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val alpha = if (isForeground) 0.75f else 0.35f
        @Suppress("UNUSED_VARIABLE") val trigger = frameTrigger
        particles.forEach { p ->
            drawCircle(Color.White.copy(alpha = alpha), p.radius, Offset(p.x * w, p.y * h))
        }
    }
}

// ==========================================
// 6. ВИЗУАЛЬНЫЕ КОМПОНЕНТЫ
// ==========================================
@Composable
fun TreeCenter(
    isRoom: Boolean,
    treeLevel: Int,
    clickPower: Double,
    plusOneTrigger: Int,
    onTreeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxLevel = if (isRoom) 15 else 30
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF141A33).copy(alpha = 0.9f),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f))
            ) {
                Text(
                    text = "Ур. $treeLevel / $maxLevel",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // ОГРАНИЧИВАЕМ ЕЛКУ И ДЕЛАЕМ РАМКУ
            Box(
                modifier = Modifier
                    .width(230.dp) // Фиксированная ширина
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, GoldAccent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTreeClick() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tree_main),
                    contentDescription = "Ёлка",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 60.dp)
        ) {
            FloatingClickParticle(trigger = plusOneTrigger, text = "+${formatNumber(clickPower.toLong())}", color = GoldAccent)
        }
    }
}

@Composable
fun FloatingClickParticle(trigger: Int, text: String = "+1", color: Color = GoldAccent) {
    var activeTrigger by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            activeTrigger = trigger
            progress.snapTo(0f)
            progress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing))
        }
    }
    if (activeTrigger > 0 && progress.value < 1f) {
        val p = progress.value
        Text(
            text = text,
            color = color.copy(alpha = (1f - p).coerceIn(0f, 1f)),
            fontSize = (14 + (1f - p) * 3).sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.offset(y = (-p * 32).dp).scale(0.85f + p * 0.35f)
        )
    }
}

@Composable
fun TopCurrencyPanel(snowflakes: Long, donateBalance: Long, incomeRate: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.85f) // Панель занимает 85% ширины экрана
            .height(70.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ui_panel),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.icon_snowflake),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = formatNumber(snowflakes),
                        color = GoldAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (incomeRate > 0) "+${formatNumber(incomeRate)}/сек" else "0/сек",
                        color = if (incomeRate > 0) Color(0xFF52B788) else SnowSoft.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.crystal),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatNumber(donateBalance),
                    color = IceCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun UpgradeButton(text: String, canAfford: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(48.dp)
            .clickable(enabled = canAfford) { onClick() }
    ) {
        Image(
            painter = painterResource(id = R.drawable.btn_green),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (canAfford) Color.White else Color.Gray.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NavButton(iconRes: Int, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.btn_frame),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun InteractiveSnowmanImage(
    plusOneTrigger: Int,
    onSnowmanClick: () -> Unit,
    clickPower: Long,
    modifier: Modifier = Modifier,
    isReady: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "SnowmanIdle")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SnowmanBob"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            FloatingClickParticle(trigger = plusOneTrigger, text = "+${formatNumber(clickPower)}", color = IceCyan)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .offset(y = bob.dp)
                .alpha(if (isReady) 1f else 0.5f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, IceCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp)) // ДОБАВИЛИ РАМКУ
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onSnowmanClick() }
        ) {
            Image(
                painter = painterResource(id = R.drawable.snowman),
                contentDescription = "Снеговик",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NightSurface.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, IceCyan.copy(alpha = 0.45f))
        ) {
            Text(
                text = if (isReady) "Бонус" else "...",
                color = IceCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun InteractiveRaccoon(
    satiety: Int,
    clickPower: Long,
    plusOneTrigger: Int,
    onCatClick: () -> Unit,
    onBowlClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            FloatingClickParticle(trigger = plusOneTrigger, text = "+${formatNumber(clickPower)}", color = GoldAccent)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, GoldAccent.copy(alpha = 0.7f), RoundedCornerShape(12.dp)) // ДОБАВИЛИ РАМКУ
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onCatClick() }
        ) {
            Image(
                painter = painterResource(id = R.drawable.raccoon),
                contentDescription = "Енот",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF261D3B))
            ) {
                val fillRatio = (satiety / 100f).coerceIn(0f, 1f)
                val barColor = when {
                    satiety > 50 -> Color(0xFF52B788)
                    satiety > 20 -> Color(0xFFF4A261)
                    else -> Color(0xFFE63946)
                }
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fillRatio).background(barColor))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "$satiety%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF4A90E2).copy(alpha = 0.9f))
                .clickable { onBowlClick() }
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(text = "Кормить 15", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DailyRewardButton(isClaimedToday: Boolean, onClaimReward: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "GiftPulse")
    val scale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GiftScale"
    )
    Box(
        modifier = modifier
            .size(72.dp)
            .scale(if (!isClaimedToday) scale else 1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isClaimedToday) { onClaimReward() }
    ) {
        Image(
            painter = painterResource(id = R.drawable.gifts),
            contentDescription = "Подарок",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (!isClaimedToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Подарок", color = GoldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GameToast(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + scaleIn(initialScale = 0.85f) + slideInVertically { it / 2 },
        exit = fadeOut() + scaleOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1B1B38).copy(alpha = 0.96f),
            border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.85f)),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = message ?: "", color = SnowWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

// ==========================================
// 7. ДИАЛОГИ
// ==========================================
@Composable
fun WardrobeDialog(
    snowflakes: Long,
    donateBalance: Long,
    purchasedOutfits: Set<Int>,
    currentOutfitId: Int,
    onDismiss: () -> Unit,
    onBuyOutfit: (Outfit) -> Unit,
    onEquipOutfit: (Outfit) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NightSurface),
            border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Гардероб", color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Баланс: $snowflakes", color = GoldAccentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Донат: $donateBalance", color = IceCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ALL_OUTFITS) { outfit ->
                        val isPurchased = purchasedOutfits.contains(outfit.id)
                        val isEquipped = currentOutfitId == outfit.id
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NightSurfaceVariant)
                                .border(1.dp, if (isEquipped) GoldAccent else Color.Transparent, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EmojiOrImage(emoji = outfit.emoji, imageRes = outfit.imageRes)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(outfit.title, color = SnowWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("+${outfit.bonusPercent}% к доходу", color = Color(0xFF52B788), fontSize = 12.sp)
                            }
                            if (isEquipped) Text("Надет", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            else if (isPurchased) Button(
                                onClick = { onEquipOutfit(outfit) },
                                colors = ButtonDefaults.buttonColors(containerColor = IceCyan)
                            ) { Text("Надеть", color = Color.Black, fontSize = 12.sp) }
                            else {
                                val canBuy = (outfit.currency == CurrencyType.SNOWFLAKES && snowflakes >= outfit.price) || (outfit.currency == CurrencyType.DONATE)
                                Button(
                                    onClick = { onBuyOutfit(outfit) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (canBuy) GoldAccent else Color.DarkGray)
                                ) {
                                    Text(
                                        if (outfit.currency == CurrencyType.DONATE) "Донат" else formatNumber(outfit.price),
                                        color = if (canBuy) Color.Black else SnowSoft,
                                        fontSize = 12.sp
                                    )
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
fun ShopDialog(
    snowflakes: Long,
    catLevel: Int,
    canUpgradeCat: Boolean,
    onUpgradeCat: () -> Unit,
    clickPower: Int,
    onDismiss: () -> Unit,
    onBuyItem: (itemIndex: Int, cost: Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NightSurface),
            border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(0.94f).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Новогодняя Лавка", color = GoldAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
                }
                Spacer(modifier = Modifier.height(14.dp))

                val costClick = getClickUpgradePrice(clickPower)
                val catUpgradeCost = getCatUpgradePrice(catLevel + 1)

                val items = listOf(
                    ShopItem("Корм", 15L, "+30% к сытости"),
                    ShopItem("Сила клика", costClick, "+ к % от CPS"),
                    ShopItem("Улучшить енота", catUpgradeCost, "Ур. ${catLevel + 1}")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEachIndexed { index, item ->
                        val canAfford = if (index == 2) canUpgradeCat else (snowflakes >= item.cost)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NightSurfaceVariant)
                                .border(1.2.dp, if (canAfford) GoldAccent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Filled.Restaurant
                                    1 -> Icons.Filled.TouchApp
                                    else -> Icons.Filled.Pets
                                },
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = item.name, color = SnowWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = item.description, color = SnowSoft.copy(alpha = 0.75f), fontSize = 8.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { if (index == 2) onUpgradeCat() else onBuyItem(index, item.cost) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) GoldAccent else Color(0xFF332947)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(34.dp)
                            ) {
                                Text(
                                    text = formatNumber(item.cost),
                                    color = if (canAfford) Color(0xFF1C1200) else SnowSoft.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
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
    onExitToMenu: () -> Unit,
    onOpenLogs: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NightSurface),
            border = BorderStroke(1.dp, IceCyan.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(0.9f).padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Настройки", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = SnowSoft) }
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
                    Text(text = "Вибрация", color = SnowWhite, fontSize = 13.sp)
                    Switch(checked = isVibrationEnabled, onCheckedChange = onVibrationToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF52B788), checkedTrackColor = Color(0xFF1B4332)))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Снег: ${(snowIntensity * 100).toInt()}%", color = SnowWhite, fontSize = 12.sp)
                Slider(
                    value = snowIntensity,
                    onValueChange = onIntensityChange,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = SnowSoft, activeTrackColor = SnowSoft.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onOpenLogs,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Логи (отладка)", color = SnowWhite) }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onDismiss(); onExitToMenu() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E32)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("В главное меню", color = SnowWhite) }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Версия 10.1 • Исправление верстки",
                    color = SnowSoft.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DebugLogDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
            border = BorderStroke(1.dp, Color.Gray),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Логи игры", color = Color.Green, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(GameLogger.logs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("❌") || log.contains("💥")) Color.Red else Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WheelDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NightSurface),
            border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(0.9f).padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Колесо Фортуны", color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, GoldAccent, RoundedCornerShape(16.dp)) // Рамка
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.wheel_fortune),
                        contentDescription = "Колесо",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Скоро здесь можно будет крутить колесо и получать призы!",
                    color = SnowWhite,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) { Text("Закрыть", color = Color.Black) }
            }
        }
    }
}

@Composable
fun AlbumDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NightSurface),
            border = BorderStroke(1.2.dp, IceCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(0.9f).padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Альбом", color = IceCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, IceCyan, RoundedCornerShape(16.dp)) // Рамка
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_album),
                        contentDescription = "Альбом",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Здесь будут коллекционные карточки, которые ты соберёшь!",
                    color = SnowWhite,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = IceCyan)
                ) { Text("Закрыть", color = Color.Black) }
            }
        }
    }
}
