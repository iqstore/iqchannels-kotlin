package ru.iqchannels.sdk.domain.models

import ru.iqchannels.sdk.schema.ChatMessage

class Channel(
	val name: String,
	val lastMessage: ChatMessage?,
	val chatType: ChatType
) {
}