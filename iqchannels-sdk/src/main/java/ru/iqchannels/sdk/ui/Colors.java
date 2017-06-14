/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.ui;

import android.support.annotation.Nullable;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 27/01/2017.
 */
class Colors {
    private static final int BLUE_GREY_400 = 0xff78909c;
    private static final int[] COLORS = {
            0xffef5350, // red-400
            0xffec407a, // pink-400
            0xffab47bc, // purple-400
            0xff7e57c2, // deep-purple-400
            0xff5c6bc0, // indigo-400
            0xff42a5f5, // blue-400
            0xff29b6f6, // light-blue-400
            0xff26c6da, // cyan-400
            0xff26a69a, // teal-400
            0xff66bb6a, // green-400
            0xff9ccc65, // light-green-400
            0xffd4e157, // lime-400
            0xffffca28, // amber-400
            0xffffa726, // orange-400
            0xffff7043, // deep-orange-400
    };

    private Colors() {}

    static int paletteColor(@Nullable String letter) {
        if (letter == null || letter.isEmpty()) {
            return BLUE_GREY_400;
        }

        char ch = letter.charAt(0);
        return COLORS[ch % COLORS.length];
    }
}
