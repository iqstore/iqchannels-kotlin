package ru.iqchannels.sdk.app

import ru.iqchannels.sdk.Log

class UIOptions {
	var disableIMGConfirmationModal: Boolean = false

	constructor() {
		disableIMGConfirmationModal = false
	}

	constructor(disableIMGConfirmationModal: Boolean) {
		this.disableIMGConfirmationModal = disableIMGConfirmationModal
	}
}

class IQChannelsConfig2 @JvmOverloads constructor(
	val address: String,
	val channels: List<String>,
	logging: Boolean = true,
	val uiOptions: UIOptions = UIOptions(),
) {

	init {
		Log.configure(logging)
	}
}
