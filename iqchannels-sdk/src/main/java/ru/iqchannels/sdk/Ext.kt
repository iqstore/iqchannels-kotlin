package ru.iqchannels.sdk

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import ru.iqchannels.sdk.styling.Border
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.ContainerStyles
import ru.iqchannels.sdk.styling.Text
import ru.iqchannels.sdk.ui.widgets.toPx

fun Context.isSystemInDarkMode() =
	when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
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

	text?.textStyle?.let { style ->
		val isBold = style.bold ?: false
		val isItalic = style.italic ?: false
		when {
			isBold && isItalic -> setTypeface(typeface, Typeface.BOLD_ITALIC)
			isBold -> setTypeface(typeface, Typeface.BOLD)
			isItalic -> setTypeface(typeface, Typeface.ITALIC)
			else -> setTypeface(typeface, Typeface.NORMAL)
		}
	}

	text?.textAlignment?.let { alignment ->
		textAlignment = when (alignment) {
			"center" -> View.TEXT_ALIGNMENT_CENTER
			"left" -> View.TEXT_ALIGNMENT_TEXT_START
			"right" -> View.TEXT_ALIGNMENT_TEXT_END
			else -> View.TEXT_ALIGNMENT_INHERIT
		}
	}
}

fun View.setBackgroundDrawable(
	style: ContainerStyles?,
	@DrawableRes drawableRes: Int?
) {
	style?.let {
		background = GradientDrawable().apply {
			setColor(style.color?.getColorInt(context) ?: ContextCompat.getColor(context, 0))
			setStroke(
				style.border?.size?.toPx?.roundToInt() ?: 0,
				style.border?.color?.getColorInt(context) ?: ContextCompat.getColor(context, 0)
			)
			cornerRadius = style.border?.borderRadius?.toPx ?: 12.toPx
		}
	} ?: run {
		if (drawableRes != null) setBackgroundResource(drawableRes)
	}
}

fun View.setBackgroundStyle(
	color: Color?,
	border: Border?,
	defaultBgColor: Int,
	defaultBorderColor: Int,
	defaultBorderWidth: Int,
	defaultBorderRadius: Float
) {
	background = GradientDrawable().apply {
		setColor(color?.getColorInt(context) ?: ContextCompat.getColor(context, defaultBgColor))
		setStroke(
			border?.size?.toPx?.roundToInt() ?: defaultBorderWidth,
			border?.color?.getColorInt(context) ?: ContextCompat.getColor(context, defaultBorderColor)
		)
		cornerRadius = border?.borderRadius?.toPx ?: defaultBorderRadius.toPx
	}
}