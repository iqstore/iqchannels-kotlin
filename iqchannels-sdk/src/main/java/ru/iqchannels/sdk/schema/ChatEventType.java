package ru.iqchannels.sdk.schema;

import com.google.gson.annotations.SerializedName;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public enum ChatEventType {
    @SerializedName("")INVALID,

    @SerializedName("chat_created")CHAT_CREATED,
    @SerializedName("chat_opened")CHAT_OPENED,
    @SerializedName("chat_closed")CHAT_CLOSED,

    @SerializedName("typing")TYPING,
    @SerializedName("message_created")MESSAGE_CREATED,
    @SerializedName("system_message_created")SYSTEM_MESSAGE_CREATED,
    @SerializedName("message_received")MESSAGE_RECEIVED,
    @SerializedName("delete-messages")MESSAGE_DELETED,
    @SerializedName("message_read")MESSAGE_READ
}
