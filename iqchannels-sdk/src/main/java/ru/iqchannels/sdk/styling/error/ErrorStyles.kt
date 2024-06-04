package ru.iqchannels.sdk.styling.error

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Text

class ErrorStyles(
	@SerializedName("title_error")
	val titleError: Text?,
	@SerializedName("text_error")
	val textError: Text?,
	@SerializedName("icon_error")
	val iconError: String?
)