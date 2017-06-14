package ru.iqchannels.sdk.schema;

import android.support.annotation.Nullable;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class Response<T> {
    public boolean OK;
    @Nullable public ResponseError Error;
    @Nullable public T Result;
    @Nullable public Relations Rels;
}
