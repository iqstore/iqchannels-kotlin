package ru.iqchannels.sdk.configs

import ru.iqchannels.sdk.http.HttpException
import ru.iqchannels.sdk.schema.ChatFilesConfig
import ru.iqchannels.sdk.schema.UploadedFile

class GetConfigsInteractorImpl(
	private val apiServices: GetFileConfigsApi
) : GetConfigsInteractor {

	override suspend fun getFileConfigs(): ChatFilesConfig? {
		val response = apiServices.getFileConfigs()

		return if (response.isSuccessful) {
			response.body()
		} else {
			throw HttpException(response.errorBody()?.string())
		}
	}

	override fun getFile(fileId: String): UploadedFile? {
		val response = apiServices.getFile(fileId)

		return if (response.isSuccessful) {
			response.body()
		} else {
			throw HttpException(response.errorBody()?.string())
		}
	}
}