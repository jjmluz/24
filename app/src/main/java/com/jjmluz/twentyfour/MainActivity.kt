package com.jjmluz.twentyfour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jjmluz.twentyfour.ui.Game24App
import com.jjmluz.twentyfour.ui.theme.Game24Theme

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
