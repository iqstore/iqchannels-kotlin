package ru.iqchannels.sdk.schema;


import androidx.annotation.NonNull;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ChatException extends RuntimeException {
    public static ChatException unauthorized() {
        return new ChatException(ChatExceptionCode.UNAUTHORIZED, "Unauthorized");
    }

    public static ChatException unknown() {
        return new ChatException(ChatExceptionCode.UNKNOWN, "Unknown server error");
    }

    @NonNull private final ChatExceptionCode code;

    public ChatException(ChatExceptionCode code) {
        this.code = code != null ? code : ChatExceptionCode.UNKNOWN;
    }

    public ChatException(ChatExceptionCode code, String message) {
        super(message);
        this.code = code != null ? code : ChatExceptionCode.UNKNOWN;
    }

    public ChatException(ChatExceptionCode code, String message, Throwable throwable) {
        super(message, throwable);
        this.code = code != null ? code : ChatExceptionCode.UNKNOWN;
    }

    public ChatException(ChatExceptionCode code, Throwable throwable) {
        super(throwable);
        this.code = code != null ? code : ChatExceptionCode.UNKNOWN;
    }

    @NonNull
    public ChatExceptionCode getCode() {
        return code;
    }
}
