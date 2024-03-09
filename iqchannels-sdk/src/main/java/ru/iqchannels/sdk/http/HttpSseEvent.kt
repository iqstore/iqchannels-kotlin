package ru.iqchannels.sdk.http

internal class HttpSseEvent(id: String?, name: String?, data: String?) {
	val id: String
	val name: String
	val data: String

	init {
		this.id = id ?: ""
		this.name = name ?: ""
		this.data = data ?: ""
	}
}
