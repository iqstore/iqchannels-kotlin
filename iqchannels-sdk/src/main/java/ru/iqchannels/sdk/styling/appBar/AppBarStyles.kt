package ru.iqchannels.sdk.styling.chat

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

class AppBarStyles(
	val background: Color?,
	@SerializedName("back_button")
	val backButton: Color?,
	@SerializedName("status_label")
	val statusLabel: Text?,
	@SerializedName("title_label")
	val titleLabel: Text?
)