package ru.iqchannels.sdk.schema

class RateRequest {
	var RatingId: Long = 0
	var Rating: RatingInput? = null

	constructor()
	constructor(ratingId: Long, value: Int) {
		RatingId = ratingId
		Rating = RatingInput(value)
	}
}
