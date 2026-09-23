/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.http

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.iqchannels.sdk.IQLog
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ExecutorService
import ru.iqchannels.sdk.IQLog.d
import ru.iqchannels.sdk.lib.InternalIO
import ru.iqchannels.sdk.schema.ChatException
import ru.iqchannels.sdk.schema.Relations
import ru.iqchannels.sdk.schema.Response
import java.util.concurrent.TimeUnit

class HttpRequest {

	companion object {
		private const val CONNECT_TIMEOUT_MILLIS = 15000
		private const val POST_READ_TIMEOUT_MILLIS = 15000
		private const val SSE_READ_TIMEOUT_MILLIS = 60000
		private const val TAG = "iqchannels.http"
		private val UTF8 = Charset.forName("UTF-8")
	}

	private val url: URL?
	private val token: String?
	private val gson: Gson?
	private val executor: ExecutorService?

	// Guarded by synchronized.
	private var closed = false
	private var conn: HttpURLConnection? = null
	private var call: Call? = null

	internal constructor() {
		url = null
		token = null
		gson = null
		executor = null
		conn = null
	}

	internal constructor(url: URL?, token: String?, gson: Gson?, executor: ExecutorService?) {
		this.url = url
		this.token = token
		this.gson = gson
		this.executor = executor
	}

	fun cancel() {
		executor?.execute { closeConnection() }
	}

	@Synchronized
	private fun closeConnection() {
		closed = true
		conn?.disconnect()
		call?.cancel()
	}

	@Synchronized
	@Throws(IOException::class)
	private fun openConnection(): HttpURLConnection? {
		if (closed) {
			return null
		}
		if (conn != null) {
			return conn
		}
		conn = url?.openConnection() as HttpURLConnection
		return conn
	}

	@SuppressLint("DefaultLocale")
	@Throws(IOException::class)
	fun <T> postJSON(
		body: Any?,
		resultType: TypeToken<Response<T>>?,
		callback: HttpCallback<Response<T>>
	) {
		var conn: HttpURLConnection? = null
		val gson = this.gson ?: return
		try {
			conn = openConnection()
			if (conn == null) {
				return
			}
			conn.requestMethod = "POST"
			conn.setRequestProperty("Content-Type", "application/json")
			if (token != null) {
				conn.setRequestProperty("Authorization", String.format("Client %s", token))
			}
			conn.readTimeout = POST_READ_TIMEOUT_MILLIS
			conn.connectTimeout = CONNECT_TIMEOUT_MILLIS
			conn.useCaches = false
			conn.defaultUseCaches = false
			conn.doInput = true
			d(TAG, String.format("POST: %s", url))

			// Write a body if present.
			if (body != null) {
				conn.doOutput = true
				val json = gson.toJson(body)
				val bytes = json?.toByteArray(UTF8)
				conn.setRequestProperty("Content-Length", String.format("%d", bytes?.size))
				val out = BufferedOutputStream(conn.outputStream)
				try {
					out.write(bytes)
					out.flush()
				} finally {
					out.close()
				}
			}

			// Get a status code.
			val status = conn.responseCode
			val statusText = conn.responseMessage
			if (status / 100 != 2) {

				val errorBody = try {
					conn.errorStream?.bufferedReader()?.use { it.readText() }
				} catch (e: Exception) {
					null
				}

				throw HttpException("$status $statusText\n$errorBody")
			}

			// Assert an application/json response.
			val ctype = conn.contentType
			if (ctype == null || !ctype.contains("application/json")) {
				throw HttpException(String.format("Unsupported response content type '%s'", ctype))
			}

			// Read a response when not void.
			val result: Response<T>
			val clength = conn.contentLength
			if (resultType == null) {
				result = Response()
				result.OK = true
				result.Result = null
				result.Rels = Relations()
			} else {
				if (clength == 0) {
					throw HttpException("Empty server response")
				}
				val reader = BufferedReader(InputStreamReader(conn.inputStream))
				try {
					val builder = StringBuilder()
					var line: String?
					while (reader.readLine().also { line = it } != null) {
						builder.append(line).append('\n')
					}
					var responseJson = builder.toString()

					val jsonObject = JsonParser.parseString(responseJson).asJsonObject
					if (!jsonObject.has("OK")) {
						val wrapped = JsonObject().apply {
							addProperty("OK", true)
							add("Result", jsonObject)
							add("Rels", JsonObject())
							add("Error", JsonNull.INSTANCE)
						}

						responseJson = wrapped.toString()
					}

					result = gson.fromJson(responseJson, resultType.type)
				} finally {
					reader.close()
				}
			}
			d(TAG, String.format("POST %d %s %db", status, url, clength))
			if (result.OK) {
				callback.onResult(result)
				return
			}
			val error = result.Error
			if (error == null) {
				callback.onException(ChatException.unknown())
				return
			}
			callback.onException(ChatException(error.Code, error.Text))
		} finally {
			conn?.disconnect()
		}
	}

