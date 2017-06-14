package ru.iqchannels.sdk.schema;

import com.google.gson.annotations.SerializedName;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public enum ChatPayloadType {
    @SerializedName("")INVALID,
    @SerializedName("text")TEXT,
    @SerializedName("file")FILE,
    @SerializedName("notice")NOTICE,
}
