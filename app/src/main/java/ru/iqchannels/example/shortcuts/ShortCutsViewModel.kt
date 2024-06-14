package ru.iqchannels.example.shortcuts

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.iqchannels.sdk.domain.models.ChatType

class ShortCutsViewModel : ViewModel() {

	private val _chatType = MutableStateFlow(ChatType.REGULAR)
	val chatType = _chatType.asStateFlow()

	private val _channel = MutableStateFlow("support")
	val channel = _channel.asStateFlow()

	fun onChannelChange(value: String) {
		_channel.value = value
	}

	fun onChatTypeChange(chatType: ChatType) {
		_chatType.value = chatType
	}
}