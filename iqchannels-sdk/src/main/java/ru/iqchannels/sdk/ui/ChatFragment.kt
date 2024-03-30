/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.app.Callback
import ru.iqchannels.sdk.app.Cancellable
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.app.IQChannelsListener
import ru.iqchannels.sdk.app.MessagesListener
import ru.iqchannels.sdk.lib.InternalIO.copy
import ru.iqchannels.sdk.schema.Action
import ru.iqchannels.sdk.schema.ActionType
import ru.iqchannels.sdk.schema.ChatEvent
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ClientAuth
import ru.iqchannels.sdk.schema.SingleChoice
import ru.iqchannels.sdk.ui.backdrop.ErrorPageBackdropDialog
import ru.iqchannels.sdk.ui.images.ImagePreviewFragment
import ru.iqchannels.sdk.ui.rv.SwipeController
import ru.iqchannels.sdk.ui.widgets.ReplyMessageView
import ru.iqchannels.sdk.ui.widgets.TopNotificationWidget

class ChatFragment : Fragment() {

	companion object {
		private const val TAG = "iqchannels"
		private const val SEND_FOCUS_SCROLL_THRESHOLD_PX = 300

		/**
		 * Use this factory method to create a new instance of
		 * this fragment using the provided parameters.
		 *
		 * @return A new instance of fragment ChatFragment.
		 */
		fun newInstance(): ChatFragment {
			val fragment = ChatFragment()
			val args = Bundle()
			fragment.arguments = args
			return fragment
		}
	}

	private var iqchannelsListenerCancellable: Cancellable? = null

	// Messages
	private var messagesLoaded = false
	private var messagesRequest: Cancellable? = null
	private var moreMessagesRequest: Cancellable? = null

	// Auth layout
	private var authLayout: RelativeLayout? = null

	// Signup layout
	private var signupLayout: LinearLayout? = null
	private var signupText: EditText? = null
	private var signupButton: Button? = null
	private var signupError: TextView? = null

	// Chat layout
	private var chatLayout: RelativeLayout? = null
	private var tnwMsgCopied: TopNotificationWidget? = null

	// Message views
	private var progress: ProgressBar? = null
	private var refresh: SwipeRefreshLayout? = null
	private var adapter: ChatMessagesAdapter? = null
	private var recycler: RecyclerView? = null
	private var btnScrollToBottom: ImageView? = null

	// Send views
	private var sendText: EditText? = null
	private var attachButton: ImageButton? = null
	private var sendButton: ImageButton? = null
	private var clReply: ReplyMessageView? = null

	// Camera and gallery
	private var cameraTempFile: File? = null
	private var onDownloadComplete: BroadcastReceiver? = null
	private var replyingMessage: ChatMessage? = null

