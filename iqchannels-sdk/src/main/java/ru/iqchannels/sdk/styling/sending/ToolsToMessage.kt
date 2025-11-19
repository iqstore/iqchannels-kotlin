package ru.iqchannels.sdk.styling.sending

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.ContainerStyles
import ru.iqchannels.sdk.styling.Text

class ToolsToMessage(
	val background: Color?,
	@SerializedName("icon_sent")
	val iconSent: String?,
	@SerializedName("background_icon_sent")
	val backgroundIconSent: ContainerStyles?,
	@SerializedName("icon_clip")
	val iconClip: String?,
	@SerializedName("background_icon_clip")
	val backgroundIconClip: ContainerStyles?,
	@SerializedName("background_input")
	val backgroundInput: ContainerStyles?,
	@SerializedName("text_input")
	val textInput: Text?
)