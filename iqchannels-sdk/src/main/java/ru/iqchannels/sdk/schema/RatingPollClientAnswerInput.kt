package ru.iqchannels.sdk.schema

class RatingPollClientAnswerInput {
	var ProjectId: Long = 0
	var ClientId: Long = 0
	var RatingId: Long = 0
	var RatingPollQuestionId: Long = 0
	var Type: PollOptionType = PollOptionType.FCR
	var FCR: Boolean? = null
	var RatingPollAnswerId: Long? = null
	var AnswerInput: String? = null
	var AnswerStars: Long? = null
	var AnswerScale: Int? = null

	constructor(
		projectId: Long,
		clientId: Long,
		ratingId: Long,
		questionId: Long,
		type: PollOptionType,
		answerId: Long? = null,
		input: String? = null,
		stars: Long? = null,
		scaleValue: Int? = null,
		fcr: Boolean? = null,
	) {
		ProjectId = projectId
		ClientId = clientId
		RatingId = ratingId
		RatingPollQuestionId = questionId
		Type = type
		RatingPollAnswerId = answerId
		AnswerInput = input
		AnswerStars = stars
		AnswerScale = scaleValue
		FCR = fcr
	}
}