package ru.iqchannels.sdk

object IQLog {

	private var LOGGING = true

	fun configure(state: Boolean) {
		synchronized(IQLog::class.java) { LOGGING = state }
	}

	fun d(tag: String, message: String) {
		if (LOGGING) android.util.Log.d(tag, message)
	}

	fun i(tag: String, message: String) {
		d(tag, message)
	}

	@JvmStatic
	fun e(tag: String, message: String) {
		if (LOGGING) android.util.Log.e(tag, message)
	}

	@JvmStatic
	fun e(tag: String, message: String?, throwable: Throwable?) {
		if (LOGGING) android.util.Log.e(tag, message, throwable)
	}
}
