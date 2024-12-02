/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.http

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.iqchannels.sdk.Log
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
import ru.iqchannels.sdk.Log.d
import ru.iqchannels.sdk.lib.InternalIO
import ru.iqchannels.sdk.schema.ChatException
import ru.iqchannels.sdk.schema.Relations
import ru.iqchannels.sdk.schema.Response

class HttpRequest {

	companion object {
		private const val CONNECT_TIMEOUT_MILLIS = 15000
		private const val POST_READ_TIMEOUT_MILLIS = 15000
		private const val SSE_READ_TIMEOUT_MILLIS = 120000
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
				throw HttpException(statusText)
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
					result = gson.fromJson(builder.toString(), resultType.type)
					// result = gson.fromJson(reader, resultType.getType());
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
	fun <T> multipart(
		params: Map<String, String>,
		files: Map<String, HttpFile>,
		resultType: TypeToken<Response<T>>?,
		callback: HttpCallback<Response<T>>,
		progressCallback: HttpProgressCallback?
	) {
		var conn: HttpURLConnection? = null
		val gson = this.gson ?: return

		try {
			conn = openConnection()
			if (conn == null) {
				return
			}
			val boundary = generateMultipartBoundary()
			conn.requestMethod = "POST"
			conn.setRequestProperty(
				"Content-Type",
				String.format("multipart/form-data;boundary=%s", boundary)
			)
			if (token != null) {
				conn.setRequestProperty("Authorization", String.format("Client %s", token))
			}
			conn.readTimeout = POST_READ_TIMEOUT_MILLIS
			conn.connectTimeout = CONNECT_TIMEOUT_MILLIS
			conn.useCaches = false
			conn.defaultUseCaches = false
			conn.doInput = true
			conn.doOutput = true

			// Write a multipart body.
			val body = generateMultipartBody(boundary, params, files).toByteArray()
			conn.setRequestProperty("Content-Length", String.format("%d", body.size))
			val out = conn.outputStream
			out.use { out ->
				InternalIO.copy(body, out, object : InternalIO.ProgressCallback {
					override fun onProgress(progress: Int) {
						progressCallback?.onProgress(progress)
					}
				})
			}

			// Get a status code.
			val status = conn.responseCode
			val statusText = conn.responseMessage
			if (status / 100 != 2) {
				val exception = HttpException(statusText)
				exception.code = status
				throw exception
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
				result = try {
					gson.fromJson(reader, resultType.type)
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
