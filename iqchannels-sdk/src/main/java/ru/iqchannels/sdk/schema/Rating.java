package ru.iqchannels.sdk.schema;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class Rating {
    public long Id;
    public long ProjectId;
    public long TicketId;
    public long ClientId;
    public long UserId;

    public String State; // RatingState
    @Nullable public Integer Value;
    @Nullable public String Comment;

    public long CreatedAt;
    public long UpdatedAt;

    // Local

    public boolean Sent;
}
