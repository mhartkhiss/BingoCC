package com.mkz.bingocard.ui.screens

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.mkz.bingocard.R
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 6000L
private const val TYPING_DELAY_MS = 45L

private data class SplashQuote(val text: String, val author: String)

private val splashQuotes = listOf(
    SplashQuote("Sa letrang \"B\"...... first birth....", "— jake"),
    SplashQuote("Sa letrang B.... IYOTSOOOOO!!", "— duswa"),
    SplashQuote("Sa letrang O.... paborito ni dodong.... 69", "")
)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val gifPainter = rememberAsyncImagePainter(
        model = R.drawable.splash_animation,
        imageLoader = imageLoader
    )

    // Fade-in + scale-up for the GIF
    val gifAlpha = remember { Animatable(0f) }
    val gifScale = remember { Animatable(0.7f) }

    // Author fade-in (appears after typing finishes)
    val authorAlpha = remember { Animatable(0f) }

    // Pick a random quote once, making sure not to repeat the one from the last launch
    val chosen = remember {
        val prefs = context.getSharedPreferences("splash_prefs", android.content.Context.MODE_PRIVATE)
        val lastIdx = prefs.getInt("last_quote_idx", -1)
        
        val availableIndices = if (splashQuotes.size > 1) {
            splashQuotes.indices.filter { it != lastIdx }
        } else {
            splashQuotes.indices.toList()
        }
        
        val newIdx = availableIndices.random()
        prefs.edit().putInt("last_quote_idx", newIdx).apply()
        
        splashQuotes[newIdx]
    }

    // Typing animation state
    var displayedText by remember { mutableStateOf("") }
    var typingFinished by remember { mutableStateOf(false) }

    // GIF entrance animation
    LaunchedEffect(Unit) {
        gifAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        gifScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }

    // Typing animation starts after GIF zoom completes
    LaunchedEffect(Unit) {
        delay(1200)
        for (i in 1..chosen.text.length) {
            displayedText = chosen.text.substring(0, i)
            delay(TYPING_DELAY_MS)
        }
        typingFinished = true
        // Fade in the author after typing completes
        if (chosen.author.isNotEmpty()) {
            authorAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
    }

    // Navigate away after splash duration
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // GIF Image
            Image(
                painter = gifPainter,
                contentDescription = "Splash Animation",
                modifier = Modifier
                    .size(260.dp)
                    .scale(gifScale.value)
                    .alpha(gifAlpha.value)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Typing quote text
            Text(
                text = displayedText + if (!typingFinished) "▌" else "",
                color = Color(0xFFF0E6D3),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Author (fades in after typing finishes)
            if (chosen.author.isNotEmpty()) {
                Text(
                    text = chosen.author,
                    color = Color(0xFFE94560),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(authorAlpha.value)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // App name
            Text(
                text = "BingoMKZ",
                color = Color.White.copy(alpha = 0.4f * gifAlpha.value),
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(gifAlpha.value)
            )
        }
    }
}
