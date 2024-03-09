package ru.iqchannels.sdk.schema

class ChatException : RuntimeException {
	val code: ChatExceptionCode

	constructor(code: ChatExceptionCode?) {
		this.code = code ?: ChatExceptionCode.UNKNOWN
	}

	constructor(code: ChatExceptionCode?, message: String?) : super(message) {
		this.code = code ?: ChatExceptionCode.UNKNOWN
	}

	constructor(code: ChatExceptionCode?, message: String?, throwable: Throwable?) : super(
		message,
		throwable
	) {
		this.code = code ?: ChatExceptionCode.UNKNOWN
	}

	constructor(code: ChatExceptionCode?, throwable: Throwable?) : super(throwable) {
		this.code = code ?: ChatExceptionCode.UNKNOWN
	}

	companion object {
		fun unauthorized(): ChatException {
			return ChatException(ChatExceptionCode.UNAUTHORIZED, "Unauthorized")
		}

		fun unknown(): ChatException {
			return ChatException(ChatExceptionCode.UNKNOWN, "Unknown server error")
		}
	}
}
