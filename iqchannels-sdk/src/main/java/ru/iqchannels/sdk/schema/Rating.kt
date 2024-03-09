package ru.iqchannels.sdk.schema

class Rating {
	var Id: Long = 0
	var ProjectId: Long = 0
	var TicketId: Long = 0
	var ClientId: Long = 0
	var UserId: Long = 0
	var State: String? = null // RatingState
	var Value: Int? = null
	var Comment: String? = null
	var CreatedAt: Long = 0
	var UpdatedAt: Long = 0

	// Local
	var Sent = false
}
