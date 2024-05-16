package ru.iqchannels.sdk.app

import android.content.Context

class IQChannelsFactory {

	fun create(context: Context, config: IQChannelsConfig2, credentials: String) {
		IQChannels.configureSystem(context)

		if (config.channels.isEmpty()) {
			return
		}

		IQChannelsConfigRepository.applyConfig(config, credentials)
	}
}