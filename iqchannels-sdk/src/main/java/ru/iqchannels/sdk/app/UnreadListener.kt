/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.app;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 02/02/2017.
 */

public interface UnreadListener {
    void unreadChanged(int unread);
}
