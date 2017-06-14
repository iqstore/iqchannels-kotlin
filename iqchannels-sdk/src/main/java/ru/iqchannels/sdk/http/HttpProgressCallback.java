/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.http;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 26/04/2017.
 */

public interface HttpProgressCallback {
    void onProgress(int progress);
}
