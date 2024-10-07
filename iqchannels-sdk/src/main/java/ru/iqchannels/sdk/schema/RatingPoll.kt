package ru.iqchannels.sdk.schema

class RatingPoll {
	val Id: Long = 0
	val FeedbackThanks: Boolean = false
	val FeedbackThanksText: String = ""
	val ShowOffer: Boolean = false
	val Questions: List<RatingPollQuestion>? = null
}