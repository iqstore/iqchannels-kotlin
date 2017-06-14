package ru.iqchannels.sdk.http;

/**
 * Copyright Bigdev.ru. All Rights Reserved.
 */
public class HttpException extends RuntimeException {
    public HttpException() {
        super();
    }

    public HttpException(String message) {
        super(message);
    }

    public HttpException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
