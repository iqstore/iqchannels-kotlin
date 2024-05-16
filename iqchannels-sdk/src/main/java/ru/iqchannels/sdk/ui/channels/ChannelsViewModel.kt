package ru.iqchannels.sdk.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.iqchannels.sdk.app.IQChannelsConfigRepository
import ru.iqchannels.sdk.domain.models.Channel

class ChannelsViewModel : ViewModel() {

	private val _channels = MutableStateFlow<List<Channel>>(emptyList())
	val channels = _channels.asStateFlow()

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
}