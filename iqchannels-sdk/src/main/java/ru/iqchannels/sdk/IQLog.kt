package ru.iqchannels.sdk

object IQLog {

	private var LOGGING = true

	fun configure(state: Boolean) {
		synchronized(IQLog::class.java) {
			LOGGING = state
		}
	}

	fun d(tag: String, message: String) {
		if (!LOGGING) return

		android.util.Log.d(tag, message)
		IQLogFileManager.append("DEBUG", tag, message)
	}

	fun i(tag: String, message: String) {
		if (!LOGGING) return

		android.util.Log.i(tag, message)
		IQLogFileManager.append("INFO", tag, message)
	}

	@JvmStatic
	fun e(tag: String, message: String) {
		if (!LOGGING) return

		android.util.Log.e(tag, message)
		IQLogFileManager.append("ERROR", tag, message)
	}

	@JvmStatic
	fun e(
		tag: String,
		message: String?,
		throwable: Throwable?
	) {
		if (!LOGGING) return

		android.util.Log.e(tag, message, throwable)

		val text = buildString {
			append(message ?: "")
			if (throwable != null) {
				append("\n")
				append(android.util.Log.getStackTraceString(throwable))
			}
		}

		IQLogFileManager.append("ERROR", tag, text)
	}
}