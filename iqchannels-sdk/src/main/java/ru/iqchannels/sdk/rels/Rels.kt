/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.rels

import java.util.*
import ru.iqchannels.sdk.schema.ChatEvent
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.Client
import ru.iqchannels.sdk.schema.ClientAuth
import ru.iqchannels.sdk.schema.FileImageSize
import ru.iqchannels.sdk.schema.FileType
import ru.iqchannels.sdk.schema.RelationMap
import ru.iqchannels.sdk.schema.Relations
import ru.iqchannels.sdk.schema.UploadedFile
import ru.iqchannels.sdk.schema.User

class Rels(address: String) {
	private val address: String

	init {
		var validAddress = address
		if (validAddress.endsWith("/")) {
			validAddress = validAddress.substring(0, validAddress.length - 1)
		}
		this.address = validAddress
	}

	// RelationMap
	fun map(rels: Relations): RelationMap {
		val map = RelationMap()

		rels.ChatMessages?.let {
			for (message in it) {
				map.ChatMessages[message.Id] = message
			}
		}

		rels.Clients?.let {
			for (client in it) {
				map.Clients[client.Id] = client
			}
		}

		rels.Files?.let {
			for (file in it) {
				map.Files[file.Id] = file
			}
		}

		rels.Ratings?.let {
			for (rating in it) {
				map.Ratings[rating.Id] = rating
			}
		}

		rels.Users?.let {
			for (user in it) {
				map.Users[user.Id] = user
			}
		}

		chatMessages(map.ChatMessages.values, map)
		clients(map.Clients.values, map)
		files(map.Files.values, map)
		users(map.Users.values, map)
		return map
	}

	// Clients
	fun client(client: Client?, map: RelationMap?) {}
	fun clients(clients: Collection<Client?>, map: RelationMap?) {
		for (client in clients) {
			client(client, map)
		}
	}

	fun clientAuth(auth: ClientAuth, map: RelationMap?) {
		client(auth.Client, map)
	}

	// Chat messages
	fun chatMessage(message: ChatMessage, map: RelationMap) {
		if (message.ClientId != null) {
			message.Client = map.Clients[message.ClientId]
		}
		if (message.UserId != null) {
			message.User = map.Users[message.UserId]
		}
		if (message.FileId != null) {
			message.File = map.Files[message.FileId]
		}
		if (message.RatingId != null) {
			message.Rating = map.Ratings[message.RatingId]
		}
		if (message.CreatedAt > 0) {
			message.Date = Date(message.CreatedAt)
		}
	}

	fun chatMessages(messages: Collection<ChatMessage>, map: RelationMap) {
		for (message in messages) {
			chatMessage(message, map)
		}
	}

	// Chat events
	fun chatEvent(event: ChatEvent, map: RelationMap) {
		if (event.ClientId != null) {
			event.Client = map.Clients[event.ClientId]
		}
		if (event.UserId != null) {
			event.User = map.Users[event.UserId]
		}
		if (event.MessageId != null) {
			event.Message = map.ChatMessages[event.MessageId]
		}
	}

	fun chatEvents(events: Collection<ChatEvent>, map: RelationMap) {
		for (event in events) {
			chatEvent(event, map)
		}
	}

	// Files
	fun file(file: UploadedFile, map: RelationMap?) {
		file.Url = fileUrl(file.Id)
		if (file.Type == FileType.IMAGE) {
			file.ImagePreviewUrl = fileImageUrl(file.Id, FileImageSize.PREVIEW)
			file.imageUrl = fileImageUrl(file.Id, FileImageSize.ORIGINAL)
		}
	}

	private fun fileUrl(fileId: String): String {
		return String.format("%s/public/api/v1/files/get/%s", address, fileId)
	}

	private fun fileImageUrl(fileId: String?, size: FileImageSize): String {
		return String.format("%s/public/api/v1/files/image/%s?size=%s", address, fileId, size)
	}

	fun files(files: Collection<UploadedFile>, map: RelationMap?) {
		for (file in files) {
			file(file, map)
		}
	}

	// Users
	fun user(user: User, map: RelationMap?) {
		if (user.AvatarId != null) {
			user.AvatarUrl = fileImageUrl(user.AvatarId, FileImageSize.AVATAR)
		}
	}

	fun users(users: Collection<User>, map: RelationMap?) {
		for (user in users) {
			user(user, map)
		}
	}
}
