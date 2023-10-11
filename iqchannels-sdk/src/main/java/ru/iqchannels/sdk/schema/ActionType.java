package ru.iqchannels.sdk.schema;

import com.google.gson.annotations.SerializedName;

public enum ActionType {
    @SerializedName("Postback") POSTBACK,
    @SerializedName("Open URL") OPEN_URL,
    @SerializedName("Say something") SAY_SOMETHING
}
