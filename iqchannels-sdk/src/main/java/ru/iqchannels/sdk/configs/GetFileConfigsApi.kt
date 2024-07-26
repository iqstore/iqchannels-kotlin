package ru.iqchannels.sdk.configs

import retrofit2.Response
import retrofit2.http.GET
import ru.iqchannels.sdk.schema.ChatFilesConfig

interface GetFileConfigsApi {

	@GET("public/api/v1/files/config/")
	suspend fun getFileConfigs(): Response<ChatFilesConfig>
}