package ru.iqchannels.sdk.styling.messages

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.ButtonStyles
import ru.iqchannels.sdk.styling.ContainerStyles
import ru.iqchannels.sdk.styling.Text

class RatingStyles(
	@SerializedName("background_container")
	val backgroundContainer: ContainerStyles?,
	@SerializedName("rating_title")
	val ratingTitle: Text?,
	@SerializedName("full_star")
	val fullStar: String?,
	@SerializedName("empty_star")
	val emptyStar: String?,
	@SerializedName("sent_rating")
	val sentRating: ButtonStyles?,
	@SerializedName("answer_button")
	val answerButton: ButtonStyles?,
	@SerializedName("scale_button")
	val scaleButton: ButtonStyles?,
	@SerializedName("scale_min_text")
	val scaleMinText: Text?,
	@SerializedName("scale_max_text")
	val scaleMaxText: Text?,
	@SerializedName("input_background")
	val inputBackground: ContainerStyles?,
	@SerializedName("input_text")
	val inputText: Text?,
	@SerializedName("feedback_thanks_text")
	val feedbackThanksText: Text?
)
