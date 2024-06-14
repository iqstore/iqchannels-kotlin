package ru.iqchannels.sdk.schema

class ChatEvent {
	var Id: Long = 0
	var Type: ChatEventType? = null
	var ChatId: Long = 0
	var Public = false
	var Transitive = false

	var SessionId: Long? = null
	var MessageId: Long? = null
	var MemberId: Long? = null

	var Actor: ActorType? = null
	var ClientId: Long? = null
	var UserId: Long? = null
	var Messages: List<ChatMessage>? = null
	var CreatedAt: Long = 0

	var NextChannelName: String? = null

	// Local
	var Client: Client? = null
	var User: User? = null
	var Message: ChatMessage? = null
}
