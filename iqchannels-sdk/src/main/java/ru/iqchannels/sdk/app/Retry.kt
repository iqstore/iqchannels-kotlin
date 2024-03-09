/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.app;

class Retry {
    private Retry() {}

    static int delaySeconds(int attempt) {
        switch (attempt) {
            case 0:
                return 1;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 5;
            case 4:
                return 10;
            case 5:
                return 15;
            case 6:
                return 20;
            default:
                return 30;
        }
    }
}
