package ru.iqchannels.sdk.ui

import android.app.Activity
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.app.IQChannelsConfigRepository
import ru.iqchannels.sdk.configs.GetConfigsInteractorImpl
import ru.iqchannels.sdk.http.retrofit.NetworkModule
import ru.iqchannels.sdk.lib.InternalIO

class ChatViewModel : ViewModel() {

	private val multipleFilesQueue: MutableList<Uri> = mutableListOf()
	private val multipleTextsQueue: MutableList<String> = mutableListOf()

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

	fun sendFile(uri: Uri, activity: Activity) {
		val file = prepareFile(uri, activity)
		IQChannels.sendFile(file, null)
	}

	fun prepareFile(uri: Uri, activity: Activity) = try {
		val resolver = activity.contentResolver
		val mimeTypeMap = MimeTypeMap.getSingleton()
		val mtype = resolver.getType(uri)
		val ext = mimeTypeMap.getExtensionFromMimeType(mtype)
		val file = FileUtils.createGalleryTempFile(activity, uri, ext)
		val `in` = resolver.openInputStream(uri)

		if (`in` == null) {
			Log.e(ChatFragment.TAG, "onGalleryResult: Failed to pick a file, no input stream")
			null
		} else {
			`in`.use { `in` ->
				val out = FileOutputStream(file)
				out.use { out ->
					InternalIO.copy(`in`, out)
				}
			}
			file
		}
	} catch (e: IOException) {
		Log.e(ChatFragment.TAG, String.format("onGalleryResult: Failed to pick a file, e=%s", e))
		null
	}

	fun sendNextFile(activity: Activity) {
		runCatching { multipleFilesQueue.removeFirst() }
			.getOrNull()
			?.let { sendFile(it, activity) }
	}

	fun addMultipleFilesQueue(files: List<Uri>) {
		multipleFilesQueue.addAll(files)
	}

	fun getNextFileFromQueue() = runCatching {
		multipleFilesQueue.removeFirst()
	}.getOrNull()

	fun clearFilesQueue() = multipleFilesQueue.clear()
}