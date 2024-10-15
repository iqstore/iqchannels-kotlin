package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

class PollRequest {
	@SerializedName("RatingPollClientAnswerInput")
	var answers: List<RatingPollClientAnswerInput>? = null

	constructor(answers: List<RatingPollClientAnswerInput>) {
		this.answers = answers
	}
}