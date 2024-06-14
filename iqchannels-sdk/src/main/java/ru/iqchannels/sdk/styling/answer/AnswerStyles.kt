package ru.iqchannels.sdk.styling.answer

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

class AnswerStyles(
	@SerializedName("text_sender")
	val textSender: Text?,
	@SerializedName("text_message")
	val textMessage: Text?,
	@SerializedName("background_text_up_message")
	val backgroundTextUpMessage: Color?,
	@SerializedName("text_answer")
	val textAnswer: Text?,
	@SerializedName("icon_cancel")
	val iconCancel: String?,
	@SerializedName("left_line")
	val leftLine: Color?
)