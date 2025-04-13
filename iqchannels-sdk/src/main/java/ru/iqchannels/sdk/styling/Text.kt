package ru.iqchannels.sdk.styling

import com.google.gson.annotations.SerializedName

class Text(
	val color: Color?,
	@SerializedName("text_size")
	val textSize: Float?,
	@SerializedName("text_align")
	val textAlignment: String?,
	@SerializedName("text_style")
	val textStyle: TextStyles?
)