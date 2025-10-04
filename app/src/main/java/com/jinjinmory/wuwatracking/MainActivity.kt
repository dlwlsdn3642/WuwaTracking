package com.jinjinmory.wuwatracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jinjinmory.wuwatracking.ui.theme.WuwaTrackingTheme
import com.jinjinmory.wuwatracking.ui.main.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WuwaTrackingTheme {
                MainScreen()
            }
        }
    }
}
