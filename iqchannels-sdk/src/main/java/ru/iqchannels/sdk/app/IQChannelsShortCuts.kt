package ru.iqchannels.sdk.app

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.iqchannels.sdk.domain.models.ChatType
import ru.iqchannels.sdk.ui.ChatFragment

object IQChannelsShortCuts {

	fun showChat(
		channel: String,
		chatType: ChatType,
		fragmentManager: FragmentManager,
		containerId: Int,
		title: String? = null
	) {
		CoroutineScope(SupervisorJob()).launch {
			IQChannelsConfigRepository.channels.collect {
				it?.let { channels ->
					channels.find { it.id == channel && it.chatType == chatType }?.let { channel1 ->
						IQChannels.configureClient(
							IQChannelsConfig(
								address = IQChannelsConfigRepository.config?.address,
								channels = IQChannelsConfigRepository.config?.channels,
								chatToOpen = channel
							)
						)
						IQChannels.chatType = chatType
						IQChannelsConfigRepository.credentials?.let { IQChannels.login(it) }

						withContext(Dispatchers.Main) {
							fragmentManager.commit {
								setReorderingAllowed(true)
								replace(containerId, ChatFragment.newInstance(title))
								addToBackStack(null)
							}
						}

						this.cancel()
					}
				}
			}
		}
	}
}