package ru.iqchannels.sdk.schema;

import android.support.annotation.NonNull;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class RateRequest {
    public long RatingId;
    @NonNull public RatingInput Rating;

    public RateRequest() {}

    public RateRequest(long ratingId, int value) {
        this.RatingId = ratingId;
        this.Rating = new RatingInput(value);
    }
}
