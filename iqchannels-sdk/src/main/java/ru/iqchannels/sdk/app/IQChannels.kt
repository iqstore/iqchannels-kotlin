package ru.iqchannels.sdk.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.webkit.MimeTypeMap
import java.io.File
import java.util.*
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.configs.GetConfigsInteractorImpl
import ru.iqchannels.sdk.domain.models.ChatType
import ru.iqchannels.sdk.http.HttpCallback
import ru.iqchannels.sdk.http.HttpClient
import ru.iqchannels.sdk.http.HttpProgressCallback
import ru.iqchannels.sdk.http.HttpRequest
import ru.iqchannels.sdk.http.HttpSseListener
import ru.iqchannels.sdk.http.retrofit.NetworkModule
import ru.iqchannels.sdk.rels.Rels
import ru.iqchannels.sdk.schema.ActorType
import ru.iqchannels.sdk.schema.ChatEvent
import ru.iqchannels.sdk.schema.ChatEventQuery
import ru.iqchannels.sdk.schema.ChatEventType
import ru.iqchannels.sdk.schema.ChatException
import ru.iqchannels.sdk.schema.ChatExceptionCode
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ChatMessageForm
import ru.iqchannels.sdk.schema.ChatPayloadType
import ru.iqchannels.sdk.schema.ChatSettings
import ru.iqchannels.sdk.schema.ChatSettingsQuery
import ru.iqchannels.sdk.schema.ClientAuth
import ru.iqchannels.sdk.schema.ClientTypingForm
import ru.iqchannels.sdk.schema.FileImageSize
import ru.iqchannels.sdk.schema.FileType
import ru.iqchannels.sdk.schema.MaxIdQuery
import ru.iqchannels.sdk.schema.RatingPollClientAnswerInput
import ru.iqchannels.sdk.schema.UploadedFile
import ru.iqchannels.sdk.schema.User

object IQChannels {

	private const val TAG = "iqchannels"
	private const val ANONYMOUS_TOKEN = "anonymous_token"

	// Config and login
	internal var config: IQChannelsConfig? = null
	private var client: HttpClient? = null
	private var handler: Handler? = null // Always nonnull where used.
	private var preferences: SharedPreferences? = null

	private var token: String? = null
	private var credentials: String? = null
	private var signupName: String? = null

