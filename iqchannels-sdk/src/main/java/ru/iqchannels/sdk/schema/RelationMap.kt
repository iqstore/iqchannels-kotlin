package ru.iqchannels.sdk.schema

class RelationMap {
	val Clients: MutableMap<Long, Client>
	val ChatMessages: MutableMap<Long, ChatMessage>
	val Files: MutableMap<String, UploadedFile>
	val Ratings: MutableMap<Long, Rating>
	val Users: MutableMap<Long, User>

	init {
		Clients = HashMap()
		ChatMessages = HashMap()
		Files = HashMap()
		Ratings = HashMap()
		Users = HashMap()
	}
}
