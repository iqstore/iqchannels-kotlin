package ru.iqchannels.sdk.schema;

import android.support.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ClientIntegrationAuthRequest {
    @Nullable public String Credentials;
    @Nullable public String Channel;

    public ClientIntegrationAuthRequest() {}

    public ClientIntegrationAuthRequest(@Nullable String credentials, @Nullable String channel) {
        this.Credentials = credentials;
        this.Channel = channel;
    }
}
