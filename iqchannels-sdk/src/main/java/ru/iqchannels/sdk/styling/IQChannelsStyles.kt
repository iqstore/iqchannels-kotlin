package ru.iqchannels.sdk.styling

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.answer.AnswerStyles
import ru.iqchannels.sdk.styling.chat.ChatStyles
import ru.iqchannels.sdk.styling.messages.MessagesFileStyles
import ru.iqchannels.sdk.styling.messages.MessagesStyles

class IQChannelsStyles(
	val chat: ChatStyles?,
	val messages: MessagesStyles?,
	val answer: AnswerStyles?,
	@SerializedName("messages_file")
	val messageFile: MessagesFileStyles?,
	val theme: Theme?
)