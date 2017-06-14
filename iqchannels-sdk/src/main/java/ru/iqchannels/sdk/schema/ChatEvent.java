package ru.iqchannels.sdk.schema;

import android.support.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class ChatEvent {
    public long Id;
    public ChatEventType Type;
    public long ChatId;
    public boolean Public;
    public boolean Transitive;

    @Nullable public Long SessionId;
    @Nullable public Long MessageId;
    @Nullable public Long MemberId;

    public ActorType Actor;
    @Nullable public Long ClientId;
    @Nullable public Long UserId;

    public long CreatedAt;

    // Local
    @Nullable public Client Client;
    @Nullable public User User;
    @Nullable public ChatMessage Message;
    
}
