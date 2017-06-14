package ru.iqchannels.sdk.schema;

import com.google.gson.annotations.SerializedName;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public enum ChatExceptionCode {
    @SerializedName("")UNKNOWN,
    @SerializedName("internal_server_error")INTERNAL_SERVER_ERROR,
    @SerializedName("bad_request")BAD_REQUEST,
    @SerializedName("not_found")NOT_FOUND,
    @SerializedName("forbidden")FORBIDDEN,
    @SerializedName("unauthorized")UNAUTHORIZED,
    @SerializedName("invalid")INVALID
}
