package ru.iqchannels.sdk.schema;

import android.support.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ClientAuthRequest {
    @Nullable public String Token;

    public ClientAuthRequest() {}

    public ClientAuthRequest(@Nullable String token) {
        this.Token = token;
    }
}
