package ru.iqchannels.sdk.schema;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 26/01/2017.
 */
public enum FileType {
    @SerializedName("")INVALID,
    @SerializedName("file")FILE,
    @SerializedName("image")IMAGE
}
