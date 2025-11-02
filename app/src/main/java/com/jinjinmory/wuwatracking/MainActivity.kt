package com.jinjinmory.wuwatracking

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.jinjinmory.wuwatracking.ui.theme.WuwaTrackingTheme
import com.jinjinmory.wuwatracking.ui.main.MainScreen
import com.jinjinmory.wuwatracking.data.preferences.AppPreferencesManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferencesManager.applyStoredLanguage(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WuwaTrackingTheme {
                MainScreen()
            }
        }
    }
}
