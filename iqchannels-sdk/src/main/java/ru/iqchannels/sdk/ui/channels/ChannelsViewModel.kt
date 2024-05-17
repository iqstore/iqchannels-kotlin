package ru.iqchannels.sdk.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.app.IQChannelsConfig
import ru.iqchannels.sdk.app.IQChannelsConfigRepository
import ru.iqchannels.sdk.domain.models.Channel

class ChannelsViewModel : ViewModel() {

	private val _channels = MutableStateFlow<List<Channel>>(emptyList())
	val channels = _channels.asStateFlow()

	private val _events = MutableSharedFlow<Event>()
	val events = _events.asSharedFlow()

	init {

		viewModelScope.launch {
			IQChannelsConfigRepository.channels.collect { list ->
				when {
					list?.isNotEmpty() == true -> {
						_channels.value = list
					}

				}
			}
		}
	}

	fun onChannelClick(channel: Channel) {
		viewModelScope.launch {
			IQChannels.configureClient(
				IQChannelsConfig(
					address = IQChannelsConfigRepository.config?.address,
					channel = channel.id
				)
			)
			IQChannels.chatType = channel.chatType
			IQChannelsConfigRepository.credentials?.let { IQChannels.login(it) }
			_events.emit(Navigate2Chat(channel))
		}
	}

	sealed class Event

	data class Navigate2Chat(val channel: Channel) : Event()
}