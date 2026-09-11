package com.nutrimate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.presentation.navigation.NutrimateNavHost
import com.nutrimate.app.presentation.theme.NutrimateTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var profileRepository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutrimateTheme {
                var onboardingDone by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    profileRepository.observeOnboardingDone().collect { done ->
                        onboardingDone = done
                    }
                }
                NutrimateNavHost(onboardingDone = onboardingDone)
            }
        }
    }
}