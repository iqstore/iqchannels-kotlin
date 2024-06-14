package ru.iqchannels.sdk.schema

class ChatEventQuery {
	var LastEventId: Long? = null
	var Limit: Int? = null
	var ChatType: String = ru.iqchannels.sdk.domain.models.ChatType.REGULAR.name.lowercase()

	constructor()
	constructor(lastEventId: Long?) {
		LastEventId = lastEventId
	}
}
