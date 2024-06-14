package ru.iqchannels.sdk.styling.chat

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

class ChatStyles(
	val background: Color?,
	@SerializedName("date_text")
	val dateText: Text?,
	@SerializedName("chat_history")
	val chatHistory: Color?,
	@SerializedName("icon_operator")
	val iconOperator: String?,
	@SerializedName("system_text")
	val systemText: Text?
)