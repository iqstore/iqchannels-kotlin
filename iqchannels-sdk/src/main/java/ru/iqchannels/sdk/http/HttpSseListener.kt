package ru.iqchannels.sdk.http

interface HttpSseListener<T> {
	fun onConnected()
	fun onEvent(event: T)
	fun onException(e: Exception?)
	fun onDisconnected()
}
