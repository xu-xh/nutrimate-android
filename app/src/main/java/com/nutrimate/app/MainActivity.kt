package com.nutrimate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nutrimate.app.presentation.navigation.NutrimateNavHost
import com.nutrimate.app.presentation.theme.NutrimateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutrimateTheme {
                NutrimateNavHost()
            }
        }
    }
}