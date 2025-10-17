package ru.iqchannels.sdk.room

import androidx.room.*
import com.google.gson.Gson
import java.util.*
import ru.iqchannels.sdk.schema.*
import java.io.File
import ru.iqchannels.sdk.schema.Client as ClientSchema
import ru.iqchannels.sdk.schema.User as UserSchema
import ru.iqchannels.sdk.schema.Rating as RatingSchema

@Entity(
    tableName = "messages",
    indices = [Index(value = ["localId"], unique = true)]
)
data class DatabaseMessage(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val id: Long = 0,
    var chatId: Long = 0,
    var sessionId: Long = 0,
    val localId: Long,
    val eventId: Long?,
    val isPublic: Boolean,
    val author: ActorType?,
    val clientId: Long?,
    val userId: Long?,
    val payload: String?,
    val text: String?,
    val fileId: String?,
    val ratingId: Long?,
    val replyToMessageId: Long?,
    val isDropDown: Boolean?,
    val disableFreeText: Boolean?,
    @TypeConverters(SingleChoiceListConverter::class) val singleChoices: List<SingleChoice>?,
    @TypeConverters(ActionListConverter::class) val actions: List<Action>?,
    val received: Boolean,
    val read: Boolean,
    val createdAt: Long,
    val receivedAt: Long?,
    val readAt: Long?,
    val my: Boolean,
    val sending: Boolean,
    val system: Boolean,
    val client: String?,
    val user: String?,
    val file: String?,
    val rating: String?,
    val date: Long?,
    val newMsgHeader: Boolean,
    @TypeConverters(FileConverter::class) var upload: File?,
    val error: Boolean
)


fun ChatMessage.toDatabaseMessage(): DatabaseMessage {
    val gson = Gson()
    return DatabaseMessage(
        id = this.Id,
        chatId = this.ChatId,
        sessionId = this.SessionId,
        localId = this.LocalId,
        eventId = this.EventId,
        isPublic = this.Public,
        author = this.Author,
        clientId = this.ClientId,
        userId = this.UserId,
        payload = this.Payload,
        text = this.Text,
        fileId = this.FileId,
        ratingId = this.RatingId,
        replyToMessageId = this.ReplyToMessageId,
        isDropDown = this.IsDropDown,
        disableFreeText = this.DisableFreeText,
        singleChoices = this.SingleChoices,
        actions = this.Actions,
        received = this.Received,
        read = this.Read,
        createdAt = this.CreatedAt,
        receivedAt = this.ReceivedAt,
        readAt = this.ReadAt,
        my = this.My,
        sending = this.Sending,
        system = this.System,
        client = gson.toJson(this.Client),
        user = gson.toJson(this.User),
        file = gson.toJson(this.File),
        rating = gson.toJson(this.Rating),
        date = this.Date?.time,
        newMsgHeader = this.NewMsgHeader,
        upload = this.Upload,
        error = this.Error
    )
}


fun DatabaseMessage?.toChatMessage(): ChatMessage? {
    if (this == null) return null
    val gson = Gson()

    return ChatMessage().apply {
        Id = this@toChatMessage.id
        ChatId = this@toChatMessage.chatId
        SessionId = this@toChatMessage.sessionId
        LocalId = this@toChatMessage.localId
        EventId = this@toChatMessage.eventId
        Public = this@toChatMessage.isPublic
        Author = this@toChatMessage.author
        ClientId = this@toChatMessage.clientId
        UserId = this@toChatMessage.userId
        Payload = this@toChatMessage.payload
        Text = this@toChatMessage.text
        FileId = this@toChatMessage.fileId
        RatingId = this@toChatMessage.ratingId
        ReplyToMessageId = this@toChatMessage.replyToMessageId
        IsDropDown = this@toChatMessage.isDropDown
        DisableFreeText = this@toChatMessage.disableFreeText
        SingleChoices = this@toChatMessage.singleChoices
        Actions = this@toChatMessage.actions
        Received = this@toChatMessage.received
        Read = this@toChatMessage.read
        CreatedAt = this@toChatMessage.createdAt
        ReceivedAt = this@toChatMessage.receivedAt
        ReadAt = this@toChatMessage.readAt
        My = this@toChatMessage.my
        Sending = this@toChatMessage.sending
        System = this@toChatMessage.system
        Client = gson.fromJson(this@toChatMessage.client, ClientSchema::class.java)
        User = gson.fromJson(this@toChatMessage.user, UserSchema::class.java)
        File = gson.fromJson(this@toChatMessage.file, UploadedFile::class.java)
        Rating = gson.fromJson(this@toChatMessage.rating, RatingSchema::class.java)
        Date = this@toChatMessage.date?.let { Date(it) }
        NewMsgHeader = this@toChatMessage.newMsgHeader
        Upload = this@toChatMessage.upload
        Error = this@toChatMessage.error
    }
}

