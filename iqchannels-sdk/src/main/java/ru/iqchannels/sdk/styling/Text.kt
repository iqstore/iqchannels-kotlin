package ru.iqchannels.sdk.styling

import com.google.gson.annotations.SerializedName

class Text(
	val color: Color?,
	@SerializedName("text_size")
	val textSize: Float?
)