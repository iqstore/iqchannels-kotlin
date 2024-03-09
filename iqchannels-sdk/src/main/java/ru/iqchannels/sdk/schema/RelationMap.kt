package ru.iqchannels.sdk.schema

class RelationMap {
	val Clients: Map<Long, Client>
	val ChatMessages: Map<Long, ChatMessage>
	val Files: Map<String, UploadedFile>
	val Ratings: Map<Long, Rating>
	val Users: Map<Long, User>

	init {
		Clients = HashMap()
		ChatMessages = HashMap()
		Files = HashMap()
		Ratings = HashMap()
		Users = HashMap()
	}
}
