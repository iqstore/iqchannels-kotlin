package ru.iqchannels.sdk.ui

import android.content.res.Resources
import android.util.TypedValue
import com.google.gson.Gson
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.schema.PollOptionType
import ru.iqchannels.sdk.schema.Rating
import ru.iqchannels.sdk.schema.RatingState
import java.lang.Integer.max
import kotlin.math.roundToInt

const val DEFAULT_RATING_MAX_VALUE = 5

object UiUtils {

	fun toPx(dp: Int): Int {
		return TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
			Resources.getSystem().displayMetrics
		).roundToInt()
	}

	fun getRatingScaleMaxValue(rating: Rating): Int {
		var maxValue = DEFAULT_RATING_MAX_VALUE
		if (rating.State == RatingState.FINISHED && rating.RatingPoll != null) {
			rating.RatingPoll?.Questions?.forEach { question ->
				if (question.AsTicketRating == true && question.Type == PollOptionType.SCALE) {
					maxValue = max(maxValue, question.Scale?.ToValue!!)
				}
			}
		}
		return maxValue
	}
}
