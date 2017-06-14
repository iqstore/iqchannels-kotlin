package ru.iqchannels.sdk.schema;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ivan on 26/01/2017.
 */

public enum FileOwnerType {
    @SerializedName("")INVALID,
    @SerializedName("public")PUBLIC,
    @SerializedName("client")CLIENT
}