	private val excHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
		Log.e(TAG, "IQChannels", throwable)
	}

	private val coroutineScope = CoroutineScope(
		SupervisorJob() + Dispatchers.IO + CoroutineName("IQChannels-coroutine-worker") + excHandler
	)

	// Auth
	var auth: ClientAuth? = null
		private set
	var authRequest: HttpRequest? = null
		private set
	var fileСhooser: Boolean = false

	private var authAttempt = 0
	private val listeners: MutableSet<IQChannelsListener>

	// Push token
	private var pushToken: String? = null
	private var isHuawei: Boolean = false
	private var pushTokenSent = false
	private var pushTokenAttempt = 0
	private var pushTokenRequest: HttpRequest? = null

	// Unread
	private var unread = 0
	private var unreadAttempt = 0
	private var unreadRequest: HttpRequest? = null
	private val unreadListeners: MutableSet<UnreadListener>

	// Messages
	private var messages: MutableList<ChatMessage>? = null
	private var messageRequest: HttpRequest? = null
	private val messageListeners: MutableSet<MessagesListener>

	// More messages
	private var moreMessageRequest: HttpRequest? = null
	private val moreMessageCallbacks: MutableSet<Callback<List<ChatMessage>?>>

	// Events
	private var eventsAttempt = 0
	private var eventsRequest: HttpRequest? = null

	// Received queue
	private var receiveAttempt = 0
	private val receivedQueue: MutableSet<Long>
	private var receivedRequest: HttpRequest? = null

	// Read queue
	private var readAttempt = 0
	private val readQueue: MutableSet<Long>
	private var readRequest: HttpRequest? = null

	// Send queue
	private var localId: Long = 0
	private var sendAttempt = 0
	private val sendQueue: MutableList<ChatMessageForm>
	private var sendRequest: HttpRequest? = null
	private var sendTypingRequest: HttpRequest? = null

	// chat
	@Volatile
	internal var chatType: ChatType = ChatType.REGULAR
	internal var systemChat: Boolean = false
	private var chatSettingsRequest: HttpRequest? = null


	init {
		listeners = HashSet()
		unreadListeners = HashSet()
		messageListeners = HashSet()
		moreMessageCallbacks = HashSet()
		receivedQueue = HashSet()
		readQueue = HashSet()
		sendQueue = ArrayList()
	}

	internal fun getCurrentToken() = client?.getCurrentToken()

	fun addListener(listener: IQChannelsListener): Cancellable {
		listeners.add(listener)
		return object : Cancellable {
			override fun cancel() {
				listeners.remove(listener)
			}
		}
	}

	private fun execute(runnable: Runnable) {
		handler?.post(runnable) ?: runnable.run()
	}

	fun configure(context: Context, config: IQChannelsConfig) {
		config.address?.let {
			if (this.config != null) {
				clear()
			}

			handler = Handler(context.applicationContext.mainLooper)
			this.config = config
			client = HttpClient(config.address, Rels(config.address))
			preferences = context.applicationContext.getSharedPreferences(
				"IQChannels", Context.MODE_PRIVATE
			)
		}
	}

	fun configureSystem(context: Context) {
		handler = Handler(context.applicationContext.mainLooper)
		preferences = context.applicationContext.getSharedPreferences(
			"IQChannels", Context.MODE_PRIVATE
		)
	}

	fun configureClient(config: IQChannelsConfig) {
		config.address?.let {
			if (this.config != null) {
				clear()
			}

			this.config = config
			client = HttpClient(config.address, Rels(config.address))
		}
	}

	fun signup(name: String?) {
		logout()
		signupName = name
		signupAnonymous()
	}

	fun login(credentials: String) {
		logout()
		this.credentials = credentials
		auth()
	}

	internal suspend fun login2(credentials: String): ClientAuth {
		logout()
		this.credentials = credentials
		return auth2()
	}

	fun loginAnonymous() {
		logout()
		authAnonymous()
		token = preferences?.getString(ANONYMOUS_TOKEN, null)
		if (token == null || token?.isEmpty() == true) {
			signup("")
		} else {
			auth()
		}
	}

	fun logout() {
		clear()
		credentials = null
		token = null
		Log.i(TAG, "Logout")
	}

	fun logoutAnonymous() {
		logout()
		preferences?.let { preferences ->
			val editor = preferences.edit()
			editor.remove(ANONYMOUS_TOKEN)
			editor.apply()
			Log.i(TAG, "Logout anonymous")
		}
	}

	internal fun getBaseUrl() = config?.address

	private fun clear() {
		clearAuth()
		clearPushTokenState()
		clearUnread()
		clearMessages()
		clearMoreMessages()
		clearEvents()
		clearReceived()
		clearRead()
		clearSend()
		Log.d(TAG, "Cleared")
	}

	// Auth
	private fun clearAuth() {
		authRequest?.cancel()
		auth = null
		authRequest = null
		client?.clearToken()
		signupName = null
		Log.d(TAG, "Cleared auth")
	}

	private fun auth() {
		if (auth != null) {
			return
		}
		if (authRequest != null) {
			return
		}
		if (client == null) {
			return
		}
		if (credentials == null && token == null) {
			return
		}

		val callback: HttpCallback<ClientAuth> = object : HttpCallback<ClientAuth> {
			override fun onResult(result: ClientAuth?) {
				result?.let {
					execute { authComplete(result) }
				}
			}

			override fun onException(exception: Exception) {
				execute { authException(exception) }
			}
		}

		client?.let { client ->
			authAttempt++
			authRequest = if (credentials != null) {
				config?.channel?.let { channel ->
					credentials?.let { credentials ->
						client.clientsIntegrationAuth(credentials, channel, callback)
					}
				}
			} else {
				token?.let {
					client.clientsAuth(it, callback)
				}
			}

			Log.i(TAG, String.format("Authenticating, attempt=%d", authAttempt))
			for (listener in listeners) {
				execute { listener.authenticating() }
			}
		}
	}

	private suspend fun auth2(): ClientAuth = suspendCoroutine { continuation ->

		val callback: HttpCallback<ClientAuth> = object : HttpCallback<ClientAuth> {
			override fun onResult(result: ClientAuth?) {
				result?.let {
					continuation.resume(it)
				}
			}

			override fun onException(exception: Exception) {
				continuation.resumeWithException(exception)
			}
		}

		client?.let { client ->
			authAttempt++
			authRequest = if (credentials != null) {
				config?.channel?.let { channel ->
					credentials?.let { credentials ->
						client.clientsIntegrationAuth(credentials, channel, callback)
					}
				}
			} else {
				token?.let {
					client.clientsAuth(it, callback)
				}
			}

			Log.i(TAG, String.format("Authenticating, attempt=%d", authAttempt))
		}
	}

	private fun authException(exception: Exception) {
		if (authRequest == null) {
			return
		}

		authRequest = null

		if (credentials == null) {
			Log.e(TAG, String.format("Failed to auth, exc=%s", exception))
			return
		}

		for (listener in listeners) {
			execute { listener.authFailed(exception, authAttempt) }
		}

		handler?.let { handler ->
			val delaySec = Retry.delaySeconds(authAttempt)
			handler.postDelayed({ auth() }, (delaySec * 1000).toLong())
			Log.e(
				TAG, String.format(
					"Failed to auth, will retry in %d seconds, exc=%s",
					delaySec, exception
				)
			)
		}
	}

	private fun authComplete(auth: ClientAuth) {
		if (auth.Client == null || auth.Session == null || auth.Session?.Token == null) {
			authException(ChatException(ChatExceptionCode.INVALID, "Invalid client auth"))
			return
		}

		if (authRequest == null) {
			return
		}

		authRequest = null

		client?.let { client ->
			this.auth = auth
			authAttempt = 0
			client.setToken(auth.Session?.Token)
			Log.i(
				TAG, String.format(
					"Authenticated, clientId=%d, sessionId=%d",
					auth.Client?.Id, auth.Session?.Id
				)
			)
			for (listener in listeners) {
				execute { listener.authComplete(auth) }
			}
			sendPushToken()
			getChatSettings()
			listenToUnread()
		}
	}

	// Anonymous auth
	private fun authAnonymous() {
		if (auth != null) {
			return
		}
		if (authRequest != null) {
			return
		}
		if (client == null) {
			return
		}
		token = preferences?.getString(ANONYMOUS_TOKEN, null)

		if (token == null || token?.isEmpty() == true) {
			logout()
			signupAnonymous()
			return
		}

		val callback: HttpCallback<ClientAuth> = object : HttpCallback<ClientAuth> {
			override fun onResult(result: ClientAuth?) {
				result?.let {
					execute { authComplete(result) }
				}
			}

			override fun onException(exception: Exception) {
				execute { authAnonymousException(exception) }
			}
		}

		authAttempt++
		authRequest = token?.let { client?.clientsAuth(it, callback) }
		Log.i(TAG, String.format("Authenticating anonymous, attempt=%d", authAttempt))
		for (listener in listeners) {
			execute { listener.authenticating() }
		}
	}

	private fun authAnonymousException(exception: Exception) {
		if (authRequest == null) {
			return
		}

		authRequest = null
		if (credentials == null) {
			Log.e(TAG, String.format("Failed to auth, exc=%s", exception))
			return
		}

		for (listener in listeners) {
			execute { listener.authFailed(exception, authAttempt) }
		}

		if (exception is ChatException) {
			val code = exception.code
			if (code == ChatExceptionCode.UNAUTHORIZED) {
				Log.e(TAG, "Failed to auth, invalid anonymous token")
				assert(preferences != null)
				val editor = preferences?.edit()
				editor?.remove(ANONYMOUS_TOKEN)
				editor?.apply()
				token = null
				logout()
				signupAnonymous()
				return
			}
		}

		handler?.let { handler ->
			val delaySec = Retry.delaySeconds(authAttempt)
			handler.postDelayed({ auth() }, (delaySec * 1000).toLong())
			Log.e(
				TAG, String.format(
					"Failed to auth, will retry in %d seconds, exc=%s",
					delaySec, exception
				)
			)
		}
	}

	// Signup anonymous
	private fun signupAnonymous() {
		if (client == null) {
			return
		}

		config?.let { config ->
			val name = signupName
			config.channel?.let { channel ->
				authRequest =
					client?.clientsSignup(name, channel, object : HttpCallback<ClientAuth> {
						override fun onResult(result: ClientAuth?) {
							result?.let {
								execute { signupComplete(result) }
							}
						}

						override fun onException(exception: Exception) {
							execute { signupException(exception) }
						}
					})
			}


			Log.i(TAG, "Signing up anonymous client")
		}

	}

	private fun signupComplete(auth: ClientAuth) {
		if (auth.Client == null || auth.Session == null || auth.Session?.Token == null) {
			signupException(ChatException(ChatExceptionCode.INVALID, "Invalid client auth"))
			return
		}

		if (authRequest == null) {
			return
		}

		preferences?.let { preferences ->
			val editor = preferences.edit()
			editor.putString(ANONYMOUS_TOKEN, auth.Session?.Token)
			editor.apply()
			Log.i(TAG, String.format("Signed up anonymous client, clientId=%d", auth.Client?.Id))
			authComplete(auth)
		}

	}

	private fun signupException(exception: Exception) {
		if (authRequest == null) {
			return
		}
		authRequest = null
		Log.e(TAG, String.format("Failed to sign up anonymous client, exc=%s", exception))
		for (listener in listeners) {
			execute { listener.authFailed(exception, authAttempt) }
		}

		handler?.let { handler ->
			val delaySec = Retry.delaySeconds(authAttempt)
			handler.postDelayed({
				logout()
				signupAnonymous()}, (delaySec * 1000).toLong())
			Log.e(
				TAG, String.format(
					"Failed to signup, will retry in %d seconds, exc=%s",
					delaySec, exception
				)
			)
		}
	}

	// Push token
	fun setPushToken(token: String?, isHuawei: Boolean = false) {
		if (token != null && pushToken != null && token == pushToken) {
			return
		}

		this.isHuawei = isHuawei
		pushToken = token
		pushTokenSent = false
		if (pushTokenRequest != null) {
			pushTokenRequest?.cancel()
			pushTokenRequest = null
		}
		sendPushToken()
	}

	private fun clearPushTokenState() {
		pushTokenRequest?.cancel()
		pushTokenSent = false
		pushTokenAttempt = 0
		pushTokenRequest = null
		Log.d(TAG, "Cleared push token state")
	}

	private fun sendPushToken() {
		if (auth == null) {
			return
		}
		if (pushToken == null) {
			return
		}
		if (pushTokenSent) {
			return
		}
		if (pushTokenRequest != null) {
			return
		}

		client?.let { client ->
			config?.channel?.let { channel ->
				pushToken?.let { pushToken ->
					pushTokenAttempt++
					pushTokenRequest = if (isHuawei) {
						client.pushChannelHMS(channel, pushToken, object : HttpCallback<Void> {
							override fun onResult(result: Void?) {
								execute { onSentPushToken() }
							}

							override fun onException(e: Exception) {
								execute { onFailedToSendPushToken(e) }
							}
						})
					} else {
						client.pushChannelFCM(channel, pushToken, object : HttpCallback<Void> {
							override fun onResult(result: Void?) {
								execute { onSentPushToken() }
							}

							override fun onException(e: Exception) {
								execute { onFailedToSendPushToken(e) }
							}
						})
					}
					Log.i(TAG, String.format("Sending a push token, attempt=%d", pushTokenAttempt))
				}
			}
		}
	}

	private fun onSentPushToken() {
		if (pushTokenRequest == null) {
			return
		}
		pushTokenRequest = null
		pushTokenSent = true
		Log.i(TAG, "Sent a push token")
	}

	private fun onFailedToSendPushToken(e: Exception) {
		if (pushTokenRequest == null) {
			return
		}
		pushTokenRequest = null
		val delaySec = Retry.delaySeconds(pushTokenAttempt)
		handler?.postDelayed({ sendPushToken() }, (delaySec * 1000).toLong())
		Log.e(
			TAG, String.format(
				"Failed to send a push token, will retyr in %ds, exc=%s",
				delaySec, e
			)
		)
	}

	// Unread
	fun addUnreadListener(listener: UnreadListener): Cancellable {
		Preconditions.checkNotNull(listener, "null listener")
		unreadListeners.add(listener)
		listenToUnread()
		Log.d(TAG, String.format("Added an unread listener %s", listener))
		val copy = unread
		execute { listener.unreadChanged(copy) }
		return object : Cancellable {
			override fun cancel() {
				unreadListeners.remove(listener)
				Log.d(TAG, String.format("Removed an unread listener %s", listener))
				clearUnreadWhenNoListeners()
			}
		}
	}

	private fun clearUnread() {
		unreadRequest?.cancel()
		unread = 0
		unreadAttempt = 0
		unreadRequest = null
		for (listener in unreadListeners) {
			execute { listener.unreadChanged(0) }
		}
		Log.d(TAG, "Cleared unread")
	}

	private fun clearUnreadWhenNoListeners() {
		if (unreadListeners.isEmpty()) {
			clearUnread()
		}
	}

	private fun listenToUnread() {
		if (auth == null) {
			return
		}
		if (unreadRequest != null) {
			return
		}
		if (unreadListeners.isEmpty()) {
			return
		}

		client?.let { client ->
			unreadAttempt++

			config?.channel?.let { channel ->
				unreadRequest =
					client.chatsChannelUnread(channel, object : HttpSseListener<Int> {
						override fun onConnected() {}

						override fun onException(e: Exception?) {
							e?.let { execute { unreadException(e) } }
						}

						override fun onEvent(event: Int) {
							execute { unreadReceived(event) }
						}

						override fun onDisconnected() {
							execute { unreadDisconnected(Exception("disconnected")) }
						}
					})
				Log.i(
					TAG,
					String.format("Listening to unread notifications, attempt=%d", unreadAttempt)
				)
			}
		}
	}

	private fun unreadException(e: Exception) {
		if (unreadRequest == null) {
			return
		}
		unreadRequest = null
		if (auth == null) {
			Log.i(TAG, String.format("Failed to listen to unread notifications, exc=%s", e))
			return
		}
		val delaySec = Retry.delaySeconds(unreadAttempt)
		handler?.postDelayed({ listenToUnread() }, (delaySec * 1000).toLong())
		Log.e(
			TAG, String.format(
				"Failed to listen to unread notifications, will retry in %ds, exc=%s",
				delaySec, e
			)
		)
	}

	private fun unreadDisconnected(e: Exception) {
		if (unreadRequest == null) {
			return
		}
		unreadRequest = null
		if (auth == null) {
			Log.i(TAG, String.format("Failed to listen to unread notifications, exc=%s", e))
			return
		}

		handler?.postDelayed({ listenToUnread() }, 1000)
		Log.e(
			TAG,
			String.format("Failed to listen to unread notifications, will retry in 1s, exc=%s", e)
		)
	}

	private fun unreadReceived(unread: Int?) {
		if (unreadRequest == null) {
			return
		}
		this.unread = unread ?: 0
		unreadAttempt = 0
		Log.i(TAG, String.format("Received an unread notification, unread=%d", this.unread))
		val copy = this.unread
		for (listener in unreadListeners) {
			execute { listener.unreadChanged(copy) }
		}
	}

	// Messages
	fun loadMessages(listener: MessagesListener): Cancellable {
		Preconditions.checkNotNull(listener, "null listener")
		messageListeners.add(listener)
		Log.d(TAG, String.format("Added a messages listener %s", listener))

		if(!fileСhooser) {
			getChatSettings()
		}
		fileСhooser = false

		if (messages != null) {
			listenToEvents()
			val copy: List<ChatMessage> = ArrayList(messages)
			execute { listener.messagesLoaded(copy) }
		}

		return object : Cancellable {
			override fun cancel() {
				messageListeners.remove(listener)
				Log.d(TAG, String.format("Removed a messages listener %s", listener))
				cancelLoadMessagesWhenNoListeners()
			}
		}
	}

	private fun cancelLoadMessagesWhenNoListeners() {
		if (messageListeners.isNotEmpty()) {
			return
		}
		messageRequest?.cancel()
		clearEvents()
	}

	private fun clearMessages() {
		messageRequest?.cancel()
		for (listener in messageListeners) {
			listener.messagesCleared()
		}
		messages = null
		messageRequest = null
		Log.d(TAG, "Cleared messages")
	}

	private fun loadMessages(autoGreeting: ChatMessage?) {
		if (messageRequest != null) {
			return
		}
		if (auth == null) {
			return
		}
		if (messageListeners.isEmpty()) {
			return
		}
		val query = MaxIdQuery().apply {
			ChatType = chatType.name.lowercase()
		}

		client?.let { client ->
			config?.channel?.let { channel ->
				messageRequest = client.chatsChannelMessages(
					channel,
					query,
					object : HttpCallback<List<ChatMessage>> {
						override fun onResult(messages: List<ChatMessage>?) {
							val copy: MutableList<ChatMessage> = ArrayList(messages)
							autoGreeting?.let {
								copy.add(autoGreeting)
							}
							messages?.let {
								execute { messagesLoaded(copy) }
							}
						}

						override fun onException(exception: Exception) {
							execute { messagesException(exception) }
						}
					}
				)
				Log.i(TAG, "Loading messages")
			}
		}
	}

	private fun showAutoGreeting(settings: ChatSettings?) {
		Log.d("chatSettings", settings.toString())
		systemChat = settings?.Enabled == true

		if(systemChat){
			openSystemChat()
		}
		val now = Date()
		val message = when {
			settings == null -> null
			systemChat && settings.TotalOpenedTickets == 0 -> ChatMessage().apply {
				Id = now.time
				Author = ActorType.USER
				CreatedAt = now.time
				Date = now
				Text = settings.Message
				Payload = ChatPayloadType.TEXT
				Read = true
				Received = true
				UserId = now.time
				User = User().apply { DisplayName = settings.OperatorName }
			}
			else -> null
		}
		loadMessages(message)
	}

	private fun getChatSettings() {
		val clientId = auth?.Client?.Id ?: return

		val query = ChatSettingsQuery().apply {
			ClientId = clientId
		}

		client?.let { client ->
			config?.channel?.let { channel ->
				chatSettingsRequest = client.getChatSettings(
					channel,
					query,
					object : HttpCallback<ChatSettings> {
						override fun onResult(result: ChatSettings?) {
							showAutoGreeting(result)
						}

						override fun onException(exception: Exception) {
							Log.e(TAG, "Failed to get chat settings, exc=${exception.message}")
						}
					}
				)
				Log.i(TAG, "get chat settings")
			}
		}
	}

	private fun openSystemChat() {
		client?.let { client ->
			config?.channel?.let { channel ->
				 client.openSystemChat(channel)
			}
		}
	}

	private fun messagesException(exception: Exception) {
		if (messageRequest == null) {
			return
		}
		messageRequest = null
		Log.e(TAG, String.format("Failed to load messages, exc=%s", exception))
		for (listener in messageListeners) {
			execute { listener.messagesException(exception) }
		}
		messageListeners.clear()
	}

	private fun messagesLoaded(messages: List<ChatMessage>) {
		if (messageRequest == null) {
			return
		}
		messageRequest = null
		setMessages(messages)
		Log.i(TAG, String.format("Loaded messages, size=%d", messages.size))
		this.messages?.let {
			val copy: List<ChatMessage> = ArrayList(it)
			for (listener in messageListeners) {
				execute { listener.messagesLoaded(copy) }
			}
			listenToEvents()
		}
	}

	// More messages
	private fun clearMoreMessages() {
		moreMessageRequest?.cancel()
		val exc = CancellationException()
		for (callback in moreMessageCallbacks) {
			execute { callback.onException(exc) }
		}
		moreMessageCallbacks.clear()
	}

	fun loadMoreMessages(callback: Callback<List<ChatMessage>?>): Cancellable {
		moreMessageCallbacks.add(callback)
		loadMoreMessages()
		return object : Cancellable {
			override fun cancel() {
				moreMessageCallbacks.remove(callback)
				cancelLoadMoreMessagesWhenNoCallbacks()
			}
		}
	}

	private fun cancelLoadMoreMessagesWhenNoCallbacks() {
		if (moreMessageCallbacks.isEmpty()) {
			clearMoreMessages()
		}
	}

	private fun loadMoreMessages() {
		if (auth == null || messages == null) {
			for (callback in moreMessageCallbacks) {
				execute { callback.onResult(null) }
			}
			moreMessageCallbacks.clear()
			return
		}
		if (moreMessageRequest != null) {
			return
		}
		val query = MaxIdQuery()

		messages?.let { messages ->
			for (message in messages) {
				if (message.Id > 0) {
					query.MaxId = message.Id
					break
				}
			}

			client?.let { client ->
				config?.channel?.let { channel ->
					moreMessageRequest = client.chatsChannelMessages(
						channel,
						query,
						object : HttpCallback<List<ChatMessage>> {
							override fun onResult(messages: List<ChatMessage>?) {
								messages?.let {
									execute { moreMessagesLoaded(messages) }
								}
							}

							override fun onException(exception: Exception) {
								execute { moreMessagesException(exception) }
							}
						})
					Log.i(TAG, String.format("Loading more messages, maxId=%s", query.MaxId))
				}
			}
		}
	}

	private fun moreMessagesException(exception: Exception) {
		if (moreMessageRequest == null) {
			return
		}
		moreMessageRequest = null
		Log.e(TAG, String.format("Failed to load more messages, exc=%s", exception))
		for (callback in moreMessageCallbacks) {
			execute { callback.onException(exception) }
		}
		moreMessageCallbacks.clear()
	}

	private fun moreMessagesLoaded(moreMessages: List<ChatMessage>) {
		if (moreMessageRequest == null) {
			return
		}
		moreMessageRequest = null
		val newMessages = prependMessages(moreMessages)

		Log.i(
			TAG, String.format(
				"Loaded more messages, count=%d, total=%d",
				moreMessages.size, messages?.size
			)
		)

		// Notify the callbacks.
		for (callback in moreMessageCallbacks) {
			execute { callback.onResult(newMessages) }
		}
		moreMessageCallbacks.clear()
	}

	// Events
	private fun clearEvents() {
		eventsRequest?.cancel()
		eventsAttempt = 0
		eventsRequest = null
		Log.d(TAG, "Cleared events")
	}

	private fun listenToEvents() {
		if (eventsRequest != null) {
			return
		}
		if (auth == null) {
			return
		}
		if (messages == null) {
			return
		}
		val query = ChatEventQuery().apply {
			ChatType = chatType.name.lowercase()
		}

		messages?.let { messages ->
			for (message in messages) {
				val messageEventId = message.EventId ?: continue

				val lastEventId = query.LastEventId
				if (lastEventId == null) {
					query.LastEventId = message.EventId
					continue
				}

				if (lastEventId < messageEventId) {
					query.LastEventId = message.EventId
				}
			}
		}

		eventsAttempt++

		client?.let { client ->
			config?.channel?.let { channel ->
				eventsRequest = client.chatsChannelEvents(
					channel,
					query,
					object : HttpSseListener<List<ChatEvent>> {
						override fun onConnected() {}
						override fun onEvent(events: List<ChatEvent>) {
							execute { eventsReceived(events) }
						}

						override fun onException(e: Exception?) {
							e?.let { execute { eventsException(e) } }
						}

						override fun onDisconnected() {
							execute { eventsDisconnected(Exception("disconnected")) }
						}
					}
				)
			}
		}
	}

	private fun eventsException(e: Exception) {
		if (eventsRequest == null) {
			return
		}
		eventsRequest = null
		if (auth == null) {
			Log.i(TAG, String.format("Failed to listen to events, exc=%s", e))
			return
		}

		val delaySec = Retry.delaySeconds(eventsAttempt)
		handler?.postDelayed({ listenToEvents() }, (delaySec * 1000).toLong())
		Log.e(
			TAG, String.format(
				"Failed to listen to events, will retry in %d seconds, exc=%s",
				delaySec, e
			)
		)
	}

	private fun eventsDisconnected(e: Exception) {
		if (eventsRequest == null) {
			return
		}
		eventsRequest = null
		if (auth == null) {
			Log.i(TAG, String.format("Failed to listen to events, exc=%s", e))
			return
		}
		handler?.postDelayed({ listenToEvents() }, 1000)
		Log.e(TAG, String.format("Failed to listen to events, will retry in 1 seconds, exc=%s", e))
	}

	private fun eventsReceived(events: List<ChatEvent>) {
		if (eventsRequest == null) {
			return
		}

		eventsAttempt = 0
		Log.i(TAG, String.format("Received chat events, count=%d", events.size))
		applyEvents(events)
	}

	// Received message ids
	private fun clearReceived() {
		receivedRequest?.cancel()
		receiveAttempt = 0
		receivedQueue.clear()
		receivedRequest = null
		Log.d(TAG, "Cleared received queue")
	}

	private fun enqueueReceived(message: ChatMessage) {
		if (message.My) {
			return
		}
		if (message.Received) {
			return
		}
		receivedQueue.add(message.Id)
		sendReceived()
	}

	private fun sendReceived() {
		if (auth == null) {
			return
		}
		if (receivedQueue.isEmpty()) {
			return
		}
		if (receivedRequest != null) {
			return
		}
		val messageIds: List<Long> = ArrayList(receivedQueue)
		receivedQueue.clear()
		receiveAttempt++
		receivedRequest = client?.chatsMessagesReceived(messageIds, object : HttpCallback<Void> {
			override fun onResult(result: Void?) {
				execute { sentReceivedMessageIds(messageIds) }
			}

			override fun onException(exception: Exception) {
				execute { sendReceivedMessageIdsException(exception, messageIds) }
			}
		})

		Log.i(TAG, String.format("Sending received message ids, count=%d", messageIds.size))
	}

	private fun sendReceivedMessageIdsException(exception: Exception, messageIds: List<Long>) {
		if (receivedRequest == null) {
			return
		}

		receivedRequest = null
		if (auth == null) {
			Log.i(TAG, "Failed to send received message ids")
			return
		}
		receivedQueue.addAll(messageIds)

		val delaySec = Retry.delaySeconds(receiveAttempt)
		handler?.postDelayed({ sendReceived() }, (delaySec * 1000).toLong())
		Log.e(
			TAG, String.format(
				"Failed to send received message ids, will retry in %d seconds, exc=%s",
				delaySec, exception
			)
		)
	}

	private fun sentReceivedMessageIds(messageIds: List<Long>) {
		if (receivedRequest == null) {
			return
		}

		receivedRequest = null
		receiveAttempt = 0
		Log.i(TAG, String.format("Sent received message ids, count=%d", messageIds.size))
		sendReceived()
	}

	// Send read message ids
	fun markAsRead(message: ChatMessage) {
		if (message.My) {
			return
		}
		if (message.Read) {
			return
		}
		readQueue.add(message.Id)
		sendRead()
	}

	private fun clearRead() {
		readRequest?.cancel()
		readAttempt = 0
		readRequest = null
		readQueue.clear()
		Log.d(TAG, "Cleared read queue")
	}

	private fun sendRead() {
		if (auth == null) {
			return
		}
		if (readQueue.isEmpty()) {
			return
		}
		if (readRequest != null) {
			return
		}
		val messageIds: List<Long> = ArrayList(readQueue)
		readQueue.clear()
		readAttempt++

		readRequest = client?.chatsMessagesRead(messageIds, object : HttpCallback<Void> {
			override fun onResult(result: Void?) {
				execute { sentReadMessageIds(messageIds) }
			}

			override fun onException(exception: Exception) {
				execute { sendReadMessageIdsException(exception, messageIds) }
			}
		})

		Log.i(TAG, String.format("Sending read message ids, count=%d", messageIds.size))
	}

	private fun sendReadMessageIdsException(exception: Exception, messageIds: List<Long>) {
		if (readRequest == null) {
			return
		}
		readRequest = null

		if (auth == null) {
			Log.i(TAG, "Failed to send read message ids")
			return
		}
		readQueue.addAll(messageIds)
		val delaySec = Retry.delaySeconds(readAttempt)

		handler?.postDelayed({ sendRead() }, (delaySec * 1000).toLong())
		Log.e(
			TAG, String.format(
				"Failed to send read message ids, will retry in %d seconds, exc=%s",
				delaySec, exception
			)
		)
	}

	private fun sentReadMessageIds(messageIds: List<Long>) {
		if (readRequest == null) {
			return
		}
		readRequest = null
		readAttempt = 0
		Log.i(TAG, String.format("Sent read message ids, count=%d", messageIds.size))
		sendRead()
	}

	// Send
	internal fun send(text: String?, replyToMessageId: Long?): ChatMessage? {
		if (text == null) {
			return null
		}
		if (text.isEmpty()) {
			return null
		}
		if (auth == null) {
			return null
		}

		auth?.Client?.let { client ->
			val localId = nextLocalId()
			val message = ChatMessage(client, localId, text)
			message.Sending = true
			messages?.add(message)

			for (listener in messageListeners) {
				execute { listener.messageSent(message) }
			}

			val form = ChatMessageForm.text(localId, text, replyToMessageId)
			sendQueue.add(form)
			form.ChatType = chatType.name.lowercase()
			Log.i(TAG, String.format("Enqueued an outgoing message, localId=%d", localId))
			send()

			return message
		}

		return null
	}

	fun handleVersion() {
		if (auth == null) {
			return
		}

		val localId = nextLocalId()
		val user = User()
		user.DisplayName = "Система"
		user.Online = true
		user.Id = 1
		val message = ChatMessage(user, localId)
		message.Text = "2.1.0"
		messages?.add(message)
		for (listener in messageListeners) {
			execute {
				val l: MutableList<ChatMessage> = ArrayList()
				l.add(message)
				listener.messagesLoaded(l)
			}
		}
	}

	internal fun sendPostbackReply(title: String?, botpressPayload: String?) {
		if (botpressPayload == null || title == null) {
			return
		}
		if (auth == null) {
			return
		}

		auth?.Client?.let { client ->
			val localId = nextLocalId()
			val message = ChatMessage(client, localId)
			message.Sending = true
			messages?.add(message)

			for (listener in messageListeners) {
				execute { listener.messageSent(message) }
			}

			val form = ChatMessageForm.payloadReply(localId, title, botpressPayload)
			sendQueue.add(form)
			form.ChatType = chatType.name.lowercase()
			Log.i(TAG, String.format("Enqueued an outgoing message, localId=%d", localId))
			send()
		}
	}

	internal fun sendFile(file: File?, replyToMessageId: Long?): ChatMessage? {
		if (file == null) {
			Log.d("prefilledmsg", "sendFile: file == null")
			return null
		}
		if (!file.exists()) {
			Log.d("prefilledmsg", "sendFile: file doesn't exist")
			return null
		}
		if (auth == null) {
			Log.d("prefilledmsg", "sendFile: auth == null")
			return null
		}

		auth?.Client?.let { client ->
			val localId = nextLocalId()
			val message = ChatMessage(client, localId, file, replyToMessageId)
			messages?.add(message)
			for (listener in messageListeners) {
				execute { listener.messageSent(message) }
			}
			Log.d("prefilledmsg", "start sendFile $message")
			sendFile(message)

			return message
		}

		return null
	}

	internal fun sendFile(message: ChatMessage) {
		if (auth == null) {
			return
		}
		if (message.UploadRequest != null) {
			return
		}
		val file = message.Upload
		if (file == null) {
			message.UploadExc = Exception("File is not found")
			return
		}
		if (!file.exists()) {
			message.UploadExc = Exception("File does not exist")
			return
		}
		var mimetype = ""
		val ext = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
		if (ext != null) {
			mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: ""
		}

		message.Sending = true
		message.UploadExc = null
		message.UploadRequest =
			client?.filesUpload(file, mimetype, object : HttpCallback<UploadedFile> {
				override fun onResult(result: UploadedFile?) {
					execute(Runnable {
						if (message.UploadRequest == null) {
							return@Runnable
						}
						message.Upload = null
						message.UploadExc = null
						message.UploadRequest = null
						message.UploadProgress = 100
						message.File = result
						Log.i(
							TAG,
							String.format("sendFile: Uploaded a file, fileId=%s", result?.Id)
						)
						val form =
							ChatMessageForm.file(localId, result?.Id, message.ReplyToMessageId)
						form.ChatType = chatType.name.lowercase()
						sendQueue.add(form)
						Log.i(
							TAG,
							String.format("Enqueued an outgoing message, localId=%d", localId)
						)
						send()
						for (listener in messageListeners) {
							execute { listener.messageUploaded(message) }
						}
					})
				}

				override fun onException(e: Exception) {
					execute(Runnable {
						if (message.UploadRequest == null) {
							return@Runnable
						}
						message.Sending = false
						message.UploadExc = e
						message.UploadProgress = 0
						message.UploadRequest = null
						Log.e(TAG, String.format("sendFile: Failed to upload a file, e=%s", e))
						for (listener in messageListeners) {
							execute { listener.messageUpdated(message) }
						}
					})
				}
			}, object : HttpProgressCallback {
				override fun onProgress(progress: Int) {
					execute {
						message.UploadProgress = progress
						for (listener in messageListeners) {
							execute { listener.messageUpdated(message) }
						}
					}
				}
			})

		Log.i(
			TAG, String.format(
				"Uploading a file, messageLocalId=%d, filename=%s",
				localId, file.name
			)
		)

		for (listener in messageListeners) {
			execute { listener.messageUpdated(message) }
		}
	}

	internal fun cancelUpload(message: ChatMessage) {
		if (auth == null) {
			return
		}
		if (message.Upload == null) {
			return
		}

		messages?.remove(message)
		message.UploadRequest?.cancel()

		for (listener in messageListeners) {
			execute { listener.messageCancelled(message) }
		}
	}

	private fun clearSend() {
		sendRequest?.cancel()
		sendAttempt = 0
		sendQueue.clear()
		sendRequest = null
		Log.d(TAG, "Cleared send queue")
	}

	private fun nextLocalId(): Long {
		var localId = Date().time
		if (localId <= this.localId) {
			localId = this.localId + 1
		}
		this.localId = localId
		return localId
	}

	private fun send() {
		if (auth == null) {
			return
		}
		if (sendQueue.isEmpty()) {
			return
		}
		if (sendRequest != null) {
			return
		}

		val form = sendQueue.removeAt(0)
		sendAttempt++
		config?.channel?.let { channel ->
			sendRequest =
				client?.chatsChannelSend(channel, form, object : HttpCallback<Void> {
					override fun onResult(result: Void?) {
						execute { sent(form) }
					}

					override fun onException(exception: Exception) {
						execute { sendException(exception, form) }
					}
				})

			Log.i(TAG, String.format("Sending a message, localId=%d", form.LocalId))
		}
	}

	private fun sendException(exception: Exception, form: ChatMessageForm) {
		if (sendRequest == null) {
			return
		}
		sendRequest = null

		if (auth == null) {
			Log.i(TAG, String.format("Failed to send a message, exc=%s", exception))
			return
		}

		sendQueue.add(0, form)
		val delaySec = Retry.delaySeconds(sendAttempt)
		handler?.postDelayed({ send() }, (delaySec * 1000).toLong())
		Log.e(
			TAG, String.format(
				"Failed to send a message, will retry in %d seconds, exc=%s",
				delaySec, exception
			)
		)
	}

	private fun sent(form: ChatMessageForm) {
		if (sendRequest == null) {
			return
		}
		sendRequest = null
		sendAttempt = 0
		Log.i(TAG, String.format("Sent a message, localId=%d", form.LocalId))
		send()
	}

	// File url
	fun filesUrl(fileId: String, callback: HttpCallback<String?>) {
		if (auth == null) {
			return
		}
		if (client == null) {
			return
		}

		client?.filesUrl(fileId, callback)
	}

	// Ratings
	internal fun ratingsRate(ratingId: Long, value: Int) {
		if (auth == null) {
			return
		}

		client?.ratingsRate(ratingId, value, object : HttpCallback<Void> {
			override fun onResult(result: Void?) {}
			override fun onException(exception: Exception) {}
		})

		Log.i(TAG, String.format("Sent rating, ratingId=%d, value=%d", ratingId, value))
	}

	internal fun ratingsSendPoll(
		answers: List<RatingPollClientAnswerInput>,
		ratingId: Long,
		pollId: Long,
		callback: HttpCallback<Void>,
	) {
		if (auth == null) {
			return
		}

		client?.sendPoll(answers, object : HttpCallback<Void> {
			override fun onResult(result: Void?) {
				client?.finishPoll(ratingId, pollId, true, callback)
			}

			override fun onException(exception: Exception) {
				callback.onException(exception)
			}
		})

		Log.i(
			TAG,
			String.format("Sent rating poll answers, ratingId=%d, pollId=%d", ratingId, pollId)
		)
	}

	// Typing
	internal fun sendTyping() {
		if (auth == null) {
			return
		}
		if (sendTypingRequest != null) {
			return
		}

		val body = ClientTypingForm().apply {
			ChatType = chatType.name.lowercase()
		}

		config?.channel?.let { channel ->
			sendTypingRequest =
				client?.chatsChannelTyping(channel, body, object : HttpCallback<Void> {
					override fun onResult(result: Void?) {
						Log.d("sendTypingRequest", "successfully sent self 'Typing...'")
					}

					override fun onException(exception: Exception) {
						Log.e("sendTypingRequest", exception.localizedMessage)
					}
				})

			handler?.postDelayed({ sendTypingRequest = null }, 5000)
			Log.i(TAG, String.format("Sending a typing message"))
		}
	}

	// Data
	private fun setMessages(messages: List<ChatMessage>) {
		this.messages = ArrayList(messages)
		for (message in messages) {
			enqueueReceived(message)
		}
	}

	private fun prependMessages(messages: List<ChatMessage>): List<ChatMessage> {
		val newMessages: MutableList<ChatMessage> = ArrayList(messages.size)
		for (message in messages) {
			if (getMessageById(message.Id) == null) {
				newMessages.add(message)
			}
		}

		this.messages?.addAll(0, newMessages)
		for (message in newMessages) {
			enqueueReceived(message)
		}

		return newMessages
	}

	private fun getMessageById(messageId: Long): ChatMessage? {
		messages?.let { messages ->
			for (message in messages) {
				if (message.Id == messageId) {
					return message
				}
			}
		}

		return null
	}

	private fun getMessageByLocalId(localId: Long): ChatMessage? {
		messages?.let { messages ->
			for (message in messages) {
				if (message.LocalId == localId) {
					return message
				}
			}
		}

		return null
	}

	private fun applyEvents(events: List<ChatEvent>) {
		for (event in events) {
			applyEvent(event)
		}
	}

	private fun applyEvent(event: ChatEvent) {
		if (event.Type == null) {
			Log.i(TAG, String.format("applyEvent: skipping %s", event))
			return
		}

		when (event.Type) {
			ChatEventType.MESSAGE_CREATED -> messageCreated(event)
			ChatEventType.MESSAGE_DELETED -> messageDeleted(event)
			ChatEventType.MESSAGE_RECEIVED -> messageReceived(event)
			ChatEventType.MESSAGE_READ -> messageRead(event)
			ChatEventType.RATING_IGNORED -> ratingIgnored(event)
			ChatEventType.TYPING -> messageTyping(event)
			ChatEventType.CHAT_CLOSED -> systemChat = false
			ChatEventType.CLOSE_SYSTEM_CHAT -> systemChat = false
			ChatEventType.CHAT_CHANNEL_CHANGE -> changeChannel(event)
			ChatEventType.FILE_UPDATED -> fileUpdated(event)
			else -> Log.i(TAG, String.format("applyEvent: %s", event.Type))
		}
	}

	private fun messageCreated(event: ChatEvent) {
		val message = event.Message ?: return
		if (message.My) {
			val existing = getMessageByLocalId(message.LocalId)
			if (existing != null) {
				existing.Id = message.Id
				existing.EventId = message.EventId
				existing.Payload = message.Payload
				existing.Text = message.Text
				existing.FileId = message.FileId
				existing.File = message.File
				existing.Client = message.Client
				existing.Sending = false
				existing.Received = true
				existing.ReplyToMessageId = message.ReplyToMessageId
				Log.i(
					TAG, String.format(
						"Received a message confirmation, localId=%d",
						message.LocalId
					)
				)
				for (listener in messageListeners) {
					execute { listener.messageUpdated(existing) }
				}
				return
			}
		}

		// Check for duplicates
		messages?.let { messages ->
			for (msg in messages) {
				if (msg.Id == message.Id) {
					return
				}
			}
		}

		messages?.add(message)
		Log.i(TAG, String.format("Received a new message, messageId=%d", message.Id))
		for (listener in messageListeners) {
			execute { listener.messageReceived(message) }
		}

		enqueueReceived(message)
	}

	private fun messageDeleted(event: ChatEvent) {
		val messagesToDelete = event.Messages

		if (messagesToDelete.isNullOrEmpty()) {
			return
		}

		for (chatMessageToDelete in messagesToDelete) {
			var oldMessage: ChatMessage? = null

			messages?.let {
				for (message in it) {
					if (message.Id == chatMessageToDelete.Id) {
						oldMessage = message
					}
				}
			}

			oldMessage?.let { oldMessage ->
				messages?.remove(oldMessage)
				Log.i(TAG, String.format("Deleted message, messageId=%d", oldMessage.Id))
			}

			for (listener in messageListeners) {
				execute { listener.messageDeleted(chatMessageToDelete) }
			}
		}
	}

	private fun messageReceived(event: ChatEvent) {
		event.MessageId?.let { messageId ->
			val message = getMessageById(messageId) ?: return

			val messageEventId = message.EventId
			if (messageEventId != null && messageEventId >= event.Id) {
				return
			}

			message.EventId = event.Id
			message.Received = true
			message.ReceivedAt = event.CreatedAt
			Log.i(TAG, String.format("Marked a message as received, messageId=%d", message.Id))
			for (listener in messageListeners) {
				execute { listener.messageUpdated(message) }
			}
		}
	}

	private fun messageRead(event: ChatEvent) {
		if (event.MessageId == null) {
			return
		}

		event.MessageId?.let { messageId ->
			val message = getMessageById(messageId) ?: return

			val messageEventId = message.EventId
			if (messageEventId != null && messageEventId >= event.Id) {
				return
			}

			message.EventId = event.Id
			message.Read = true
			message.ReadAt = event.CreatedAt
			if (!message.Received) {
				message.Received = true
				message.ReceivedAt = event.CreatedAt
			}
			Log.i(TAG, String.format("Marked a message as read, messageId=%d", message.Id))

			for (listener in messageListeners) {
				execute { listener.messageUpdated(message) }
			}
		}
	}

	private fun ratingIgnored(event: ChatEvent) {
		val message = event.Message ?: return

		val existing = getMessageByLocalId(message.LocalId)

		if (existing != null) {
			existing.Rating = message.Rating
			Log.i(
				TAG, String.format(
					"Received a rating confirmation, localId=%d",
					message.LocalId
				)
			)
			for (listener in messageListeners) {
				execute { listener.messageUpdated(existing) }
			}
			return
		}
	}

	private fun messageTyping(event: ChatEvent) {
		for (listener in messageListeners) {
			execute { listener.eventTyping(event) }
		}
	}

	private fun changeChannel(event: ChatEvent) {
		event.NextChannelName?.let { channel ->
			for (listener in messageListeners) {
				execute { listener.eventChangeChannel(channel) }
			}
		}
	}

	private fun fileUpdated(event: ChatEvent) {
		event.MessageId?.let { messageId ->
			val message = getMessageById(messageId) ?: return
			val fileId = message.FileId ?: return

			coroutineScope.launch(Dispatchers.IO) {
				try {
					val interactor =
						GetConfigsInteractorImpl(NetworkModule.provideGetConfigApiService())
					val file = interactor.getFile(fileId)
					file?.let { prepareFile(it) }
					Log.d(TAG, "success got file: ${file?.Id}")
					message.File = file

					for (listener in messageListeners) {
						execute { listener.messageUpdated(message) }
					}
				} catch (e: Exception) {
					Log.e(TAG, e.message, e)
				}
			}
		}
	}

	fun prepareFile(file: UploadedFile) {
		file.Url = file.Id?.let { fileUrl(it) }
		if (file.Type == FileType.IMAGE) {
			file.ImagePreviewUrl = fileImageUrl(file.Id, FileImageSize.PREVIEW)
			file.imageUrl = fileImageUrl(file.Id, FileImageSize.ORIGINAL)
		}
	}

	private fun fileUrl(fileId: String): String {
		return String.format("%s/public/api/v1/files/get/%s", getBaseUrl(), fileId)
	}

	private fun fileImageUrl(fileId: String?, size: FileImageSize): String {
		return String.format("%s/public/api/v1/files/image/%s?size=%s", getBaseUrl(), fileId, size)
	}
}
