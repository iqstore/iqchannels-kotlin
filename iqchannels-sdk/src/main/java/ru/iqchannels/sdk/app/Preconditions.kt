/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.app;

public class Preconditions {
    private Preconditions() {}

    public static <T> T checkNotNull(T v) {
        return checkNotNull(v, "null value");
    }

    public static <T> T checkNotNull(T v, String message) {
        if (v == null) {
            throw new NullPointerException(message != null ? message : "null value");
        }
        return v;
    }
}
