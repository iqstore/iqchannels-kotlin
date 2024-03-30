package ru.iqchannels.sdk.ui.widgets

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.schema.SingleChoice
import ru.iqchannels.sdk.ui.UiUtils

class DropDownButton @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

	private var singleChoice: SingleChoice? = null

	init {
		setBackgroundResource(R.drawable.bg_single_choice_btn_dropdown)
		setTextColor(ContextCompat.getColor(getContext(), R.color.drop_down_btn_text))
		setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
		isAllCaps = false
		elevation = 0f
		val vertical = UiUtils.toPx(4)
		val horizontal = UiUtils.toPx(4)
		gravity = Gravity.CENTER
		setPadding(horizontal, vertical, horizontal, vertical)
	}

	fun setSingleChoice(singleChoice: SingleChoice) {
		this.singleChoice = singleChoice
		text = singleChoice.title
		val lp = ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			UiUtils.toPx(36)
		)
		layoutParams = lp
	}
}
