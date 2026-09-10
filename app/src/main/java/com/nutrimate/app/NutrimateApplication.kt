package com.nutrimate.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Hilt generates the DI graph here; all feature components resolve
 * through modules defined in the `di` package.
 */
@HiltAndroidApp
class NutrimateApplication : Application()