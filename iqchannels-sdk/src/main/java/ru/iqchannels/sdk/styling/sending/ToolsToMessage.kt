package ru.iqchannels.sdk.styling.sending

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.ContainerStyles
import ru.iqchannels.sdk.styling.Text

class ToolsToMessage(
	@SerializedName("icon_sent")
	val iconSent: String?,
	@SerializedName("icon_clip")
	val iconClip: String?,
	@SerializedName("background_icon")
	val backgroundIcon: Color?,
	@SerializedName("background_chat")
	val backgroundChat: ContainerStyles?,
	@SerializedName("text_chat")
	val textChat: Text?
)