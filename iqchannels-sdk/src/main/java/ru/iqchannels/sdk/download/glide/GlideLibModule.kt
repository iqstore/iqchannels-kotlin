package ru.iqchannels.sdk.download.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.LibraryGlideModule
import java.io.InputStream
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@GlideModule
class GlideLibModule : LibraryGlideModule() {

	override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
		val auth: Interceptor = GlideAuthInterceptor()
		val client = OkHttpClient().newBuilder().addInterceptor(auth).build()

		registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(client))
	}
}