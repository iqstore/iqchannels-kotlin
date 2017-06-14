package ru.iqchannels.sdk.schema;

import com.google.gson.annotations.SerializedName;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public enum ActorType {
    @SerializedName("")ANONYMOUS,
    @SerializedName("client")CLIENT,
    @SerializedName("user")USER,
    @SerializedName("system")SYSTEM
}
