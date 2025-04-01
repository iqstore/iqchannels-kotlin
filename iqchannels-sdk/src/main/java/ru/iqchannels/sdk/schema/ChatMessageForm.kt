package ru.iqchannels.sdk.schema

class ChatMessageForm {
	var LocalId: Long = 0
	var Payload: String? = null // ChatPayloadType
	var Text: String? = null
	var FileId: String? = null
	var ReplyToMessageId: Long? = null
	var BotpressPayload: String? = null
	var ChatType: String = ru.iqchannels.sdk.domain.models.ChatType.REGULAR.name.lowercase()

	companion object {
		fun text(localId: Long, text: String?, replyToMessageId: Long?): ChatMessageForm {
			val form = ChatMessageForm()
			form.LocalId = localId
			form.Payload = ChatPayloadType.TEXT
			form.Text = text
			form.ReplyToMessageId = replyToMessageId
			return form
		}

		fun file(localId: Long, text: String?, fileId: String?, replyToMessageId: Long?): ChatMessageForm {
			val form = ChatMessageForm()
			form.LocalId = localId
			form.Payload = ChatPayloadType.FILE
			form.FileId = fileId
			form.Text = text
			form.ReplyToMessageId = replyToMessageId
			return form
		}

		fun payloadReply(localId: Long, text: String?, botpressPayload: String?): ChatMessageForm {
			val form = ChatMessageForm()
			form.LocalId = localId
			form.Payload = ChatPayloadType.TEXT
			form.Text = text
			form.BotpressPayload = botpressPayload
			return form
		}
	}
}
