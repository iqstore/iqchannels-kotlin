package ru.iqchannels.sdk.schema

import com.google.gson.Gson
import org.json.JSONObject

class Rating {
	val Id: Long = 0
	val ProjectId: Long = 0
	val TicketId: Long = 0
	val ClientId: Long = 0
	val UserId: Long = 0
	var State: String? = null // RatingState
	var Value: Int? = null
	val Comment: String? = null
	val CreatedAt: Long = 0
	val UpdatedAt: Long = 0
	val RatingPollId: Long? = null

	// Local
	var Sent = false
	var RatingPoll: RatingPoll? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}
