package ru.iqchannels.sdk.app

import ru.iqchannels.sdk.IQLog

class UIOptions {
	var disableIMGConfirmationModal: Boolean = false
	var showBottomTypingBar: Boolean = false

	constructor() {
		disableIMGConfirmationModal = false
		showBottomTypingBar = false
	}

	constructor(disableIMGConfirmationModal: Boolean) {
		this.disableIMGConfirmationModal = disableIMGConfirmationModal
	}

	constructor(disableIMGConfirmationModal: Boolean, showBottomTypingBar: Boolean) {
		this.disableIMGConfirmationModal = disableIMGConfirmationModal
		this.showBottomTypingBar = showBottomTypingBar
	}
}

class IQChannelsConfig2 @JvmOverloads constructor(
	val address: String,
	val channels: List<String>,
	logging: Boolean = true,
	val disableSSLValidation: Boolean = false,
	val uiOptions: UIOptions = UIOptions(),
) {

	init {
		IQLog.configure(logging)
	}
}
