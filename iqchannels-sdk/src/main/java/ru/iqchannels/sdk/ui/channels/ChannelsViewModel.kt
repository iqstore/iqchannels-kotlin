package ru.iqchannels.sdk.ui.channels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.iqchannels.sdk.domain.models.Channel

class ChannelsViewModel : ViewModel() {

	private val _channels = MutableStateFlow<List<Channel>>(emptyList())
	val channels = _channels.asStateFlow()
}