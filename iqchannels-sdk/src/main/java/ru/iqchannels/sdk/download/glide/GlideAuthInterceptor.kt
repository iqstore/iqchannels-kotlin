package ru.iqchannels.sdk.download.glide

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.OkHttpClient
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.app.IQChannels

class GlideAuthInterceptor : Interceptor {
	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val Cookie = String.format("client-session=%s", IQChannels.getCurrentToken())

		val request = chain.request()
			.newBuilder()
			.addHeader("Cookie", Cookie)
			.build()

		return chain.proceed(request)
	}
}

object GlideHttpClient {
	val instance: OkHttpClient = OkHttpClient.Builder()
		.addInterceptor(GlideAuthInterceptor())
		.build()
}