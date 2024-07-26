package ru.iqchannels.sdk.http.retrofit

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.configs.GetFileConfigsApi

object NetworkModule {

	fun provideOkHttpClient(
	): OkHttpClient {
		val timeout = 300L

		return OkHttpClient
			.Builder()
			.connectTimeout(timeout, TimeUnit.SECONDS)
			.readTimeout(timeout, TimeUnit.SECONDS)
			.build()
	}

	fun provideRetrofit(): Retrofit =
		Retrofit.Builder()
			.addConverterFactory(GsonConverterFactory.create())
			.baseUrl(IQChannels.getBaseUrl() ?: throw Exception("could not get address"))
			.client(provideOkHttpClient())
			.build()

	fun provideGetConfigApiService(): GetFileConfigsApi {
		val retrofit = provideRetrofit()
		return retrofit.create(GetFileConfigsApi::class.java)
	}
}