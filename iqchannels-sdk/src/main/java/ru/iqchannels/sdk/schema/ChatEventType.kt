package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

enum class ChatEventType {
	@SerializedName("")
	INVALID,
	@SerializedName("chat_created")
	CHAT_CREATED,
	@SerializedName("chat_opened")
	CHAT_OPENED,
	@SerializedName("chat_closed")
	CHAT_CLOSED,
	@SerializedName("typing")
	TYPING,
	@SerializedName("message_created")
	MESSAGE_CREATED,
	@SerializedName("rating_ignored")
	RATING_IGNORED,
	@SerializedName("system_message_created")
	SYSTEM_MESSAGE_CREATED,
	@SerializedName("message_received")
	MESSAGE_RECEIVED,
	@SerializedName("message_read")
	MESSAGE_READ,
	@SerializedName("delete-messages")
	MESSAGE_DELETED,
	@SerializedName("chat-channel-change")
	CHAT_CHANNEL_CHANGE,
	@SerializedName("file_updated")
	FILE_UPDATED
}