	@SuppressLint("DefaultLocale")
	@Throws(IOException::class)
	fun <T> getJSON(
		resultType: TypeToken<Response<T>>?,
		callback: HttpCallback<Response<T>>
	) {
		var conn: HttpURLConnection? = null
		val gson = this.gson ?: return
		try {
			conn = openConnection()
			if (conn == null) {
				return
			}
			conn.requestMethod = "GET"
			conn.setRequestProperty("Accept", "application/json")
			if (token != null) {
				conn.setRequestProperty("Authorization", String.format("Client %s", token))
			}
			conn.readTimeout = POST_READ_TIMEOUT_MILLIS
			conn.connectTimeout = CONNECT_TIMEOUT_MILLIS
			conn.useCaches = false
			conn.defaultUseCaches = false
			conn.doInput = true

			d(TAG, String.format("GET: %s", url))

			// Get status code
			val status = conn.responseCode
			val statusText = conn.responseMessage
			if (status / 100 != 2) {
				throw HttpException(statusText)
			}

			// Check content type
			val ctype = conn.contentType
			if (ctype == null || !ctype.contains("application/json")) {
				throw HttpException(String.format("Unsupported response content type '%s'", ctype))
			}

			// Read response
			val result: Response<T>
			val clength = conn.contentLength
			if (resultType == null) {
				result = Response()
				result.OK = true
				result.Result = null
				result.Rels = Relations()
			} else {
				if (clength == 0) {
					throw HttpException("Empty server response")
				}
				val reader = BufferedReader(InputStreamReader(conn.inputStream))
				try {
					val builder = StringBuilder()
					var line: String?
					while (reader.readLine().also { line = it } != null) {
						builder.append(line).append('\n')
					}

					result = gson.fromJson(builder.toString(), resultType.type)
				} finally {
					reader.close()
				}
			}

			d(TAG, String.format("GET %d %s %db", status, url, clength))

			if (result.OK) {
				callback.onResult(result)
				return
			}

			val error = result.Error
			if (error == null) {
				callback.onException(ChatException.unknown())
				return
			}
			callback.onException(ChatException(error.Code, error.Text))
		} finally {
			conn?.disconnect()
		}
	}


