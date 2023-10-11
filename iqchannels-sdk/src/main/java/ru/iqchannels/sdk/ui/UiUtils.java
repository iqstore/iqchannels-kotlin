package ru.iqchannels.sdk.ui;

import android.content.res.Resources;
import android.util.TypedValue;

public class UiUtils {

    public static int toPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) dp,
                Resources.getSystem().getDisplayMetrics()
        ));
    }
}
