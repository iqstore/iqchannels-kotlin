package ru.iqchannels.sdk.schema;


import androidx.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ClientSession {
    public long Id;
    public long ClientId;
    public String Token;
    public boolean Integration;
    @Nullable public String IntegrationHash;
    @Nullable public String IntegrationCredentials;
    public long CreatedAt;
}
