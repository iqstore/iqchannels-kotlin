/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.schema;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 26/01/2017.
 */

public enum FileImageSize {
    @SerializedName("")ORIGINAL(""),
    @SerializedName("avatar")AVATAR("avatar"),
    @SerializedName("thumbnail")THUMBNAIL("thumbnail"),
    @SerializedName("preview")PREVIEW("preview");

    private final String name;

    FileImageSize(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
