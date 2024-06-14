package ru.iqchannels.sdk.styling.buttons

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Border
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

class SingleChoiceBtnStyles(
	@SerializedName("background_button")
	val backgroundButton: Color?,
	@SerializedName("border_button")
	val borderButton: Border?,
	@SerializedName("text_button")
	val textButton: Text?,
	@SerializedName("background_IVR")
	val backgroundIvr: Color?,
	@SerializedName("border_IVR")
	val borderIvr: Border?,
	@SerializedName("text_IVR")
	val textIvr: Text?,
)