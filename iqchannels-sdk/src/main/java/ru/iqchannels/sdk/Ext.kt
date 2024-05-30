package ru.iqchannels.sdk

import android.content.Context
import android.content.res.Configuration

fun Context.isSystemInDarkMode() = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
	Configuration.UI_MODE_NIGHT_NO -> false // Night mode is not active, we're using the light theme
	Configuration.UI_MODE_NIGHT_YES -> true // Night mode is active, we're using dark theme
	else -> false
}