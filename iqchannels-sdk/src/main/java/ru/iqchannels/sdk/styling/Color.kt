package ru.iqchannels.sdk.styling

import android.content.Context
import android.graphics.Color
import ru.iqchannels.sdk.isSystemInDarkMode

class Color(
	val light: String?,
	val dark: String?
) {

	fun getColor(context: Context): String? {
		return when(IQStyles.iqChannelsStyles?.theme) {
			Theme.DARK -> dark
			Theme.LIGHT -> light
			else -> {
				return if (context.isSystemInDarkMode()) {
					dark
				} else {
					light
				}
			}
		}
	}

	fun getColorInt(context: Context): Int? {
		return Color.parseColor(getColor(context))
	}
}