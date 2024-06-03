package ru.iqchannels.sdk.styling.messages

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

class RatingStyles(
	@SerializedName("background_container")
	val backgroundContainer: Color?,
	@SerializedName("full_star")
	val fullStar: String?,
	@SerializedName("empty_star")
	val emptyStar: String?,
	@SerializedName("sent_rating")
	val sentRating: RateButton?
)

class RateButton(
	@SerializedName("color_enabled")
	val colorEnabled: Color?,
	@SerializedName("color_disabled")
	val colorDisabled: Color?,
	@SerializedName("text_enabled")
	val textEnabled: Text?,
	@SerializedName("text_disabled")
	val textDisabled: Text?
)