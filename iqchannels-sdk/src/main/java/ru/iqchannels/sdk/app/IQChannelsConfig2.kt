package ru.iqchannels.sdk.app

import ru.iqchannels.sdk.Log

class IQChannelsConfig2 @JvmOverloads constructor(
	val address: String,
	val channels: List<String>,
	logging: Boolean = true
) {

	init {
		Log.configure(logging)
	}
}
