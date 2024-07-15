package ru.iqchannels.sdk.app

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.domain.models.Channel
import ru.iqchannels.sdk.domain.models.ChatType

internal object IQChannelsConfigRepository {

	var config: IQChannelsConfig2? = null

	var credentials: String? = null

	private val _channels = MutableStateFlow<List<Channel>?>(null)
	val channels = _channels.asStateFlow()

	private var job: Job? = null

	internal fun applyConfig(config: IQChannelsConfig2, credentials: String) {
		this.config = config
		this.credentials = credentials

		if (config.channels.isNotEmpty()) {
			if (job == null || job?.isActive == false) {
				val excHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
					Log.e("applyConfig", "Apply conf error", throwable)
				}

				val coroutineContext = SupervisorJob() + Dispatchers.IO + CoroutineName("applyConfig-worker") + excHandler
				job = CoroutineScope(coroutineContext).launch {
					val channels = mutableListOf<Channel>()
					config.channels.forEach { channel ->
						IQChannels.configureClient(IQChannelsConfig(config.address, channel))
						val clientAuth = IQChannels.login2(credentials)

						clientAuth.Client?.MultiChatsInfo?.let { multiChatsInfo ->
							if (multiChatsInfo.EnableChat) {
								channels.add(
									Channel(
										id = channel,
										name = multiChatsInfo.ChannelName,
										chatType = ChatType.REGULAR,
										iconColor = multiChatsInfo.ChannelIconColor
									)
								)
							}

							if (multiChatsInfo.EnableForPersonalManagers) {
								channels.add(
									Channel(
										id = channel,
										name = multiChatsInfo.ChannelName,
										chatType = ChatType.PERSONAL_MANAGER,
										iconColor = multiChatsInfo.ChannelIconColor
									)
								)
							}
						}
					}

					channels.takeIf { it.isNotEmpty() }?.let {
						_channels.value = it
					}
				}
			}
		}
	}
}