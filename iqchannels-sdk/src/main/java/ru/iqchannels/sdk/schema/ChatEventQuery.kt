package ru.iqchannels.sdk.schema

class ChatEventQuery {
	var LastEventId: Long? = null
	var Limit: Int? = null

	constructor()
	constructor(lastEventId: Long?) {
		LastEventId = lastEventId
	}
}
