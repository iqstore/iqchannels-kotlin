package ru.iqchannels.sdk.http

class HttpException : RuntimeException {
	constructor() : super()
	constructor(message: String?) : super(message)
	constructor(message: String?, throwable: Throwable?) : super(message, throwable)

	var code = -1
}
