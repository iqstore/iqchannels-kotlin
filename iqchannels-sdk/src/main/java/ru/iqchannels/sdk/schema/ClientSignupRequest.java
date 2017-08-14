/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.schema;

import android.support.annotation.Nullable;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 14/08/2017.
 */

public class ClientSignupRequest {
    @Nullable public String Name;
    @Nullable public String Channel;

    public ClientSignupRequest() {}

    public ClientSignupRequest(@Nullable String name, @Nullable String channel) {
        this.Name = name;
        this.Channel = channel;
    }
}
