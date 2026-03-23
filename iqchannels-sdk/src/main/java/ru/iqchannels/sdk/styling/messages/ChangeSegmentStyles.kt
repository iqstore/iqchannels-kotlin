package ru.iqchannels.sdk.styling.messages

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.ContainerStyles
import ru.iqchannels.sdk.styling.Text

class ChangeSegmentStyles(
	@SerializedName("background_container")
	val backgroundContainer: ContainerStyles?,
	val title: Text?,
	@SerializedName("background_button")
	val backgroundButton: ContainerStyles?,
	@SerializedName("text_button")
	val textButton: Text?,
)
