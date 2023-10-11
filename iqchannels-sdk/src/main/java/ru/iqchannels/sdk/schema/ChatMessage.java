package ru.iqchannels.sdk.schema;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Date;
import java.util.List;

import ru.iqchannels.sdk.http.HttpRequest;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ChatMessage {
    public long Id;
    @Nullable public String UID;
    public long ChatId;
    public long SessionId;
    public long LocalId;
    @Nullable public Long EventId;
    public boolean Public;

    // Author
    public ActorType Author;
    @Nullable public Long ClientId;
    @Nullable public Long UserId;

    // Payload
    public String Payload; // ChatPayloadType
    public String Text;
    @Nullable public String FileId;
    @Nullable public String NoticeId;
    @Nullable public Long RatingId;

    @Nullable public Long ReplyToMessageId;

    @Nullable public Boolean IsDropDown;
    @Nullable public Boolean DisableFreeText;
    @Nullable public List<SingleChoice> SingleChoices;
    @Nullable public List<Action> Actions;

    // Flags
    public boolean Received;
    public boolean Read;

    public long CreatedAt;
    @Nullable public Long ReceivedAt;
    @Nullable public Long ReadAt;

    // Transient
    public boolean My;

    // Local
    public boolean Sending;
    @Nullable public Client Client;
    @Nullable public User User;
    @Nullable public UploadedFile File;
    @Nullable public Rating Rating;
    @Nullable public Date Date;

    // Upload
    @Nullable public transient File Upload;
    @Nullable public transient Exception UploadExc;
    @Nullable public transient HttpRequest UploadRequest;
    public transient int UploadProgress;

    public ChatMessage() {}

    public ChatMessage(@NonNull Client client, long localId, String text) {
        this(client, localId);

        Payload = ChatPayloadType.TEXT;
        Text = text;
    }

    public ChatMessage(@NonNull Client client, long localId, @NonNull File file, @Nullable Long replyToMessageId) {
        this(client, localId);

        Payload = ChatPayloadType.FILE;
        Text = file.getName();
        Upload = file;
        ReplyToMessageId = replyToMessageId;
    }

    public ChatMessage(@NonNull Client client, long localId) {
        LocalId = localId;
        Public = true;

        // Author
        Author = ActorType.CLIENT;
        ClientId = client.Id;

        Date now = new Date();
        CreatedAt = now.getTime();

        // Transitive
        My = true;

        // Local
        Client = client;
        Date = now;
    }

    public ChatMessage(@NonNull User user, long localId) {
        LocalId = localId;
        Public = true;

        // Author
        Author = ActorType.USER;
        UserId = user.Id;

        Date now = new Date();
        CreatedAt = now.getTime();

        // Transitive
        My = false;

        // Local
        User = user;
        Date = now;
    }
}
