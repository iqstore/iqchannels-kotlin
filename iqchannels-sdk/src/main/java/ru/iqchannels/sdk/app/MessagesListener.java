/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.app;

import java.util.List;

import ru.iqchannels.sdk.schema.ChatMessage;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 26/01/2017.
 */

public interface MessagesListener {
    void messagesLoaded(List<ChatMessage> messages);

    void messagesException(Exception e);

    void messagesCleared();

    void messageReceived(ChatMessage message);

    void messageSent(ChatMessage message);

    void messageUploaded(ChatMessage message);

    void messageUpdated(ChatMessage message);

    void messageCancelled(ChatMessage message);
}
