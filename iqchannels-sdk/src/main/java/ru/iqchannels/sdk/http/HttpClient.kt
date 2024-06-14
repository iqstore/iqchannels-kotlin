package ru.iqchannels.sdk.http

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InterruptedIOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile
import ru.iqchannels.sdk.Log.d
import ru.iqchannels.sdk.Log.e
import ru.iqchannels.sdk.rels.Rels
import ru.iqchannels.sdk.schema.ChatEvent
import ru.iqchannels.sdk.schema.ChatEventQuery
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ChatMessageForm
import ru.iqchannels.sdk.schema.Client
import ru.iqchannels.sdk.schema.ClientAuth
import ru.iqchannels.sdk.schema.ClientAuthRequest
import ru.iqchannels.sdk.schema.ClientIntegrationAuthRequest
import ru.iqchannels.sdk.schema.ClientSignupRequest
import ru.iqchannels.sdk.schema.ClientTypingForm
import ru.iqchannels.sdk.schema.FileToken
import ru.iqchannels.sdk.schema.MaxIdQuery
import ru.iqchannels.sdk.schema.PushTokenInput
import ru.iqchannels.sdk.schema.RateRequest
import ru.iqchannels.sdk.schema.UploadedFile

class HttpClient(
	address: String,
	rels: Rels
) {

	companion object {
		private val threadCounter = AtomicInteger()
		private const val TAG = "iqchannels.http"
	}

	private val gson: Gson
	private val rels: Rels
	private val executor: ExecutorService

	private val address: String

	@Volatile
	private var token: String? = null // Can be changed by the application main thread.

	init {
		var validAddress = address

		if (address.endsWith("/")) {
			validAddress = address.substring(0, address.length - 1)
		}

		this.address = validAddress
		this.rels = rels
		gson = Gson()
		executor = Executors.newCachedThreadPool { runnable ->
			val pkg = HttpClient::class.java.getPackage()?.name
			val thread = Thread(runnable)
			thread.name = String.format("%s-%d", pkg, threadCounter.incrementAndGet())
			thread
		}
	}

	internal fun getCurrentToken() = token


	fun setToken(token: String?) {
		this.token = token
	}

	fun clearToken() {
		token = null
	}

	@Throws(MalformedURLException::class)
	private fun requestUrl(path: String): URL {
		return URL(String.format("%s/public/api/v1%s", address, path))
	}

	// Clients
	fun clientsMe(callback: HttpCallback<Client>): HttpRequest {
		val path = "/clients/me"
		val type: TypeToken<ru.iqchannels.sdk.schema.Response<Client>> =
			object : TypeToken<ru.iqchannels.sdk.schema.Response<Client>>() {}

		return post(
			path,
			null,
			type,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<Client>> {
				override fun onResult(result: ru.iqchannels.sdk.schema.Response<Client>?) {
					val map = result?.Rels?.let { rels.map(it) }
					val client = result?.Result
					rels.client(client, map)
					callback.onResult(client)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	fun clientsSignup(
		name: String?,
		channel: String,
		callback: HttpCallback<ClientAuth>
	): HttpRequest {
		val path = "/clients/signup"
		val req = ClientSignupRequest(name, channel)
		val type: TypeToken<ru.iqchannels.sdk.schema.Response<ClientAuth>> =
			object : TypeToken<ru.iqchannels.sdk.schema.Response<ClientAuth>>() {}

		return post(
			path,
			req,
			type,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<ClientAuth>> {
				override fun onResult(response: ru.iqchannels.sdk.schema.Response<ClientAuth>?) {
					val map = response?.Rels?.let { rels.map(it) }
					val auth = response?.Result
					if (auth != null) {
						rels.clientAuth(auth, map)
					}
					callback.onResult(auth)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	fun clientsAuth(
		token: String,
		callback: HttpCallback<ClientAuth>
	): HttpRequest {
		val path = "/clients/auth"
		val req = ClientAuthRequest(token)
		val type: TypeToken<ru.iqchannels.sdk.schema.Response<ClientAuth>> =
			object : TypeToken<ru.iqchannels.sdk.schema.Response<ClientAuth>>() {}

		return post(
			path,
			req,
			type,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<ClientAuth>> {
				override fun onResult(response: ru.iqchannels.sdk.schema.Response<ClientAuth>?) {
					val map = response?.Rels?.let { rels.map(it) }
					val auth = response?.Result
					if (auth != null) {
						rels.clientAuth(auth, map)
					}
					callback.onResult(auth)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	fun clientsIntegrationAuth(
		credentials: String,
		channel: String,
		callback: HttpCallback<ClientAuth>
	): HttpRequest {
		val path = "/clients/integration_auth"
		val req = ClientIntegrationAuthRequest(credentials, channel)
		val type: TypeToken<ru.iqchannels.sdk.schema.Response<ClientAuth>> =
			object : TypeToken<ru.iqchannels.sdk.schema.Response<ClientAuth>>() {}

		return post(
			path,
			req,
			type,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<ClientAuth>> {
				override fun onResult(response: ru.iqchannels.sdk.schema.Response<ClientAuth>?) {
					val map = response?.Rels?.let { rels.map(it) }
					val auth = response?.Result
					if (auth != null) {
						rels.clientAuth(auth, map)
					}
					callback.onResult(auth)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	// Push token
	fun pushChannelFCM(
		channel: String,
		token: String,
		callback: HttpCallback<Void>
	): HttpRequest {
		val path = "/push/channel/fcm/$channel"
		val input = PushTokenInput(token)

		return post(
			path,
			input,
			null,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<Any>> {
				override fun onResult(result: ru.iqchannels.sdk.schema.Response<Any>?) {
					callback.onResult(null)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			})
	}

	fun pushChannelHMS(
		channel: String,
		token: String,
		callback: HttpCallback<Void>
	): HttpRequest {
		val path = "/push/channel/hcm/$channel"
		val input = PushTokenInput(token)

		return post(
			path,
			input,
			null,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<Any>> {
				override fun onResult(result: ru.iqchannels.sdk.schema.Response<Any>?) {
					callback.onResult(null)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			})
	}

	// Channel chat messages
	fun chatsChannelTyping(
		channel: String,
		body: ClientTypingForm,
		callback: HttpCallback<Void>
	): HttpRequest {
		val path = "/chats/channel/typing/$channel"
		return post<Any>(
			path,
			body,
			null,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<Any>> {
				override fun onResult(result: ru.iqchannels.sdk.schema.Response<Any>?) {
					callback.onResult(null)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			})
	}

	fun chatsChannelMessages(
		channel: String,
		query: MaxIdQuery,
		callback: HttpCallback<List<ChatMessage>>
	): HttpRequest {
		val path = "/chats/channel/messages/$channel"
		val type: TypeToken<ru.iqchannels.sdk.schema.Response<List<ChatMessage>>> =
			object : TypeToken<ru.iqchannels.sdk.schema.Response<List<ChatMessage>>>() {}

		return post(
			path,
			query,
			type,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<List<ChatMessage>>> {
				override fun onResult(response: ru.iqchannels.sdk.schema.Response<List<ChatMessage>>?) {
					val map = response?.Rels?.let { rels.map(it) }
					val messages = response?.Result
					map?.let {
						messages?.let {
							rels.chatMessages(messages, map)
							callback.onResult(messages)
						}
					}
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	fun chatsChannelSend(
		channel: String,
		form: ChatMessageForm,
		callback: HttpCallback<Void>
	): HttpRequest {
		val path = "/chats/channel/send/$channel"

		return post<Any>(
			path,
			form,
			null,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<Any>> {
				override fun onResult(result: ru.iqchannels.sdk.schema.Response<Any>?) {
					callback.onResult(null)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	// Channel chat events
	@SuppressLint("DefaultLocale")
	fun chatsChannelEvents(
		channel: String,
		query: ChatEventQuery,
		listener: HttpSseListener<List<ChatEvent>>
	): HttpRequest {
		var path = "/sse/chats/channel/events/$channel?ChatType=${query.ChatType}"

		if (query.LastEventId != null) {
			path = String.format("%s?LastEventId=%d", path, query.LastEventId)
		}

		if (query.Limit != null) {
			path = if (path.contains("?")) {
				"$path&"
			} else {
				"$path?"
			}
			path = String.format("%sLimit=%d", path, query.Limit)
		}

		val type: TypeToken<ru.iqchannels.sdk.schema.Response<List<ChatEvent>>> =
			object : TypeToken<ru.iqchannels.sdk.schema.Response<List<ChatEvent>>>() {}

		return sse(
			path,
			type,
			object : HttpSseListener<ru.iqchannels.sdk.schema.Response<List<ChatEvent>>> {
				override fun onConnected() {
					listener.onConnected()
				}

				override fun onEvent(event: ru.iqchannels.sdk.schema.Response<List<ChatEvent>>) {
					val map = event.Rels?.let { rels.map(it) }
					val events = event.Result
					map?.let {
						events?.let {
							rels.chatEvents(events, map)
							listener.onEvent(events)
						}
					}
				}

				override fun onException(e: Exception?) {
					listener.onException(e)
				}

				override fun onDisconnected() {
					listener.onDisconnected()
				}
			}
		)
	}

	fun chatsChannelUnread(
		channel: String,
		listener: HttpSseListener<Int>
	): HttpRequest {
		val path = String.format("/sse/chats/channel/unread/%s", channel)
		val resultType: TypeToken<ru.iqchannels.sdk.schema.Response<Int>> =
			object : TypeToken<ru.iqchannels.sdk.schema.Response<Int>>() {}

		return sse(
			path,
			resultType,
			object : HttpSseListener<ru.iqchannels.sdk.schema.Response<Int>> {
				override fun onConnected() {
					listener.onConnected()
				}

				override fun onEvent(event: ru.iqchannels.sdk.schema.Response<Int>) {
					event.Result?.let { listener.onEvent(it) }
				}

				override fun onException(e: Exception?) {
					listener.onException(e)
				}

				override fun onDisconnected() {
					listener.onDisconnected()
				}
			}
		)
	}

	// Chat messages
	fun chatsMessagesReceived(
		messageIds: List<Long>,
		callback: HttpCallback<Void>
	): HttpRequest {
		val path = "/chats/messages/received"

		return post(
			path,
			messageIds,
			null,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<Any>> {
				override fun onResult(result: ru.iqchannels.sdk.schema.Response<Any>?) {
					callback.onResult(null)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	fun chatsMessagesRead(
		messageIds: List<Long>,
		callback: HttpCallback<Void>
	): HttpRequest {
		val path = "/chats/messages/read"

		return post(
			path,
			messageIds,
			null,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<Any>> {
				override fun onResult(result: ru.iqchannels.sdk.schema.Response<Any>?) {
					callback.onResult(null)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	// Files
	fun filesUpload(
		file: File,
		mimeType: String,
		callback: HttpCallback<UploadedFile>,
		progressCallback: HttpProgressCallback?
	): HttpRequest {
		val params: MutableMap<String, String> = HashMap()
		if (mimeType != null && mimeType.startsWith("image/")) {
			params["Type"] = "image"
		} else {
			params["Type"] = "file"
		}

		val files: MutableMap<String, HttpFile> = HashMap()
		files["File"] = HttpFile(mimeType, file)
		val path = "/files/upload"
		val resultType: TypeToken<ru.iqchannels.sdk.schema.Response<UploadedFile>> =
			object : TypeToken<ru.iqchannels.sdk.schema.Response<UploadedFile>>() {}

		return multipart(
			path,
			params,
			files,
			resultType,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<UploadedFile>> {
				override fun onResult(response: ru.iqchannels.sdk.schema.Response<UploadedFile>?) {
					val map = response?.Rels?.let { rels.map(it) }
					val file = response?.Result
					if (file != null) {
						rels.file(file, map)
					}
					callback.onResult(file)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			},
			progressCallback
		)
	}

	fun filesUrl(
		fileId: String,
		callback: HttpCallback<String?>
	): HttpRequest {
		return filesToken(fileId, object : HttpCallback<FileToken?> {
			override fun onResult(result: FileToken?) {
				val token = result?.Token
				token?.let {
					val url = fileUrl(fileId, token)
					callback.onResult(url)
				}
			}

			override fun onException(exception: Exception) {
				callback.onException(exception)
			}
		})
	}

	fun filesToken(
		fileId: String,
		callback: HttpCallback<FileToken?>
	): HttpRequest {
		val path = "/files/token"
		val params: MutableMap<String, String> = HashMap()
		params["FileId"] = fileId
		val type: TypeToken<ru.iqchannels.sdk.schema.Response<FileToken>> =
			object : TypeToken<ru.iqchannels.sdk.schema.Response<FileToken>>() {}

		return post(
			path,
			params,
			type,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<FileToken>> {
				override fun onResult(result: ru.iqchannels.sdk.schema.Response<FileToken>?) {
					callback.onResult(result?.Result)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	private fun fileUrl(fileId: String, token: String): String {
		return String.format("%s/public/api/v1/files/get/%s?token=%s", address, fileId, token)
	}

	// Ratings
	fun ratingsRate(
		ratingId: Long,
		value: Int,
		callback: HttpCallback<Void>
	): HttpRequest {
		val path = "/ratings/rate"
		val req = RateRequest(ratingId, value)
		return post(
			path,
			req,
			null,
			object : HttpCallback<ru.iqchannels.sdk.schema.Response<Any>> {
				override fun onResult(result: ru.iqchannels.sdk.schema.Response<Any>?) {
					callback.onResult(null)
				}

				override fun onException(exception: Exception) {
					callback.onException(exception)
				}
			}
		)
	}

	// POST JSON
	private fun <T> post(
		path: String,
		body: Any?,
		responseType: TypeToken<ru.iqchannels.sdk.schema.Response<T>>?,
		callback: HttpCallback<ru.iqchannels.sdk.schema.Response<T>>
	): HttpRequest {
		val url: URL = try {
			requestUrl(path)
		} catch (e: MalformedURLException) {
			e(TAG, String.format("POST exception, path=%s, exc=%s", path, e))
			callback.onException(e)
			return HttpRequest()
		}
		val request = HttpRequest(url, token, gson, executor)
		executor.submit {
			try {
				request.postJSON(body, responseType, callback)
			} catch (e: InterruptedIOException) {
				d(TAG, String.format("POST cancelled, url=%s", url))
				callback.onException(e)
			} catch (e: Exception) {
				e(TAG, String.format("POST exception, url=%s, exc=%s", url, e))
				callback.onException(e)
			}
		}

		return request
	}

	private fun <T> multipart(
		path: String,
		params: Map<String, String>,
		files: Map<String, HttpFile>,
		resultType: TypeToken<ru.iqchannels.sdk.schema.Response<T>>?,
		callback: HttpCallback<ru.iqchannels.sdk.schema.Response<T>>,
		progressCallback: HttpProgressCallback?
	): HttpRequest {
		val url: URL = try {
			requestUrl(path)
		} catch (e: MalformedURLException) {
			e(TAG, String.format("Multipart exception, path=%s, exc=%s", path, e))
			callback.onException(e)
			return HttpRequest()
		}
		val request = HttpRequest(url, token, gson, executor)
		executor.submit {
			try {
				request.multipart(params, files, resultType, callback, progressCallback)
			} catch (e: InterruptedIOException) {
				d(TAG, String.format("Multipart cancelled, url=%s", url))
				callback.onException(e)
			} catch (e: Exception) {
				e(TAG, String.format("Multipart exception, url=%s, exc=%s", url, e))
				callback.onException(e)
			}
		}

		return request
	}

	private fun <T> sse(
		path: String,
		eventType: TypeToken<ru.iqchannels.sdk.schema.Response<T>>,
		listener: HttpSseListener<ru.iqchannels.sdk.schema.Response<T>>
	): HttpRequest {
		val url: URL = try {
			requestUrl(path)
		} catch (e: MalformedURLException) {
			e(TAG, String.format("SSE: exception, path=%s, exc=%s", path, e))
			listener.onException(e)
			return HttpRequest()
		}

		val request = HttpRequest(url, token, gson, executor)
		executor.submit {
			try {
				request.sse(eventType, listener)
			} catch (e: Exception) {
				e(TAG, String.format("SSE: exception, url=%s, exc=%s", url, e))
				listener.onException(e)
			}
		}

		return request
	}
}
