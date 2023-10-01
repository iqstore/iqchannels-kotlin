package ru.iqchannels.sdk.schema;

import androidx.annotation.Nullable;

public class Action {

    public Long Id;
    @Nullable public String Title;
    @Nullable public ActionType Action;
    @Nullable public String Payload;
    @Nullable public String URL;
    @Nullable public Long ChatMessageId;

}
