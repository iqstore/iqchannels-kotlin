package ru.iqchannels.example

import android.content.Context
import java.io.InputStream
import java.nio.charset.Charset
import org.json.JSONObject

fun Context.getJSFromAssets(filename: String): JSONObject? {
	var stream: InputStream? = null
	return try {
		stream = assets?.open(filename)
		val size: Int = stream?.available() ?: 0
		val buffer = ByteArray(size)
		stream?.read(buffer)
		stream?.close()
		JSONObject(String(buffer, Charset.forName("UTF-8")))
	} catch (e: Exception) {
		null
	} finally {
		stream?.close()
	}
}