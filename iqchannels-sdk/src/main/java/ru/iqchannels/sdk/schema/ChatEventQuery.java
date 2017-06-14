package ru.iqchannels.sdk.schema;

import android.support.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ChatEventQuery {
    @Nullable public Long LastEventId;
    @Nullable public Integer Limit;
    
    public ChatEventQuery() {}
    
    public ChatEventQuery(@Nullable Long lastEventId) {
        this.LastEventId = lastEventId;
    }
}
