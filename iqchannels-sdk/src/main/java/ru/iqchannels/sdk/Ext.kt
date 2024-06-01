package ru.iqchannels.sdk

import android.content.Context
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

fun Context.isSystemInDarkMode() = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
	Configuration.UI_MODE_NIGHT_NO -> false // Night mode is not active, we're using the light theme
	Configuration.UI_MODE_NIGHT_YES -> true // Night mode is active, we're using dark theme
	else -> false
}

fun TextView.applyIQStyles(text: Text?) {
	text?.color?.getColorInt(context)?.let {
		setTextColor(it)
	}

	text?.textSize?.let {
		setTextSize(TypedValue.COMPLEX_UNIT_SP, it)
	}
}

fun ViewGroup.setBackgroundDrawable(color: Color?, @DrawableRes drawableRes: Int) {
	color?.let {
		background = ContextCompat.getDrawable(context, drawableRes)
			?.apply {
				colorFilter =
					PorterDuffColorFilter(color.getColorInt(context), PorterDuff.Mode.SRC_ATOP)
			}
	} ?: run {
		setBackgroundResource(drawableRes)
	}
}