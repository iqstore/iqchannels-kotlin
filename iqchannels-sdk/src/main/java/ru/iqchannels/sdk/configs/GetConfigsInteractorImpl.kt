package ru.iqchannels.sdk.configs

import com.google.gson.Gson
import org.json.JSONObject
import ru.iqchannels.sdk.http.HttpException
import ru.iqchannels.sdk.schema.ChatFilesConfig
import ru.iqchannels.sdk.schema.UploadedFile
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.schema.ChatFilesConfigResponse

class GetConfigsInteractorImpl(
	private val apiServices: GetFileConfigsApi
) : GetConfigsInteractor {

	override suspend fun getFileConfigs(): ChatFilesConfig? {
		val response = apiServices.getFileConfigs()
		return if (response.isSuccessful) {
			response.body()?.result
		} else {
			throw HttpException(response.errorBody()?.string())
		}
	}

	override suspend fun getFile(fileId: String): UploadedFile? {
		val response = apiServices.getFile(fileId)

		return if (response.isSuccessful) {
			val responseBody = response.body()
			val json = responseBody?.string()?.let { JSONObject(it) }
			val result = json?.getJSONObject("Result")
			Gson().getAdapter(UploadedFile::class.java).fromJson(result.toString())
		} else {
			throw HttpException(response.errorBody()?.string())
		}
	}
}