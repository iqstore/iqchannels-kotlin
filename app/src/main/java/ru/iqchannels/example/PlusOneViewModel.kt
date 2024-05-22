package ru.iqchannels.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlusOneViewModel : ViewModel() {

	private val _testingType = MutableStateFlow(TestingType.MultiChat)
	val testingType = _testingType.asStateFlow()

	fun onTestingChange(testingType: TestingType) {
		_testingType.value = testingType
	}
}