	private val requestAllPermissions =
		registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
			if (permissions[Manifest.permission.CAMERA] == true) {
				showAttachChooser(true)
			} else {
				showAttachChooser(false)
			}
		}

	private val requestPickImageFromFiles =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
			if (it.resultCode == Activity.RESULT_OK) {
				val intent = it.data
				val uri = intent?.data

				when(uri == null) {
					true -> { // multiple choice
						it.data?.clipData?.let { clipData ->
							val uris = ArrayList<Uri>()
							val itemCount = if (clipData.itemCount > 10) 10 else clipData.itemCount
							for (i in 0 until itemCount) {
								uris.add(clipData.getItemAt(i).uri)
							}

							sendMultipleFiles(uris)
						}
					}
					false -> { // single choice
						var isCamera = false
						if (!isCamera) {
							val action = intent.action
							isCamera = MediaStore.ACTION_IMAGE_CAPTURE == action
						}
						if (!isCamera) {
							isCamera = uri == null
						}
						if (isCamera) {
							onCameraResult(it.resultCode)
						} else {
							onGalleryResult(uri)
						}
					}
				}
			}
		}

	private val multipleFilesQueue: MutableList<Uri> = mutableListOf()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val view = inflater.inflate(R.layout.fragment_chat, container, false)

		// Auth views.
		authLayout = view.findViewById<View>(R.id.authLayout) as RelativeLayout

		// Login views.
		signupLayout = view.findViewById<View>(R.id.signupLayout) as LinearLayout
		signupText = view.findViewById<View>(R.id.signupName) as EditText
		signupButton = view.findViewById<View>(R.id.signupButton) as Button
		clReply = view.findViewById(R.id.reply)
		signupButton?.setOnClickListener { signup() }
		signupError = view.findViewById<View>(R.id.signupError) as TextView

		// Chat.
		chatLayout = view.findViewById<View>(R.id.chatLayout) as RelativeLayout
		tnwMsgCopied = view.findViewById(R.id.tnw_msg_copied)

		// Messages.
		progress = view.findViewById<View>(R.id.messagesProgress) as ProgressBar
		refresh = view.findViewById<View>(R.id.messagesRefresh) as SwipeRefreshLayout
		refresh?.isEnabled = false
		refresh?.setOnRefreshListener { refreshMessages() }

		adapter = ChatMessagesAdapter(
			IQChannels,
			view,
			{ view.width to view.height },
			ItemClickListener()
		)

		recycler = view.findViewById(R.id.messages)
		recycler?.adapter = adapter
		btnScrollToBottom = view.findViewById(R.id.iv_scroll_down)

		btnScrollToBottom?.setOnClickListener {
			adapter?.itemCount?.let {
				recycler?.scrollToPosition(it - 1)
			}
		}

		recycler?.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
			maybeScrollToBottomOnKeyboardShown(
				bottom,
				oldBottom
			)
		}

		val swipeController = SwipeController(object : SwipeController.SwipeListener {

			override fun onSwiped(position: Int) {
				val chatMessage = adapter?.getItem(position) ?: return
				replyingMessage = chatMessage
				clReply?.showReplyingMessage(chatMessage)
				clReply?.post { maybeScrollToBottomOnNewMessage() }
			}
		})

		val itemTouchHelper = ItemTouchHelper(swipeController)
		itemTouchHelper.attachToRecyclerView(recycler)
		clReply?.setCloseBtnClickListener { hideReplying() }

		// Send.
		sendText = view.findViewById(R.id.sendText)
		sendText?.setOnEditorActionListener { v, actionId, event ->
			var handled = false
			if (actionId == EditorInfo.IME_ACTION_SEND) {
				sendMessage()
				handled = true
			}
			handled
		}

		sendText?.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
				IQChannels.sendTyping()
			}

			override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable) {
				sendButton?.isVisible = s.isNotEmpty()
			}
		})

		attachButton = view.findViewById(R.id.attachButton)
		attachButton?.setOnClickListener { showAttachChooser() }
		sendButton = view.findViewById(R.id.sendButton)
		sendButton?.setOnClickListener { sendMessage() }

		if (!requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
			attachButton?.visibility = View.GONE
		}

		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		childFragmentManager.setFragmentResultListener(
			FileActionsChooseFragment.REQUEST_KEY,
			this
		) { _: String?, bundle: Bundle ->
			val downloadID = bundle.getLong(FileActionsChooseFragment.KEY_DOWNLOAD_ID)
			val fileName = bundle.getString(FileActionsChooseFragment.KEY_FILE_NAME)
			handleDownload(downloadID, fileName)
		}

		recycler?.addOnScrollListener(object : OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
				val messagesCount = adapter?.itemCount ?: return
				val lastVisibleItemPosition = lm.findLastVisibleItemPosition()
				btnScrollToBottom?.isVisible = lastVisibleItemPosition < messagesCount - 5
			}
		})
	}

	override fun onDestroyView() {
		tnwMsgCopied?.destroy()
		super.onDestroyView()
	}

	override fun onDestroy() {
		if (onDownloadComplete != null) {
			context?.unregisterReceiver(onDownloadComplete)
		}
		super.onDestroy()
	}

	private fun updateViews() {
		authLayout?.visibility =
			if (IQChannels.auth == null && IQChannels.authRequest != null) View.VISIBLE else View.GONE
		signupLayout?.visibility =
			if (IQChannels.auth == null && IQChannels.authRequest == null) View.VISIBLE else View.GONE
		signupButton?.isEnabled = IQChannels.authRequest == null
		signupText?.isEnabled = IQChannels.authRequest == null
		chatLayout?.visibility = if (IQChannels.auth != null) View.VISIBLE else View.GONE
	}

	override fun onStart() {
		super.onStart()

		iqchannelsListenerCancellable = IQChannels.addListener(object : IQChannelsListener {
			override fun authenticating() {
				signupError?.text = ""
				updateViews()
			}

			override fun authComplete(auth: ClientAuth) {
				signupError?.text = ""
				loadMessages()
				updateViews()
			}

			override fun authFailed(e: Exception) {
				signupError?.text = String.format("Ошибка: %s", e.localizedMessage)
				updateViews()
			}
		})

		if (IQChannels.auth != null) {
			loadMessages()
		}
		updateViews()
	}

	override fun onStop() {
		super.onStop()

		if (iqchannelsListenerCancellable != null) {
			iqchannelsListenerCancellable?.cancel()
			iqchannelsListenerCancellable = null
		}

		clearMessages()
		clearMoreMessages()
	}

	// Messages scroll
	private fun maybeScrollToBottomOnKeyboardShown(bottom: Int, oldBottom: Int) {
		if (sendText?.hasFocus() != true) {
			return
		}

		if (bottom >= oldBottom) {
			return
		}

		recycler?.let { recycler ->
			val extent = recycler.computeVerticalScrollExtent()
			val offset = recycler.computeVerticalScrollOffset()
			val range = recycler.computeVerticalScrollRange()
			if (range - (oldBottom - bottom) - (extent + offset) > SEND_FOCUS_SCROLL_THRESHOLD_PX) {
				return
			}
			val count = adapter!!.itemCount
			recycler.smoothScrollToPosition(if (count == 0) 0 else count - 1)
		}
	}

	private fun maybeScrollToBottomOnNewMessage() {
		recycler?.let { recycler ->
			val extent = recycler.computeVerticalScrollExtent()
			val offset = recycler.computeVerticalScrollOffset()
			val range = recycler.computeVerticalScrollRange()
			if (range - (extent + offset) > SEND_FOCUS_SCROLL_THRESHOLD_PX) {
				return
			}
			val count = adapter?.itemCount ?: 0
			recycler.smoothScrollToPosition(if (count == 0) 0 else count - 1)
		}
	}

	// Signup
	private fun signup() {
		val name = signupText?.text?.toString() ?: return
		if (name.length < 3) {
			signupError!!.text = "Ошибка: длина имени должна быть не менее 3-х символов."
			return
		}

		signupError?.text = ""
		IQChannels.signup(name)
	}

	// Messages
	private fun clearMessages() {
		messagesRequest?.cancel()
		messagesLoaded = false
		messagesRequest = null
		adapter?.clear()
		progress?.visibility = View.GONE
		refresh?.isRefreshing = false
		refresh?.isEnabled = false
	}

	private fun refreshMessages() {
		if (!messagesLoaded) {
			if (messagesRequest != null) {
				refresh?.isRefreshing = false
				return
			}

			// Load messages.
			loadMessages()
			return
		}
		loadMoreMessages()
	}

	private fun loadMessages() {
		if (messagesLoaded) {
			return
		}
		if (messagesRequest != null) {
			return
		}

		// Show the progress bar only when the refresh control is not active already.
		disableSend()
		progress?.visibility = if (refresh?.isRefreshing == true) View.GONE else View.VISIBLE

		messagesRequest = IQChannels.loadMessages(object : MessagesListener {
			override fun messagesLoaded(messages: List<ChatMessage>) {
				this@ChatFragment.messagesLoaded(messages)
			}

			override fun messagesException(e: Exception) {
				this@ChatFragment.messagesException(e)
			}

			override fun messagesCleared() {
				clearMessages()
			}

			override fun messageReceived(message: ChatMessage) {
				this@ChatFragment.messageReceived(message)
			}

			override fun messageSent(message: ChatMessage) {
				this@ChatFragment.messageSent(message)
			}

			override fun messageUploaded(message: ChatMessage) {
				this@ChatFragment.messageUploaded(message)
			}

			override fun messageUpdated(message: ChatMessage) {
				this@ChatFragment.messageUpdated(message)
			}

			override fun messageCancelled(message: ChatMessage) {
				this@ChatFragment.messageCancelled(message)
			}

			override fun messageDeleted(message: ChatMessage) {
				this@ChatFragment.messageDeleted(message)
			}

			override fun eventTyping(event: ChatEvent) {
				this@ChatFragment.eventTyping(event)
			}
		})
	}

	private fun checkDisableFreeText(message: ChatMessage) {
		disableFreeText(message.DisableFreeText == true)
	}

	private fun disableFreeText(disable: Boolean) {
		sendText?.isEnabled = !disable
		sendText?.isFocusable = !disable
		sendText?.isFocusableInTouchMode = !disable
		attachButton?.isClickable = !disable
	}

	private fun messagesLoaded(messages: List<ChatMessage>) {
		if (messagesRequest == null) {
			return
		}

		messagesLoaded = true
		if (messages.isNotEmpty()) {
			val lastMsg = messages[messages.size - 1]
			checkDisableFreeText(lastMsg)
		}
		enableSend()

		adapter?.loaded(messages)
		recycler?.scrollToPosition(if (messages.isEmpty()) 0 else messages.size - 1)
		progress?.visibility = View.GONE
		refresh?.isRefreshing = false
		refresh?.isEnabled = true
	}

	private fun messagesException(e: Exception) {
		if (messagesRequest == null) {
			return
		}

		messagesRequest = null
		progress?.visibility = View.GONE
		refresh?.isRefreshing = false
		refresh?.isEnabled = true
		showMessagesErrorAlert(e)
	}

	private fun messageReceived(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		checkDisableFreeText(message)
		adapter?.received(message)
		maybeScrollToBottomOnNewMessage()
	}

	private fun messageSent(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		adapter?.sent(message)
		maybeScrollToBottomOnNewMessage()
	}

	private fun messageUploaded(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		adapter?.updated(message)
		maybeScrollToBottomOnNewMessage()

		runCatching { multipleFilesQueue.removeFirst() }
			.getOrNull()
			?.let { sendFile(it) }

	}

	private fun messageCancelled(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		adapter?.cancelled(message)
	}

	private fun messageDeleted(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		adapter?.deleted(message)
	}

	private fun eventTyping(event: ChatEvent) {
		adapter?.typing(event)
		maybeScrollToBottomOnNewMessage()
	}

	private fun messageUpdated(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		checkDisableFreeText(message)
		adapter?.updated(message)
	}

	// More messages
	private fun clearMoreMessages() {
		moreMessagesRequest?.cancel()
		moreMessagesRequest = null
	}

	private fun loadMoreMessages() {
		if (!messagesLoaded) {
			refresh?.isRefreshing = false
			return
		}
		if (moreMessagesRequest != null) {
			return
		}

		moreMessagesRequest = IQChannels.loadMoreMessages(object : Callback<List<ChatMessage>?> {
			override fun onResult(result: List<ChatMessage>?) {
				result?.let { moreMessagesLoaded(it) }
			}

			override fun onException(e: Exception) {
				moreMessagesException(e)
			}
		})
	}

	private fun moreMessagesException(e: Exception) {
		if (moreMessagesRequest == null) {
			return
		}

		moreMessagesRequest = null
		refresh?.isRefreshing = false
		showMessagesErrorAlert(e)
	}

	private fun moreMessagesLoaded(moreMessages: List<ChatMessage>) {
		if (moreMessagesRequest == null) {
			return
		}

		moreMessagesRequest = null
		refresh?.isRefreshing = false
		adapter?.loadedMore(moreMessages)
	}

	// Attach
	private fun showAttachChooser() {
		if (ContextCompat.checkSelfPermission(
				requireContext(),
				Manifest.permission.CAMERA
			) == PackageManager.PERMISSION_DENIED
		) {
			requestAllPermissions.launch(
				arrayOf(
					Manifest.permission.CAMERA,
					Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE
				)
			)

			return
		}

		showAttachChooser(true)
	}

	private fun showAttachChooser(withCamera: Boolean) {
		// Try to create a camera intent.
		var cameraIntent: Intent? = null
		if (withCamera) {
			val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
			try {
				if (intent.resolveActivity(requireActivity().packageManager) != null) {
					val tmpDir: File? = Environment.getExternalStorageDirectory()
					val tmp = File.createTempFile("image", ".jpg", tmpDir)
					tmp.deleteOnExit()
					intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmp))
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
					intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
					cameraTempFile = tmp
				}
				cameraIntent = intent
			} catch (e: IOException) {
				Log.e(
					TAG, String.format(
						"showAttachChooser: Failed to create a temp file for the camera, e=%s", e
					)
				)
			}
		}

		// Create a gallery intent.
		val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
		galleryIntent.setType("image/*")
		galleryIntent.putExtra(
			Intent.EXTRA_MIME_TYPES, arrayOf(
				"audio/*", "video/*", "text/*", "application/*", "file/*"
			)
		)
		galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

		// Create and start an intent chooser.
		val title = resources.getText(R.string.chat_camera_or_file)
		val chooser = Intent.createChooser(galleryIntent, title)
		if (cameraIntent != null) {
			chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(cameraIntent))
		}

		requestPickImageFromFiles.launch(chooser)
	}

	// Gallery
	private fun onGalleryResult(uri: Uri) {

		lifecycleScope.launch(Dispatchers.IO) {
			val result = prepareFile(uri)

			withContext(Dispatchers.Main) {
				result?.let {
					showConfirmDialog(it, it.name)
				}
			}
		}
	}

	private fun onGalleryMutipleFilesResult(uri: Uri) {

		lifecycleScope.launch(Dispatchers.IO) {
			val result = prepareFile(uri)

			withContext(Dispatchers.Main) {
				result?.let {
					showConfirmDialog(it, getString(R.string.chat_send_multiple_file_confirmation))
				}
			}
		}
	}

	private fun sendMultipleFiles(fileUris: List<Uri>) {
		lifecycleScope.launch {
			multipleFilesQueue.addAll(fileUris)
			val uri = multipleFilesQueue.removeFirst()
			onGalleryMutipleFilesResult(uri)
		}
	}

	private fun sendFile(uri: Uri) {
		val file = prepareFile(uri)
		IQChannels.sendFile(file, null)
	}

	private fun prepareFile(uri: Uri) = try {
		val resolver = requireActivity().contentResolver
		val mimeTypeMap = MimeTypeMap.getSingleton()
		val mtype = resolver.getType(uri)
		val ext = mimeTypeMap.getExtensionFromMimeType(mtype)
		val file = createGalleryTempFile(uri, ext)
		val `in` = resolver.openInputStream(uri)

		if (`in` == null) {
			Log.e(TAG, "onGalleryResult: Failed to pick a file, no input stream")
			null
		} else {
			`in`.use { `in` ->
				val out = FileOutputStream(file)
				out.use { out ->
					copy(`in`, out)
				}
			}
			file
		}
	} catch (e: IOException) {
		Log.e(TAG, String.format("onGalleryResult: Failed to pick a file, e=%s", e))
		null
	}

	private fun showConfirmDialog(file: File, message: String) {
		val builder = AlertDialog.Builder(context)
			.setTitle(R.string.chat_send_file_confirmation)
			.setMessage(message)
			.setPositiveButton(R.string.ok) { dialogInterface: DialogInterface?, i: Int ->
				var replyToMessageId: Long? = null
				if (replyingMessage != null) {
					replyToMessageId = replyingMessage?.Id
				}
				IQChannels.sendFile(file, replyToMessageId)
				hideReplying()
			}
			.setNegativeButton(R.string.cancel, null)
		builder.show()
	}

	private fun hideReplying() {
		clReply?.visibility = View.GONE
		replyingMessage = null
	}

	@Throws(IOException::class)
	private fun createGalleryTempFile(uri: Uri, ext: String?): File {
		var ext = ext
		var filename = getGalleryFilename(uri)
		if (filename != null) {
			val i = filename.lastIndexOf(".")
			if (i > -1) {
				ext = filename.substring(i + 1)
				filename = filename.substring(0, i - 1)
			}
		} else {
			filename = "file"
			val mimeType = activity?.contentResolver?.getType(uri)
			ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
		}

		if (filename.length < 3) {
			filename = "file-$filename"
		}
		val file = File.createTempFile(filename, ".$ext", activity?.cacheDir)
		file.deleteOnExit()
		return file
	}

	private fun getGalleryFilename(uri: Uri): String? {
		var path = uri.path
		val i = path?.lastIndexOf("/")
		if (i != null) {
			if (i > -1) {
				path = path?.substring(i + 1)
			}
		}

		return path
	}

	// Camera
	private fun onCameraResult(resultCode: Int) {
		if (resultCode != Activity.RESULT_OK || cameraTempFile == null) {
			Log.i(
				TAG, String.format(
					"onCameraResult: Did not capture a photo, activity result=%d", resultCode
				)
			)

			cameraTempFile?.delete()
			return
		}
		val file: File

		try {
			// Create a dst file.
			var dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
			val app = resources.getString(R.string.app_name)
			if (app.isNotEmpty()) {
				dir = File(dir, app)
				dir.mkdirs()
			}
			val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_").format(Date())
			file = File.createTempFile(timestamp, ".jpg", dir)
			copy(cameraTempFile, file)
			cameraTempFile?.delete()
			cameraTempFile = null
		} catch (e: IOException) {
			Log.e(TAG, String.format("showCamera: Failed to save a captured file, error=%s", e))
			return
		}

		addCameraPhotoToGallery(file)
		showConfirmDialog(file, file.name)
	}

	private fun addCameraPhotoToGallery(file: File) {
		val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
		val contentUri = Uri.fromFile(file)
		scanIntent.setData(contentUri)
		activity?.sendBroadcast(scanIntent)
	}

	// Send
	private fun enableSend() {
		sendText?.isEnabled = true
		attachButton?.isEnabled = true
		sendButton?.isEnabled = true
	}

	private fun disableSend() {
		sendText?.isEnabled = false
		attachButton?.isEnabled = false
		sendButton?.isEnabled = false
	}

	private fun sendMessage() {
		val text = sendText?.text.toString()
		sendText?.setText("")

		if (text == "/version_sdk") {
			IQChannels.handleVersion()
			return
		}

		var replyToMessageId: Long? = null
		if (replyingMessage != null) {
			replyToMessageId = replyingMessage?.Id
		}

		IQChannels.send(text, replyToMessageId)
		hideReplying()
	}

	private fun sendMessage(text: String?) {
		IQChannels.send(text, null)
		hideReplying()
	}

	private fun sendSingleChoice(singleChoice: SingleChoice) {
		IQChannels.sendPostbackReply(singleChoice.title, singleChoice.value)
	}

	private fun sendAction(action: Action) {
		IQChannels.sendPostbackReply(action.Title, action.Payload)
	}

	// Error alerts
	private fun showMessagesErrorAlert(e: Exception?) {
		val builder = AlertDialog.Builder(context)
			.setTitle(R.string.chat_failed_to_load_messages)
			.setNeutralButton(R.string.ok, null)

		if (e != null) {
			builder.setMessage(e.toString())
		} else {
			builder.setMessage(R.string.unknown_exception)
		}

		builder.show()
	}

	private fun handleDownload(downloadID: Long, fileName: String?) {
		if (downloadID > 0) {
			onDownloadComplete = object : BroadcastReceiver() {
				override fun onReceive(context: Context, intent: Intent) {
					val action = intent.action
					if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
						val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
						Log.d(TAG, "received: $downloadId")
						if (downloadID != downloadId) return
						val downloadManager =
							context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
						val query = DownloadManager.Query()
						query.setFilterById(downloadId)
						val cursor = downloadManager.query(query)
						if (cursor.moveToFirst()) {
							val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
							val status = cursor.getInt(columnIndex)
							if (status == DownloadManager.STATUS_SUCCESSFUL) {
								// Загрузка завершена успешно
								Log.d(TAG, "SUCCESS")
								Toast.makeText(
									context,
									getString(R.string.file_saved_success_msg, fileName),
									Toast.LENGTH_LONG
								).show()
							} else if (status == DownloadManager.STATUS_FAILED) {
								val columnReason =
									cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
								val reason = cursor.getInt(columnReason)
								// Обработка ошибки загрузки
								Log.d(TAG, "FAILED")
								Toast.makeText(
									context,
									getString(R.string.file_saved_fail_msg, fileName),
									Toast.LENGTH_LONG
								).show()
							}
						}
						cursor.close()
						context.unregisterReceiver(this)
					}
				}
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				context?.registerReceiver(
					onDownloadComplete,
					IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
					Context.RECEIVER_NOT_EXPORTED
				)
			} else {
				context?.registerReceiver(
					onDownloadComplete,
					IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
				)
			}
		}
	}

	private inner class ItemClickListener : ChatMessagesAdapter.ItemClickListener {
		override fun onFileClick(url: String, fileName: String) {
			val fragmentTransaction = childFragmentManager.beginTransaction()
			fragmentTransaction.add(FileActionsChooseFragment.newInstance(url, fileName), null)
			fragmentTransaction.commit()
		}

		override fun onImageClick(message: ChatMessage) {
			val senderName: String? = if (message.My) {
				if (message.Client != null) message.Client?.Name else ""
			} else {
				if (message.User != null) message.User?.DisplayName else ""
			}

			val imageUrl: String? = if (message.File != null) message.File?.imageUrl else ""
			val date = message.Date
			val msg = message.Text

			if (senderName != null && imageUrl != null && date != null && msg != null) {
				val transaction = parentFragmentManager.beginTransaction()
				val fragment = ImagePreviewFragment.newInstance(
					senderName, date, imageUrl, msg
				)

				transaction.replace((view?.parent as ViewGroup).id, fragment)
				transaction.addToBackStack(null)
				transaction.commit()
			}
		}

		override fun onButtonClick(message: ChatMessage, singleChoice: SingleChoice) {
			sendSingleChoice(singleChoice)
			disableFreeText(false)
		}

		override fun onActionClick(message: ChatMessage, action: Action) {
			when (action.Action) {
				ActionType.POSTBACK -> sendAction(action)
				ActionType.OPEN_URL -> {
					val intent = Intent(Intent.ACTION_VIEW)
					intent.setData(Uri.parse(action.URL))
					startActivity(intent)
				}

				ActionType.SAY_SOMETHING -> sendMessage(action.Title)
				else -> Unit
			}
		}

		override fun onMessageLongClick(message: ChatMessage) {
			message.Text?.let { text ->
				val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
				val clip = ClipData.newPlainText(text, text)
				clipboard.setPrimaryClip(clip)

				tnwMsgCopied?.show()
			}
		}

		override fun fileUploadException(errorMessage: String?) {
			val backdrop = ErrorPageBackdropDialog.newInstance(errorMessage)
			backdrop.show(childFragmentManager, ErrorPageBackdropDialog.TRANSACTION_TAG)
		}
	}
}
