/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.app

import ru.iqchannels.sdk.schema.ChatEvent
import ru.iqchannels.sdk.schema.ChatMessage

interface MessagesListener {
	fun messagesLoaded(messages: List<ChatMessage>)
	fun messagesException(e: Exception)
	fun messagesCleared()
	fun messageReceived(message: ChatMessage)
	fun messageSent(message: ChatMessage)
	fun messageUploaded(message: ChatMessage)
	fun messageUpdated(message: ChatMessage)
	fun eventTyping(event: ChatEvent)
	fun messageCancelled(message: ChatMessage)
	fun messageDeleted(message: ChatMessage)
	fun eventChangeChannel(channel: String)
	fun ratingRenderQuestion()
}
