package ru.iqchannels.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlusOneViewModel : ViewModel() {

	private val _testingType = MutableStateFlow(TestingType.MultiChat)
	val testingType = _testingType.asStateFlow()

	private val _address = MutableStateFlow("https://sandbox.iqstore.ru")
	val address = _address.asStateFlow()

	private val _channels = MutableStateFlow(listOf("support", "finance"))
	val channels = _channels.asStateFlow()

	private val _chatToOpen = MutableStateFlow("")
	val chatToOpen = _chatToOpen.asStateFlow()

	fun onTestingChange(testingType: TestingType) {
		_testingType.value = testingType
	}

	fun onAddressChange(value: String) {
		_address.value = value
	}

	fun onChannelsChange(value: String) {
		value.split(',').let {
			_channels.value = it.map { it.trim() }
		}
	}

	fun onChatToOpenChange(value: String) {
		_chatToOpen.value = value
	}
}