package ru.iqchannels.sdk.schema;

import androidx.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ChatMessageForm {
    public long LocalId;
    public String Payload; // ChatPayloadType
    public String Text;
    @Nullable public String FileId;
    @Nullable public Long ReplyToMessageId;
    @Nullable public String BotpressPayload;

    public ChatMessageForm() {}

    public static ChatMessageForm text(long localId, String text, Long replyToMessageId) {
        ChatMessageForm form = new ChatMessageForm();
        form.LocalId = localId;
        form.Payload = ChatPayloadType.TEXT;
        form.Text = text;
        form.ReplyToMessageId = replyToMessageId;
        return form;
    }

    public static ChatMessageForm file(long localId, String fileId, Long replyToMessageId) {
        ChatMessageForm form = new ChatMessageForm();
        form.LocalId = localId;
        form.Payload = ChatPayloadType.FILE;
        form.FileId = fileId;
        form.ReplyToMessageId = replyToMessageId;
        return form;
    }

    public static ChatMessageForm payloadReply(long localId, String text, String botpressPayload) {
        ChatMessageForm form = new ChatMessageForm();
        form.LocalId = localId;
        form.Payload = ChatPayloadType.TEXT;
        form.Text = text;
        form.BotpressPayload = botpressPayload;
        return form;
    }
}
