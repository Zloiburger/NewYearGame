package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun AlbumScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val savedState = remember { GameStorage.load(context) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D1321)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { 
                    Icon(Icons.Filled.ArrowBack, "Назад", tint = SnowSoft) 
                }
                Text("Альбом", color = GoldAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        item(span = { GridItemSpan(2) }) {
            Text("Хроника ёлки (Улица)", color = SnowWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        items(6) { step ->
            val shownLevel = (step + 1) * 5
            val isUnlocked = savedState.streetTreeLevel >= shownLevel
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isUnlocked) NightSurfaceVariant else Color.Black.copy(alpha = 0.5f),
                border = borderStroke(1.dp, if (isUnlocked) GoldAccent.copy(alpha = 0.5f) else Color.Transparent),
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            ) {
                if (isUnlocked) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Image(
                            painter = painterResource(id = R.drawable.tree_main),
                            contentDescription = "Ёлка Ур. $shownLevel",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            color = Color(0xCC000000),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Ур. $shownLevel",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Закрыто",
                            tint = SnowSoft.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Коллекция нарядов", color = SnowWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        items(ALL_OUTFITS) { outfit ->
            val isPurchased = savedState.purchasedOutfitIds.contains(outfit.id)
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isPurchased) NightSurfaceVariant else Color.Black.copy(alpha = 0.3f),
                border = borderStroke(1.dp, if (isPurchased) IceCyan.copy(alpha = 0.5f) else Color.Transparent),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    verticalArrangement = Arrangement.Center, 
                    modifier = Modifier.padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        EmojiOrImage(emoji = outfit.emoji, imageRes = outfit.imageRes, fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPurchased) outfit.title else "???",
                        color = if (isPurchased) SnowWhite else SnowSoft.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
