package ru.iqchannels.sdk.http.retrofit

import okhttp3.Interceptor
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.configs.GetFileConfigsApi

object NetworkModule {

	fun provideOkHttpClient(): OkHttpClient {
		val timeout = 300L

		val interceptor = Interceptor { chain ->
			val request = chain.request().newBuilder()
				.addHeader("Cookie", "client-session=${IQChannels.getCurrentToken()}")
				.build()
			chain.proceed(request)
		}

		return OkHttpClient.Builder()
			.connectTimeout(timeout, TimeUnit.SECONDS)
			.readTimeout(timeout, TimeUnit.SECONDS)
			.addInterceptor(interceptor)
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