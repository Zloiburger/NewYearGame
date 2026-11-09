package com.example.ui

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay

sealed class Screen {
    data object Splash : Screen()
    data object Menu : Screen()
    data object Cutscene : Screen()
    data object Game : Screen()
}

@Composable
fun App() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cozy_new_year_prefs", Context.MODE_PRIVATE) }
    
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val storySeen = remember { prefs.getBoolean("story_seen", false) }

    when (currentScreen) {
        Screen.Splash -> SplashScreen(onTimeout = { currentScreen = Screen.Menu })
        
        Screen.Menu -> MainMenuScreen(
            onStartGame = { currentScreen = if (storySeen) Screen.Game else Screen.Cutscene },
            onOpenAlbum = { /* Пока заглушка, сделаем на следующем шаге */ },
            onOpenSettings = { /* Пока заглушка */ }
        )
        
        Screen.Cutscene -> CutsceneScreen(onFinish = {
            prefs.edit().putBoolean("story_seen", true).apply()
            currentScreen = Screen.Game
        })
        
        Screen.Game -> MainScreen(
            onExitToMenu = { currentScreen = Screen.Menu }
        )
    }
}

// ==========================================
// ГЛАВНОЕ МЕНЮ
// ==========================================
@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1321), Color(0xFF1D2D44)))),
        contentAlignment = Alignment.Center
    ) {
        // Фоновые снежинки или елка (можно добавить позже)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🎄",
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Новогодняя Сказка",
                color = GoldAccent,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Уютный праздничный кликер",
                color = SnowSoft,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            // Кнопки меню
            MenuButton(text = "Начать игру", icon = Icons.Filled.PlayArrow, onClick = onStartGame)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = "Альбом", icon = Icons.Filled.Star, onClick = onOpenAlbum)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = "Настройки", icon = Icons.Filled.Settings, onClick = onOpenSettings)
        }
    }
}

@Composable
fun MenuButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = NightSurface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.7f)),
        modifier = Modifier.width(250.dp).height(56.dp)
    ) {
        Icon(icon, contentDescription = null, tint = GoldAccent)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// СЮЖЕТНАЯ ЗАСТАВКА (КОМИКС)
// ==========================================
data class StorySlide(
    @DrawableRes val imageRes: Int? = null, // Пока null, картинки сгенерируем позже
    val fallbackEmoji: String,
    val text: String
)

val STORY_SLIDES = listOf(
    StorySlide(null, "🎅", "Накануне Нового года на складе Деда Мороза случилась беда..."),
    StorySlide(null, "🌪️", "Порыв метели рассыпал все подарки по окрестностям!"),
    StorySlide(null, "😢", "— Без подарков праздник не состоится! — расстроился Дед Мороз."),
    StorySlide(null, "❄️", "— Поможешь нам? Ты можешь зарабатывать снежинки, чтобы спасти праздник!"),
    StorySlide(null, "🎄", "Вперед! С каждым кликом мешок подарков растет...")
)

@Composable
fun CutsceneScreen(onFinish: () -> Unit) {
    var slideIndex by remember { mutableIntStateOf(0) }
    val isLastSlide = slideIndex == STORY_SLIDES.lastIndex
    val slide = STORY_SLIDES[slideIndex]

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D1321)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Область картинки
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .background(Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
            contentAlignment = Alignment.Center
        ) {
            if (slide.imageRes != null) {
                Image(
                    painter = painterResource(id = slide.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Заглушка, пока нет картинок
                Text(text = slide.fallbackEmoji, fontSize = 120.sp, modifier = Modifier.scale(1.2f))
            }
        }

        // Плашка с текстом
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1B1B38).copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = slide.text,
                color = SnowWhite,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { if (isLastSlide) onFinish() else slideIndex++ },
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(bottom = 32.dp).height(50.dp).width(200.dp)
        ) {
            Text(
                text = if (isLastSlide) "Спасём Новый год! 🎄" else "Далее (${slideIndex + 1}/${STORY_SLIDES.size})",
                color = Color(0xFF1C1200),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
