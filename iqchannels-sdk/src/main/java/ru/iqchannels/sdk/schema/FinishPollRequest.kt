package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

class FinishPollRequest {
	@SerializedName("RatingId")
	var ratingId: Long = 0

	@SerializedName("RatingPollId")
	var ratingPollId: Long = 0

	@SerializedName("Rated")
	var rated: Boolean = false

	constructor(rated: Boolean, ratingPollId: Long, ratingId: Long) {
		this.rated = rated
		this.ratingPollId = ratingPollId
		this.ratingId = ratingId
	}
}