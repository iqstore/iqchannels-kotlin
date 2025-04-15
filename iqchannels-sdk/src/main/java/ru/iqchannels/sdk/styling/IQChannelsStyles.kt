package ru.iqchannels.sdk.styling

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.answer.AnswerStyles
import ru.iqchannels.sdk.styling.buttons.SingleChoiceBtnStyles
import ru.iqchannels.sdk.styling.chat.ChatStyles
import ru.iqchannels.sdk.styling.chat.SignupStyles
import ru.iqchannels.sdk.styling.error.ErrorStyles
import ru.iqchannels.sdk.styling.messages.MessagesFileStyles
import ru.iqchannels.sdk.styling.messages.MessagesStyles
import ru.iqchannels.sdk.styling.messages.RatingStyles
import ru.iqchannels.sdk.styling.sending.ToolsToMessage

class IQChannelsStyles(
	val signup: SignupStyles?,
	val chat: ChatStyles?,
	val messages: MessagesStyles?,
	val answer: AnswerStyles?,
	@SerializedName("messages_file")
	val messageFile: MessagesFileStyles?,
	val rating: RatingStyles?,
	@SerializedName("tools_to_message")
	val toolsToMessage: ToolsToMessage?,
	val error: ErrorStyles?,
	@SerializedName("single-choice")
	val singleChoiceBtnStyles: SingleChoiceBtnStyles?,
	val theme: Theme?
)