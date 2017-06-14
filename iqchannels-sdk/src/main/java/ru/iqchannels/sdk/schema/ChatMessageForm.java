package ru.iqchannels.sdk.schema;

import android.support.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ChatMessageForm {
    public long LocalId;
    public ChatPayloadType Payload;
    public String Text;
    @Nullable public String FileId;

    public ChatMessageForm() {}

    public static ChatMessageForm text(long localId, String text) {
        ChatMessageForm form = new ChatMessageForm();
        form.LocalId = localId;
        form.Payload = ChatPayloadType.TEXT;
        form.Text = text;
        return form;
    }

    public static ChatMessageForm file(long localId, String fileId) {
        ChatMessageForm form = new ChatMessageForm();
        form.LocalId = localId;
        form.Payload = ChatPayloadType.FILE;
        form.FileId = fileId;
        return form;
    }
}
