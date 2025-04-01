package ru.iqchannels.sdk.schema

import com.google.gson.Gson
import java.io.File
import java.util.*
import ru.iqchannels.sdk.http.HttpRequest

class ChatMessage {
	var Id: Long = 0
	var UID: String? = null
	var ChatId: Long = 0
	var SessionId: Long = 0
	var LocalId: Long = 0
	var EventId: Long? = null
	var Public = false

	// Author
	var Author: ActorType? = null
	var ClientId: Long? = null
	var UserId: Long? = null

	// Payload
	var Payload: String? = null // ChatPayloadType
	var Text: String? = null
	var FileId: String? = null
	var NoticeId: String? = null
	var RatingId: Long? = null
	var ReplyToMessageId: Long? = null
	var IsDropDown: Boolean? = null
	var DisableFreeText: Boolean? = null
	var SingleChoices: List<SingleChoice>? = null
	var Actions: List<Action>? = null

	// Flags
	var Received = false
	var Read = false
	var CreatedAt: Long = 0
	var ReceivedAt: Long? = null
	var ReadAt: Long? = null

	// Transient
	var My = false

	// Local
	var Sending = false
	var System = false
	var Client: Client? = null
	var User: User? = null
	var File: UploadedFile? = null
	var Rating: Rating? = null
	var Date: Date? = null
	var newMsgHeader: Boolean = false

	override fun toString(): String {
		return Gson().toJson(this)
	}

	// Upload
	@Transient
	var Upload: File? = null

	@Transient
	var UploadExc: Exception? = null

	@Transient
	var UploadRequest: HttpRequest? = null

	@Transient
	var UploadProgress = 0

	constructor()
	constructor(client: Client, localId: Long, text: String?) : this(client, localId) {
		Payload = ChatPayloadType.TEXT
		Text = text
	}

	constructor(client: Client, localId: Long, text: String, file: File, replyToMessageId: Long?) : this(
		client,
		localId
	) {
		Payload = ChatPayloadType.FILE
		Text = text
		Upload = file
		ReplyToMessageId = replyToMessageId
	}

	constructor(client: Client, localId: Long) {
		LocalId = localId
		Public = true

		// Author
		Author = ActorType.CLIENT
		ClientId = client.Id
		val now = Date()
		CreatedAt = now.time

		// Transitive
		My = true

		// Local
		Client = client
		Date = now
	}

	constructor(user: User, localId: Long) {
		LocalId = localId
		Public = true

		// Author
		Author = ActorType.USER
		UserId = user.Id
		val now = Date()
		CreatedAt = now.time

		// Transitive
		My = false

		// Local
		User = user
		Date = now
	}
}
