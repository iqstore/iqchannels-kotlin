package ru.iqchannels.sdk.schema

class RatingPollQuestion {
	val Id: Long = 0
	val Text: String = ""
	val Type: PollOptionType = PollOptionType.FCR
	val Scale: PollQuestionScale? = null
	val AsTicketRating: Boolean? = null
	val Answers: List<RatingPollAnswer>? = null
}

