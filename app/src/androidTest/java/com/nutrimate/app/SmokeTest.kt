package com.nutrimate.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runtime smoke test executed on a real emulator (CI job "instrumented").
 *
 * This is the first test that runs the actual APK on Android: it verifies the
 * activity host + Compose render after a fresh install (onboarding shown),
 * which exercises the Hilt graph, Room + DataStore wiring and the navigation
 * shell at runtime.
 *
 * Notes:
 *  - A fresh emulator installation has an empty DataStore, so the app must
 *    land on the Onboarding wizard (step 1 of 3).
 *  - Keep it deterministic: we only assert on stable text that does not
 *    depend on the clock or AI providers.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshInstallShowsOnboardingStep1() {
        composeRule.onNodeWithText("第 1 / 3 步").assertIsDisplayed()
    }

    @Test
    fun onboardingTitleShown() {
        composeRule.onNodeWithText("关于你").assertIsDisplayed()
    }
}