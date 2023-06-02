package ru.iqchannels.sdk.schema;

import androidx.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class User {
    public long Id;
    public String Name;
    public String DisplayName;
    public String Email;
    public boolean Online;
    public boolean Deleted;
    @Nullable public String AvatarId;

    public long CreatedAt;
    @Nullable public Long LoggedInAt;
    @Nullable public Long LastSeenAt;

    // Local
    @Nullable public String AvatarUrl;
}
