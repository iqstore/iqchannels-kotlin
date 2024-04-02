package ru.iqchannels.sdk.download.glide

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response
import ru.iqchannels.sdk.app.IQChannels

class GlideAuthInterceptor : Interceptor {
	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val header = String.format("Client %s", IQChannels.getCurrentToken())
		val request = chain.request()
			.newBuilder()
			.addHeader("Authorization", header)
			.build()

		return chain.proceed(request)
	}
}