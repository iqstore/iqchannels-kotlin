/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.app;

import ru.iqchannels.sdk.schema.ClientAuth;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 14/08/2017.
 */

public interface IQChannelsListener {
    void authenticating();
    void authComplete(ClientAuth auth);
    void authFailed(Exception e);
}
