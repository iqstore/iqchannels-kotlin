package ru.iqchannels.sdk.app

import ru.iqchannels.sdk.Log

class IQChannelsConfig @JvmOverloads constructor(
	val address: String?,
	val channel: String?,
	logging: Boolean = true,
	val uiOptions: UIOptions = UIOptions(),
) {

	init {
		Log.configure(logging)
	}
}
