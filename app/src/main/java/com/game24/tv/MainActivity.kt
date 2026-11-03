package com.game24.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.game24.tv.ui.Game24App
import com.game24.tv.ui.theme.Game24Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Game24Theme {
                Game24App()
            }
        }
    }
}