	@SuppressLint("DefaultLocale")
	@Throws(IOException::class)
	fun <T> multipart(
		params: Map<String, String>,
		files: Map<String, HttpFile>,
		resultType: TypeToken<Response<T>>?,
		callback: HttpCallback<Response<T>>,
		progressCallback: HttpProgressCallback?
	) {
		val gson = this.gson ?: return

		val boundary = generateMultipartBoundary()
		val multipartBody = IqMultipartRequestBody(boundary, params, files, progressCallback)

		IQLog.d("!!!!!!!!!", "===== MULTIPART =====")
		IQLog.d("!!!!!!!!!", "URL: $url")
		IQLog.d("!!!!!!!!!", "Method: POST")
		IQLog.d("!!!!!!!!!", "Content-Type: ${multipartBody.contentType()}")
		IQLog.d("!!!!!!!!!", "Content-Length: ${multipartBody.contentLength()}")
		IQLog.d("!!!!!!!!!", "Boundary: $boundary")
		IQLog.d("!!!!!!!!!", "Authorization: ${if (token != null) "Client ***" else "null"}")

		IQLog.d("!!!!!!!!!", "Params:")
		params.forEach { (key, value) ->
			IQLog.d("!!!!!!!!!", "  $key=$value")
		}

		IQLog.d("!!!!!!!!!", "Files:")
		files.forEach { (key, httpFile) ->
			IQLog.d(
				"!!!!!!!!!",
				"  field=$key, " +
					"name=${httpFile.file.name}, " +
					"mime=${httpFile.mimeType}, " +
					"size=${httpFile.file.length()}"
			)
		}

		IQLog.d("!!!!!!!!!", "===== END MULTIPART =====")

		val requestBuilder = Request.Builder()
			.url(url!!)
			.post(multipartBody)

		if (token != null) {
			requestBuilder.addHeader(
				"Authorization",
				"Client $token"
			)
		}

		val client = OkHttpClient.Builder()
			.addNetworkInterceptor(LoggingInterceptor())
			.connectTimeout(CONNECT_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
			.readTimeout(POST_READ_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
			.build()

		val request = requestBuilder.build()



		IQLog.d("!!!!!!!!!", "===== OKHTTP REQUEST =====")
		IQLog.d("!!!!!!!!!", "URL: ${request.url}")
		IQLog.d("!!!!!!!!!", "Method: ${request.method}")

		for (i in 0 until request.headers.size) {
			val name = request.headers.name(i)
			val value = request.headers.value(i)

			val logValue = if (name.equals("Authorization", ignoreCase = true)) {
				"Client ***"
			} else {
				value
			}

			IQLog.d("!!!!!!!!!", "$name: $logValue")
		}

		IQLog.d("!!!!!!!!!", "Body contentType: ${request.body?.contentType()}")
		IQLog.d("!!!!!!!!!", "Body contentLength: ${request.body?.contentLength()}")
		IQLog.d("!!!!!!!!!", "===== END OKHTTP REQUEST =====")



		call = client.newCall(request)

		call?.enqueue(object : Callback {

			override fun onFailure(call: Call, e: IOException) {
				callback.onException(e)
			}

			override fun onResponse(call: Call, response: okhttp3.Response) {

				try {

					if (!response.isSuccessful) {
						val errorBody = try {
							response.body?.string()
						} catch (e: Exception) {
							null
						}
						val exception = HttpException(
							String.format("%d %s\n%s", response.code, response.message, errorBody)
						)
						exception.code = response.code
						throw exception
					}

					val responseBody = response.body?.string()
						?: throw HttpException("Empty response")

					val result: Response<T> = gson.fromJson(responseBody, resultType?.type)

					if (result.OK && !closed) {
						callback.onResult(result)
					} else {
						callback.onException(
							Exception(result.Error?.Text ?: "Unknown error")
						)
					}

				} catch (e: Exception) {
					callback.onException(e)
				} finally {
					response.close()
				}
			}
		})
	}

	private fun generateMultipartBoundary(): String {
		val uuid = UUID.randomUUID().toString()
		return String.format("-----------iqchannels-boundary-%s", uuid)
	}

	@Throws(IOException::class)
	private fun generateMultipartBody(
		boundary: String,
		params: Map<String, String>,
		files: Map<String, HttpFile>
	): ByteArrayOutputStream {
		val out = ByteArrayOutputStream()

		for (key in params.keys) {
			val value = params[key]
			out.write(String.format("--%s\r\n", boundary).toByteArray(UTF8))
			out.write(
				String.format(
					"Content-Disposition: form-data; name=\"%s\"\r\n\r\n", key
				).toByteArray(UTF8)
			)
			out.write(value?.toByteArray(UTF8))
			out.write("\r\n".toByteArray(UTF8))
		}

		for (key in files.keys) {
			val httpFile = files[key] ?: continue
			out.write(String.format("--%s\r\n", boundary).toByteArray(UTF8))
			out.write(
				String.format(
					"Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n",
					key, httpFile.file.name
				).toByteArray(UTF8)
			)
			out.write(
				String.format("Content-Type: %s\r\n\r\n", httpFile.mimeType).toByteArray(UTF8)
			)
			val `in` = FileInputStream(httpFile.file)
			try {
				InternalIO.copy(`in`, out)
			} finally {
				`in`.close()
			}
			out.write("\r\n".toByteArray(UTF8))
		}
		out.write(String.format("--%s--\r\n", boundary).toByteArray(UTF8))
		return out
	}

	@Throws(IOException::class)
	fun <T> sse(
		eventType: TypeToken<Response<T>>,
		listener: HttpSseListener<Response<T>>
	) {
		var conn: HttpURLConnection? = null
		try {
			conn = openConnection()
			if (conn == null) {
				return
			}
			conn.requestMethod = "GET"
			if (token != null) {
				conn.setRequestProperty("Authorization", String.format("Client %s", token))
			}
			conn.readTimeout = SSE_READ_TIMEOUT_MILLIS
			conn.connectTimeout = CONNECT_TIMEOUT_MILLIS
			conn.useCaches = false
			conn.defaultUseCaches = false
			d(TAG, String.format("SSE %s", url))

			// Get a status code.
			val status = conn.responseCode
			val statusText = conn.responseMessage
			if (status / 100 != 2) {
				throw HttpException(statusText)
			}

			// Assert a text/event-stream response.
			val ctype = conn.contentType
			if (ctype == null || ctype != "text/event-stream") {
				throw HttpException(String.format("Unsupported response content type '%s'", ctype))
			}

			// Read an event stream.
			d(TAG, String.format("SSE connected to %s", url))
			listener.onConnected()
			var reader: HttpSseReader? = null
			try {
				reader = HttpSseReader(BufferedReader(InputStreamReader(conn.inputStream)))
				while (true) {
					val sseEvent = reader.readEvent() ?: break
					if (gson == null) throw Exception("Gson is null")
					val event = gson.fromJson<Response<T>>(sseEvent.data, eventType.type)
					listener.onEvent(event)
				}
			} finally {
				reader?.close()
			}
		} finally {
			if (conn != null) {
				conn.disconnect()
				listener.onDisconnected()
			}
		}
	}
}

class LoggingInterceptor : Interceptor {

	override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
		val request = chain.request()

		IQLog.d("!!!!!!!!!!", "===== REAL HTTP REQUEST =====")
		IQLog.d("!!!!!!!!!!", "${request.method} ${request.url}")

		for (i in 0 until request.headers.size) {
			val name = request.headers.name(i)
			val value = request.headers.value(i)

			if (name.equals("Authorization", ignoreCase = true)) {
				IQLog.d("!!!!!!!!!!", "$name: Client ***")
			} else {
				IQLog.d("!!!!!!!!!!", "$name: $value")
			}
		}

		val contentLengthHeader = request.header("Content-Length")
		val transferEncodingHeader = request.header("Transfer-Encoding")
		IQLog.d(
			"!!!!!!!!!!",
			"Content-Length header: " +
				(contentLengthHeader?.let { "PRESENT ($it)" } ?: "ABSENT")
		)
		IQLog.d(
			"!!!!!!!!!!",
			"Transfer-Encoding header: " +
				(transferEncodingHeader?.let { "PRESENT ($it)" } ?: "ABSENT")
		)

		IQLog.d("!!!!!!!!!!", "body.contentLength = ${request.body?.contentLength()}")
		IQLog.d("!!!!!!!!!!", "body.contentType = ${request.body?.contentType()}")
		IQLog.d("!!!!!!!!!!", "===== END REAL HTTP REQUEST =====")

		return chain.proceed(request)
	}
}
