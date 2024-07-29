package ru.iqchannels.sdk.configs

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import ru.iqchannels.sdk.schema.ChatFilesConfig
import ru.iqchannels.sdk.schema.UploadedFile

interface GetFileConfigsApi {

	@GET("public/api/v1/files/config/")
	suspend fun getFileConfigs(): Response<ChatFilesConfig>

	@GET("public/api/v1/files/get_file/{fileId}/")
	fun getFile(
		@Path("fileId") fileId: String
	): Response<UploadedFile>
}