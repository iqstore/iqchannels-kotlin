package ru.iqchannels.sdk.schema;

import android.support.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ClientIntegrationAuthRequest {
    @Nullable public String Credentials;

    public ClientIntegrationAuthRequest() {}

    public ClientIntegrationAuthRequest(@Nullable String credentials) {
        this.Credentials = credentials;
    }
}
