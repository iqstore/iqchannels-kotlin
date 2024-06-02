package ru.iqchannels.sdk.styling

import ru.iqchannels.sdk.styling.answer.AnswerStyles
import ru.iqchannels.sdk.styling.chat.ChatStyles
import ru.iqchannels.sdk.styling.messages.MessagesStyles

class IQChannelsStyles(
	val chat: ChatStyles?,
	val messages: MessagesStyles?,
	val answer: AnswerStyles?,
	val theme: Theme?
)