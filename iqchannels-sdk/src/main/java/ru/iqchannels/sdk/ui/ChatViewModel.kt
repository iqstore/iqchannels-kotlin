package ru.iqchannels.sdk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.app.IQChannelsConfigRepository
import ru.iqchannels.sdk.configs.GetConfigsInteractorImpl
import ru.iqchannels.sdk.http.retrofit.NetworkModule

class ChatViewModel : ViewModel() {

	fun getConfigs() {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val interactor = GetConfigsInteractorImpl(NetworkModule.provideGetConfigApiService())
				val configs = interactor.getFileConfigs()
				Log.d("ChatViewModel", "success get chat file configs: $configs")
				IQChannelsConfigRepository.chatFilesConfig = configs
			} catch (e: Exception) {
				Log.e("ChatViewModel", e.message, e)
			}
		}
	}
}