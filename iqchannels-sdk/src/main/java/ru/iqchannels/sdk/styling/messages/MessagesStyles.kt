package ru.iqchannels.sdk.styling.messages

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

class MessagesStyles(
	@SerializedName("background_operator")
	val backgroundOperator: Color?,
	@SerializedName("background_client")
	val backgroundClient: Color?,
	@SerializedName("text_operator")
	val textOperator: Text?,
	@SerializedName("text_client")
	val textClient: Text?,
	@SerializedName("text_time")
	val textTime: Text?,
	@SerializedName("text_up")
	val textUp: Text?,
)