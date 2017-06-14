/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.http;

import java.io.File;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 25/04/2017.
 */

public class HttpFile {
    public final String mimeType;
    public final File file;

    public HttpFile(String mimeType, File file) {
        this.mimeType = mimeType;
        this.file = file;
    }
}
