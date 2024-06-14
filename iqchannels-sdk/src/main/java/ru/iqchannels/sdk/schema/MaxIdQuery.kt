package ru.iqchannels.sdk.schema

class MaxIdQuery {
	var MaxId: Long? = null
	var Limit: Int? = null
	var ChatType: String = ru.iqchannels.sdk.domain.models.ChatType.REGULAR.name.lowercase()
}
