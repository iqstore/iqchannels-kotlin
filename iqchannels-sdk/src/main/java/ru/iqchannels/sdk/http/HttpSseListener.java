package ru.iqchannels.sdk.http;

public interface HttpSseListener<T> {
    void onConnected();

    void onEvent(T event);

    void onException(Exception e);

    void onDisconnected();
}
