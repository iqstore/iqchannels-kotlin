package ru.iqchannels.sdk.styling

import com.google.gson.annotations.SerializedName

class Border(
	val size: Float?,
	val color: Color?,
	@SerializedName("border-radius")
	val borderRadius: Float?
) {
}