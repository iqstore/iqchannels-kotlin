package ru.iqchannels.sdk.ui.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.localization.IQChannelsLanguage

class TopNotificationWidget @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

	private var job: Job? = null

	init {
		inflate(context, R.layout.layout_top_notification, this)

		findViewById<ImageView>(R.id.iv_close)?.run {
			setOnClickListener {
				hide()
			}
		}
		findViewById<TextView>(R.id.text)?.run {
			text = IQChannelsLanguage.iqChannelsLanguage.textCopied
		}
	}

	fun show() {
		if (job == null || job?.isActive == false) {
			job = CoroutineScope(Dispatchers.Main).launch {
				delay(3000)
				hide()
			}

			animate().translationY(8.toPx).alpha(1.0f).setListener(object : AnimatorListenerAdapter() {
				override fun onAnimationStart(animation: Animator) {
					super.onAnimationStart(animation)
					visibility = View.VISIBLE
				}
			})
		}
	}

	fun hide() {
		job?.cancel()
		animate()
			.translationY(0f)
			.alpha(0.0f)
			.setListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					super.onAnimationCancel(animation)
					this@TopNotificationWidget.visibility = View.INVISIBLE
				}
			})
	}

	fun destroy() {
		job?.cancel()
		job = null
	}
}