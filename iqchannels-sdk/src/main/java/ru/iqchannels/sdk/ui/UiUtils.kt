package ru.iqchannels.sdk.ui

import android.content.res.Resources
import android.util.TypedValue
import kotlin.math.roundToInt

object UiUtils {

	fun toPx(dp: Int): Int {
		return TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
			Resources.getSystem().displayMetrics
		).roundToInt()
	}
}
