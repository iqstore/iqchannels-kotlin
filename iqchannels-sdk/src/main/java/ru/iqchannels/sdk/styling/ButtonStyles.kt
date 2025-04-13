package ru.iqchannels.sdk.styling

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Border
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

class ButtonStyles(
	@SerializedName("background_enabled")
	val backgroundEnabled: ContainerStyles?,
	@SerializedName("background_disabled")
	val backgroundDisabled: ContainerStyles?,
	@SerializedName("text_enabled")
	val textEnabled: Text?,
	@SerializedName("text_disabled")
	val textDisabled: Text?
)