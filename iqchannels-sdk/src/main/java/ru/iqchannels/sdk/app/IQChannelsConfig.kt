package ru.iqchannels.sdk.app

import ru.iqchannels.sdk.IQLog

class IQChannelsConfig @JvmOverloads constructor(
	val address: String?,
	val channel: String?,
	logging: Boolean = true,
	val uiOptions: UIOptions = UIOptions(),
) {

	init {
		IQLog.configure(logging)
	}
}
