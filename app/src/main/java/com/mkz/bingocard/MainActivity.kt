package com.mkz.bingocard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mkz.bingocard.ui.theme.BingoCardTheme
import com.mkz.bingocard.ui.BingoApp
import com.mkz.bingocard.vision.AndroidContextProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidContextProvider.init(application)
        enableEdgeToEdge()
        setContent {
            BingoCardTheme {
                BingoApp()
            }
        }
    }
}