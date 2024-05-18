package ru.iqchannels.sdk.domain.models

class Channel(
	val id: String,
	val name: String?,
	val chatType: ChatType,
	val iconColor: String? = null
)