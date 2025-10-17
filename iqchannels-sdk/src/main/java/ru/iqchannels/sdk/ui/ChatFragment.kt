/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.iqchannels.sdk.IQLog
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.app.Callback
import ru.iqchannels.sdk.app.Cancellable
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.app.IQChannels.sendingFile
import ru.iqchannels.sdk.app.IQChannelsConfig
import ru.iqchannels.sdk.app.IQChannelsConfigRepository
import ru.iqchannels.sdk.app.IQChannelsListener
import ru.iqchannels.sdk.app.MessagesListener
import ru.iqchannels.sdk.applyIQStyles
import ru.iqchannels.sdk.domain.models.ChatType
import ru.iqchannels.sdk.domain.models.PreFilledMessages
import ru.iqchannels.sdk.download.FileConfigChecker
import ru.iqchannels.sdk.http.HttpException
import ru.iqchannels.sdk.lib.InternalIO.copy
import ru.iqchannels.sdk.localization.IQChannelsLanguage
import ru.iqchannels.sdk.schema.Action
import ru.iqchannels.sdk.schema.ActionType
import ru.iqchannels.sdk.schema.ActorType
import ru.iqchannels.sdk.schema.ChatEvent
import ru.iqchannels.sdk.schema.ChatException
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ClientAuth
import ru.iqchannels.sdk.schema.Language
import ru.iqchannels.sdk.schema.SingleChoice
import ru.iqchannels.sdk.setBackgroundDrawable
import ru.iqchannels.sdk.styling.IQChannelsStyles
import ru.iqchannels.sdk.styling.IQStyles
import ru.iqchannels.sdk.ui.backdrop.ErrorPageBackdropDialog
import ru.iqchannels.sdk.ui.images.ImagePreviewFragment
import ru.iqchannels.sdk.ui.nav_bar.NavBar
import ru.iqchannels.sdk.ui.results.ClassParcelable
import ru.iqchannels.sdk.ui.results.IQChatEvent
import ru.iqchannels.sdk.ui.results.toParcelable
import ru.iqchannels.sdk.ui.rv.SwipeController
import ru.iqchannels.sdk.ui.theming.IQChannelsTheme
import ru.iqchannels.sdk.ui.widgets.ReplyMessageView
import ru.iqchannels.sdk.ui.widgets.FileMessageView
import ru.iqchannels.sdk.ui.widgets.TopNotificationWidget
import ru.iqchannels.sdk.ui.widgets.toPx
import kotlin.math.roundToInt

class ChatFragment : Fragment() {

	companion object {
		const val REQUEST_KEY = "ChatFragment#requestKey"
		const val RESULT_KEY_EVENT = "ChatFragment#resultKeyEvent"
		internal const val TAG = "iqchannels"
		private const val SEND_FOCUS_SCROLL_THRESHOLD_PX = 300
		private const val PARAM_LM_STATE = "ChatFragment#lmState"
		private const val ARG_TITLE = "ChatFragment#title"
		private const val ARG_STYLES = "ChatFragment#styles"
		private const val ARG_LANGUAGE = "ChatFragment#language"
		private const val ARG_HANDLED_EVENTS = "ChatFragment#handledEvents"
		private const val ARG_PREFILLED_MSG = "ChatFragment#preFilledMsg"

		/**
		 * Use this factory method to create a new instance of
		 * this fragment using the provided parameters.
		 *
		 * @return A new instance of fragment ChatFragment.
		 */
		fun newInstance(
			title: String? = null,
			stylesJson: String? = null,
			localizationJson: String? = null,
			handledEvents: List<Class<out IQChatEvent>>? = null,
			preFilledMessages: PreFilledMessages? = null
		): ChatFragment {
			val fragment = ChatFragment()
			val args = Bundle().apply {
				putString(ARG_TITLE, title)
				putString(ARG_STYLES, stylesJson)
				putString(ARG_LANGUAGE, localizationJson)
				putParcelableArray(
					ARG_HANDLED_EVENTS,
					handledEvents?.map { it.toParcelable() }?.toTypedArray()
				)
				putParcelable(ARG_PREFILLED_MSG, preFilledMessages)
			}
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
	private var signupTextName: EditText? = null
	private var signupButton: Button? = null
	private var signupError: TextView? = null
	private var signupTitle: TextView? = null
	private var signupSubtitle: TextView? = null
	private var signupCheckBox: CheckBox? = null

	// Chat layout
	private var chatLayout: RelativeLayout? = null
	private var chatUnavailableLayout: ConstraintLayout? = null
	private var chatUnavailableErrorText: TextView? = null
	private var tnwMsgCopied: TopNotificationWidget? = null
	private var btnGoBack: Button? = null

	// Message views
	private var progress: ProgressBar? = null
	private var refresh: SwipeRefreshLayout? = null
	private var adapter: ChatMessagesAdapter? = null
	private var recycler: RecyclerView? = null
	private var btnScrollToBottom: FrameLayout? = null
	private var btnScrollToBottomDot: ImageView? = null

	// Send views
	private var sendText: EditText? = null
	private var attachButton: ImageButton? = null
	private var sendButton: ImageButton? = null
	private var clReply: ReplyMessageView? = null
	private var clFile: FileMessageView? = null

	// Camera and gallery
	private var cameraTempFile: File? = null
	private var selectedFile: File? = null
	private var replyingMessage: ChatMessage? = null

	private val requestAllPermissions =
		registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
			showAttachChooser(false)
		}

	private val requestPickImageFromFiles =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
			if (it.resultCode == Activity.RESULT_OK) {
				val intent = it.data
				val uri = intent?.data

				clFile?.imageView?.setImageResource(R.drawable.doc_32)
				clFile?.imageView?.imageTintList =
					context?.let { it1 -> ContextCompat.getColor(it1, R.color.other_file_icon) }
						?.let { it2 ->
							ColorStateList.valueOf(
								it2
							)
						}

				when (uri == null) {
					true -> { // multiple choice
						it.data?.clipData?.let { clipData ->
							val uris = ArrayList<Uri>()
							clFile?.imageView?.imageTintList = null
							clFile?.imageView?.scaleType = ImageView.ScaleType.CENTER_CROP
							val itemCount = clipData.itemCount
							for (i in 0 until itemCount) {
								val itemUri = clipData.getItemAt(i).uri
								clFile?.imageView?.setImageURI(itemUri)
								uris.add(itemUri)
							}

							val checkedFiles = context?.let { context ->
								FileConfigChecker.checkFiles(context, uris, childFragmentManager)
							} ?: uris

							when (checkedFiles.size) {
								1 -> {
									onGalleryResult(checkedFiles.first())
								}

								else -> {
									sendMultipleFiles(checkedFiles)
								}
							}
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
							val ctx = context

							val mimeType = context?.contentResolver?.getType(uri)
							if (mimeType != null && mimeType.startsWith("image/")) {
								clFile?.imageView?.imageTintList = null
								clFile?.imageView?.scaleType = ImageView.ScaleType.CENTER_CROP
								clFile?.imageView?.setImageURI(uri)
							}

							val checkedFile = if (ctx != null) {
								FileConfigChecker.checkFiles(ctx, listOf(uri), childFragmentManager).firstOrNull()
							} else {
								uri
							}

							checkedFile?.let {
								onGalleryResult(checkedFile)
							}
						}
					}
				}
			}
		}

	private var lmState: Parcelable? = null

	private val handledEvents: List<Class<IQChatEvent>>?
		get() {
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				arguments?.getParcelableArray(ARG_HANDLED_EVENTS, ClassParcelable::class.java)?.map {
					it.clazz
				} as? List<Class<IQChatEvent>>
			} else {
				(arguments?.getParcelableArray(ARG_HANDLED_EVENTS)?.toList() as? List<ClassParcelable<Class<IQChatEvent>>>)?.map {
					it.clazz
				} as? List<Class<IQChatEvent>>
			}
		}

	private var lastUnreadMsgIndex = -1

	private val viewModel: ChatViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		viewModel.getConfigs()
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {

		arguments?.getString(ARG_STYLES)?.let { json ->
			try {
				Gson().fromJson(json, TypeToken.get(IQChannelsStyles::class.java))?.also {
					IQStyles.iqChannelsStyles = it
				}
			} catch (e: Exception) {
				IQLog.e("ChatFragment", "Error on parsing", e)
			}
		}

		arguments?.getString(ARG_LANGUAGE)?.let { json ->
			try {
				Gson().fromJson(json, TypeToken.get(Language::class.java))?.also {
					IQChannelsLanguage.iqChannelsLanguage = it
				}
			} catch (e: Exception) {
				IQLog.e("ChatFragment", "Error on parsing", e)
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			arguments?.getParcelable(ARG_PREFILLED_MSG, PreFilledMessages::class.java)?.let {
				viewModel.applyPrefilledMessages(it)
			}
		} else {
			(arguments?.getParcelable(ARG_PREFILLED_MSG) as? PreFilledMessages)?.let {
				viewModel.applyPrefilledMessages(it)
			}
		}

		val view = inflater.inflate(R.layout.fragment_chat, container, false)

		view.setBackgroundColor(
			IQStyles.iqChannelsStyles?.chat?.background?.getColorInt(requireContext())
				?: ContextCompat.getColor(requireContext(), R.color.white)
		)

		// Auth views.
		authLayout = view.findViewById<View>(R.id.authLayout) as RelativeLayout

		// Login views.
		signupLayout = view.findViewById<View>(R.id.signupLayout) as LinearLayout
		signupTitle = view.findViewById<View>(R.id.signupTitle) as TextView
		signupSubtitle = view.findViewById<View>(R.id.signupSubtitle) as TextView
		signupCheckBox = view.findViewById<View>(R.id.signupCheckBox) as CheckBox
		signupTextName = view.findViewById<View>(R.id.signupName) as EditText
		signupButton = view.findViewById<View>(R.id.signupButton) as Button
		signupButton?.setOnClickListener { signup() }
		signupError = view.findViewById<View>(R.id.signupError) as TextView

		signupTitle?.text = IQChannelsLanguage.iqChannelsLanguage.signupTitle
		signupSubtitle?.text = IQChannelsLanguage.iqChannelsLanguage.signupSubtitle
		signupCheckBox?.text = IQChannelsLanguage.iqChannelsLanguage.signupCheckboxText
		signupTextName?.hint = IQChannelsLanguage.iqChannelsLanguage.signupNamePlaceholder
		signupButton?.text = IQChannelsLanguage.iqChannelsLanguage.signupButtonText

		clReply = view.findViewById<ReplyMessageView?>(R.id.reply).apply {
			applyReplyStyles()
		}
		clFile = view.findViewById<FileMessageView?>(R.id.file).apply {
			applyFileStyles()
		}

		// Chat.
		chatLayout = view.findViewById<View>(R.id.chatLayout) as RelativeLayout
		chatUnavailableLayout = view.findViewById(R.id.chatUnavailableLayout)
		chatUnavailableErrorText = view.findViewById<TextView?>(R.id.tv_description)?.apply {
			text = IQChannelsLanguage.iqChannelsLanguage.textError
			applyIQStyles(IQStyles.iqChannelsStyles?.error?.textError)
		}
		view.findViewById<TextView?>(R.id.tv_title)?.apply {
			text = IQChannelsLanguage.iqChannelsLanguage.titleError
			applyIQStyles(IQStyles.iqChannelsStyles?.error?.titleError)
		}

		IQStyles.iqChannelsStyles?.error?.iconError?.let {
			view.findViewById<ImageView?>(R.id.iv_error)?.apply {
				val glideUrl = GlideUrl(
					it,
					LazyHeaders.Builder()
						.addHeader("Cookie", "client-session=${IQChannels.getCurrentToken()}")
						.build()
				)
				Glide.with(context)
					.load(glideUrl)
					.into(this)
			}
		}

		tnwMsgCopied = view.findViewById(R.id.tnw_msg_copied)
		btnGoBack = view.findViewById(R.id.btn_go_back)
		btnGoBack?.text = IQChannelsLanguage.iqChannelsLanguage.buttonError

		// Messages.
		progress = (view.findViewById<View>(R.id.messagesProgress) as ProgressBar)
		IQStyles.iqChannelsStyles?.chat?.chatLoader?.getColorInt(requireContext())?.let {
			progress?.indeterminateDrawable?.colorFilter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_ATOP)
			(view.findViewById<View>(R.id.authProgress) as ProgressBar).indeterminateDrawable?.colorFilter =
				PorterDuffColorFilter(it, PorterDuff.Mode.SRC_ATOP)
		}

		refresh = view.findViewById<View>(R.id.messagesRefresh) as SwipeRefreshLayout
		refresh?.isEnabled = false
		refresh?.setOnRefreshListener { refreshMessages() }
		IQStyles.iqChannelsStyles?.chat?.chatHistory?.getColorInt(requireContext())?.let {
			refresh?.setColorSchemeColors(it)
		}

		val markwon = Markwon.builder(requireContext())
			.usePlugin(StrikethroughPlugin.create())
			.build()

		adapter = ChatMessagesAdapter(
			IQChannels,
			view,
			{ view.width to view.height },
			markwon,
			ItemClickListener()
		)

		recycler = view.findViewById(R.id.messages)
		recycler?.adapter = adapter
		btnScrollToBottom = view.findViewById(R.id.fl_scroll_down)
		btnScrollToBottomDot = view.findViewById(R.id.iv_scroll_down_dot)

		btnScrollToBottom?.setOnClickListener {
			adapter?.itemCount?.let {
				recycler?.scrollToPosition(it - 1)
			}
			btnScrollToBottomDot?.isVisible = true
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
		clFile?.setCloseBtnClickListener { hideSelectedFile() }

		// Send.
		sendText = view.findViewById<EditText?>(R.id.sendText)?.apply {
			applyIQStyles(IQStyles.iqChannelsStyles?.toolsToMessage?.textInput)

			hint = IQChannelsLanguage.iqChannelsLanguage.inputMessagePlaceholder

			IQStyles.iqChannelsStyles?.toolsToMessage?.backgroundInput
				?.let {
					background = GradientDrawable().apply {
						setColor(it.color?.getColorInt(context) ?: ContextCompat.getColor(context, R.color.default_color))
						setStroke(
							it.border?.size?.toPx?.roundToInt() ?: 0,
							it.border?.color?.getColorInt(context) ?: ContextCompat.getColor(context, R.color.default_color)
						)
						cornerRadius = it.border?.borderRadius?.toPx ?: 12.toPx
					}
				}
		}
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

		attachButton = view.findViewById<ImageButton?>(R.id.attachButton)?.apply {
			IQStyles.iqChannelsStyles?.toolsToMessage?.iconClip?.let {
				val glideUrl = GlideUrl(
					it,
					LazyHeaders.Builder()
						.addHeader("Cookie", "client-session=${IQChannels.getCurrentToken()}")
						.build()
				)
				Glide.with(context)
					.load(glideUrl)
					.into(this)
			}
		}
		attachButton?.setOnClickListener {
			IQChannels.fileСhooser = true
			showAttachChooser()
		}
		sendButton = view.findViewById<ImageButton?>(R.id.sendButton)?.apply {
			IQStyles.iqChannelsStyles?.toolsToMessage?.iconSent?.let {
				val glideUrl = GlideUrl(
					it,
					LazyHeaders.Builder()
						.addHeader("Cookie", "client-session=${IQChannels.getCurrentToken()}")
						.build()
				)
				Glide.with(context)
					.load(glideUrl)
					.into(this)
			}

			IQStyles.iqChannelsStyles?.toolsToMessage?.backgroundIcon?.getColorInt(context)?.let {
				this.setBackgroundColor(it)
			}
		}
		sendButton?.setOnClickListener { sendMessage() }

		if (!requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
			attachButton?.visibility = View.GONE
		}

		arguments?.getString(ARG_TITLE)?.let { title ->
			view.findViewById<ComposeView>(R.id.nav_bar)?.let {

				it.setContent {
					IQChannelsTheme {
						NavBar(title = title) {
							if (checkEvent(IQChatEvent.NavBarBackButtonPressed::class.java)) {
								sendChatEvent(IQChatEvent.NavBarBackButtonPressed)
							} else {
								parentFragmentManager.popBackStack()
							}
						}
					}
				}
			}
		}

		return view
	}

	private fun ReplyMessageView.applyReplyStyles() {
		tvFileName.applyIQStyles(IQStyles.iqChannelsStyles?.answer?.textMessage)
		tvText.applyIQStyles(IQStyles.iqChannelsStyles?.answer?.textMessage)
		tvSenderName.applyIQStyles(IQStyles.iqChannelsStyles?.answer?.textSender)
		setBackgroundColor(
			IQStyles.iqChannelsStyles?.answer?.backgroundTextUpMessage?.getColorInt(context)
				?: ContextCompat.getColor(requireContext(), R.color.white)
		)

		IQStyles.iqChannelsStyles?.answer?.iconCancel?.let {
			val glideUrl = GlideUrl(
				it,
				LazyHeaders.Builder()
//					.addHeader("Cookie", "client-session=${IQChannels.getCurrentToken()}")
					.build()
			)
			Glide.with(context)
				.load(glideUrl)
				.into(ibClose)
		}

		IQStyles.iqChannelsStyles?.answer?.leftLine?.getColorInt(context)?.let {
			setVerticalDividerColorInt(it)
		}
	}

	private fun FileMessageView.applyFileStyles() {
		tvFileName.applyIQStyles(IQStyles.iqChannelsStyles?.answer?.textMessage)
		tvFileSize.applyIQStyles(IQStyles.iqChannelsStyles?.answer?.textMessage)

		setBackgroundColor(
			IQStyles.iqChannelsStyles?.answer?.backgroundTextUpMessage?.getColorInt(context)
				?: ContextCompat.getColor(requireContext(), R.color.white)
		)

		IQStyles.iqChannelsStyles?.answer?.iconCancel?.let {
			val glideUrl = GlideUrl(
				it,
				LazyHeaders.Builder()
					.addHeader("Cookie", "client-session=${IQChannels.getCurrentToken()}")
					.build()
			)
			Glide.with(context)
				.load(glideUrl)
				.into(ibClose)
		}
	}

	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		savedInstanceState?.let {
			lmState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				it.getParcelable(PARAM_LM_STATE, Parcelable::class.java)
			} else {
				it.getParcelable(PARAM_LM_STATE)
			}
		}

		recycler?.addOnScrollListener(object : OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
				val messagesCount = adapter?.itemCount ?: return
				val lastVisibleItemPosition = lm.findLastVisibleItemPosition()
				val isVisible = lastVisibleItemPosition < messagesCount - 5
				btnScrollToBottom?.isVisible = isVisible

				if (!isVisible) {
					btnScrollToBottomDot?.isVisible = false
				}
			}
		})

		btnGoBack?.setOnClickListener {
			if (checkEvent(IQChatEvent.ErrorGoBackButtonPressed::class.java)) {
				sendChatEvent(IQChatEvent.ErrorGoBackButtonPressed)
			} else {
				parentFragmentManager.popBackStack()
			}
		}
	}

	override fun onDestroyView() {
		tnwMsgCopied?.destroy()
		super.onDestroyView()
	}

	override fun onDestroy() {
		super.onDestroy()
	}

	private fun updateViews() {
		val token = IQChannels.getCurrentToken()
		authLayout?.visibility =
			if (IQChannels.auth == null && IQChannels.authRequest != null && token != null) View.VISIBLE else View.GONE

		if(IQChannels.auth == null && IQChannels.authRequest == null && token == null && !IQChannels.authFailed){
			changeStyleButton(signupButton?.isEnabled)

			IQStyles.iqChannelsStyles?.signup?.button?.backgroundDisabled
				?.let {
					signupButton?.setBackgroundDrawable(it, null)
				}
			signupButton?.applyIQStyles(IQStyles.iqChannelsStyles?.signup?.button?.textDisabled)

			signupTextName?.addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
				override fun afterTextChanged(name: Editable) {
					signupButton?.isEnabled = name.isNotEmpty() && signupCheckBox?.isChecked ?: false
					changeStyleButton(signupButton?.isEnabled)
				}
			})

			IQStyles.iqChannelsStyles?.signup?.inputBackground
				?.let {
					signupTextName?.setBackgroundDrawable(it, null)
				}
			signupTextName?.applyIQStyles(IQStyles.iqChannelsStyles?.signup?.inputText)

			signupCheckBox?.setOnCheckedChangeListener { _, isChecked ->
				signupButton?.isEnabled = signupTextName?.text?.isNotEmpty() ?: false && isChecked
				changeStyleButton(signupButton?.isEnabled)
			}
			signupCheckBox?.applyIQStyles(IQStyles.iqChannelsStyles?.signup?.checkBoxText)

			val greetingBold = IQChannels.signupGreetingSettings?.GreetingBold
			val greeting = IQChannels.signupGreetingSettings?.Greeting
			if (!greetingBold.isNullOrBlank()) {signupTitle?.text = greetingBold}
			signupTitle?.applyIQStyles(IQStyles.iqChannelsStyles?.signup?.title)
			if (!greeting.isNullOrBlank()) {signupSubtitle?.text = greeting}
			signupSubtitle?.applyIQStyles(IQStyles.iqChannelsStyles?.signup?.subtitle)

			signupLayout?.visibility = View.VISIBLE
			signupLayout?.setBackgroundColor(
				IQStyles.iqChannelsStyles?.signup?.background?.getColorInt(requireContext())
					?: ContextCompat.getColor(requireContext(), R.color.white)
			)

			signupError?.applyIQStyles(IQStyles.iqChannelsStyles?.signup?.errorText)
		} else if(IQChannels.authFailed){
			showUnavailableView(IQChannelsLanguage.iqChannelsLanguage.textError)
		}
		else{
			signupLayout?.visibility = View.GONE
		}
		signupTextName?.isEnabled = IQChannels.authRequest == null
		chatLayout?.visibility = if (IQChannels.auth != null) View.VISIBLE else View.GONE
		chatUnavailableLayout?.isVisible = false
	}

	private fun showLoading() {
		if (chatUnavailableLayout?.isVisible == false) {
			authLayout?.isVisible = true
			signupLayout?.isVisible = false
			chatLayout?.isVisible = false
			chatUnavailableLayout?.isVisible = false
		}
	}

	private fun showUnavailableView(errorMessage: String) {
		if (chatUnavailableLayout?.isVisible == false) {
			authLayout?.visibility = View.GONE
			signupLayout?.visibility = View.GONE
			chatLayout?.visibility = View.GONE
			chatUnavailableLayout?.isVisible = true
			chatUnavailableErrorText?.text = errorMessage
		}
	}

	override fun onStart() {
		super.onStart()

		iqchannelsListenerCancellable = IQChannels.addListener(object : IQChannelsListener {
			override fun authenticating() {
				signupError?.text = ""
				showLoading()
			}

			override fun authComplete(auth: ClientAuth) {
				signupError?.text = ""
				loadMessages()
				updateViews()
			}

			override fun authFailed(e: Exception, attempt: Int) {
				signupError?.text = String.format("Ошибка: %s", e.localizedMessage)

//				if (attempt >= 5) {
//					showUnavailableView(IQChannelsLanguage.iqChannelsLanguage?.TextError ?: "Мы уже все исправляем. Обновите\nстраницу или попробуйте позже")
//				}
//
//				val message = when (e) {
//					is UnknownHostException -> {
//						IQChannelsLanguage.iqChannelsLanguage?.TextError ?: "Мы уже все исправляем. Обновите\nстраницу или попробуйте позже"
//					}
//
//					is SocketTimeoutException, is TimeoutException -> {
//						getString(R.string.timeout_message)
//					}
//
//					is HttpException -> {
//						getString(R.string.chat_unavailable_description)
//					}
//
//					else -> return
//				}
				showUnavailableView(IQChannelsLanguage.iqChannelsLanguage.textError)

//				showUnavailableView(message)
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

		lmState = (recycler?.layoutManager as? LinearLayoutManager)?.onSaveInstanceState()

		clearMessages()
		clearMoreMessages()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putParcelable(PARAM_LM_STATE, lmState)
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

	private fun maybeScrollToBottomOnNewMessage(force: Boolean = false) {
		if (btnScrollToBottom?.isVisible != true || force) {
			recycler?.let { recycler ->
				val count = adapter?.itemCount ?: 0
				recycler.smoothScrollToPosition(if (count == 0) 0 else count - 1)
			}
		}
	}

	// Signup
	private fun signup() {
		val name = signupTextName?.text?.toString() ?: return
		if (name.length < 3) {
			signupError!!.text = IQChannelsLanguage.iqChannelsLanguage.signupError
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
		updateViews()
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

			override fun eventChangeChannel(channel: String) {
				this@ChatFragment.onChannelChange(channel)
			}

			override fun eventTyping(event: ChatEvent) {
				this@ChatFragment.eventTyping(event)
			}

			override fun ratingRenderQuestion() {
				this@ChatFragment.maybeScrollToBottomOnNewMessage()
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

		val messagesWithUnread: MutableList<ChatMessage>? = checkForUnreadMessages(messages)
		adapter?.loaded(messagesWithUnread ?: messages)

		lmState?.let {
			(recycler?.layoutManager as? LinearLayoutManager)?.onRestoreInstanceState(it)
		} ?: run {
			val scrollPosition = when {
				messagesWithUnread != null && lastUnreadMsgIndex > 0 -> lastUnreadMsgIndex
				messages.isEmpty() -> 0
				else -> messages.size - 1
			}
			recycler?.scrollToPosition(scrollPosition)
		}

		progress?.visibility = View.GONE
		refresh?.isRefreshing = false
		refresh?.isEnabled = true

		viewModel.startSendPrefilled(requireActivity())
	}

	private fun messagesException(e: Exception) {
		if (messagesRequest == null) {
			return
		}

		messagesRequest = null
		progress?.visibility = View.GONE
		refresh?.isRefreshing = false
		refresh?.isEnabled = true

		if (checkEvent(IQChatEvent.MessagesLoadException::class.java)) {
			sendChatEvent(IQChatEvent.MessagesLoadException(e))
		} else {
			showMessagesErrorAlert(e)
		}
	}

	private fun messageReceived(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		checkDisableFreeText(message)
		adapter?.received(message)

		if (!message.My) {
			maybeScrollToBottomOnNewMessage()
		}

		if (btnScrollToBottom?.isVisible == true) {
			btnScrollToBottomDot?.isVisible = true
		}
	}

	private fun messageSent(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		adapter?.sent(message)
		maybeScrollToBottomOnNewMessage(true)
	}

	private fun messageUploaded(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		adapter?.updated(message)
		maybeScrollToBottomOnNewMessage()

		IQLog.d("abctag", "messageUploaded")
		viewModel.sendNextFile(requireActivity())
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
		if (event.Actor == ActorType.CLIENT) {
			return
		}

		val name = if (event.User != null) event.User?.DisplayName else "Оператор"

		val typingText = view?.findViewById<TextView?>(R.id.typing)
		typingText?.text = "$name ${IQChannelsLanguage.iqChannelsLanguage.operatorTyping}..."
		typingText?.applyIQStyles(IQStyles.iqChannelsStyles?.chat?.systemText)

		typingText?.visibility = View.VISIBLE

		Handler(Looper.getMainLooper()).postDelayed(
			{
				typingText?.visibility = View.GONE
			},
			3000
		)
	}

	private fun messageUpdated(message: ChatMessage) {
		if (messagesRequest == null) {
			return
		}

		checkDisableFreeText(message)
		adapter?.updated(message)

		checkException(message)

		viewModel.onMessageUpdated(message, requireActivity())
	}

	private fun checkException(message: ChatMessage) {
		val exception = message.UploadExc ?: return
		val errMessage: String?

		when (exception) {
			is HttpException -> {
				errMessage = if (exception.code == 413) {
					IQChannelsLanguage.iqChannelsLanguage.fileWeightError
				} else {
					IQChannelsLanguage.iqChannelsLanguage.textError
				}
			}

			is UnknownHostException -> {
				errMessage = IQChannelsLanguage.iqChannelsLanguage.textError
			}

			is SocketTimeoutException, is TimeoutException, is java.net.SocketException -> {
				errMessage = IQChannelsLanguage.iqChannelsLanguage.textError
			}

			is ChatException -> {
				errMessage = exception.message ?: "Exception"
			}

			else -> {
				IQLog.d(
					"UploadException",
					"Message load exception. Type: ${exception.javaClass}. Body: ${exception.stackTraceToString()}"
				)
				errMessage = "Upload exception"
			}
		}

		ItemClickListener().fileUploadException(errMessage)
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

		if (checkEvent(IQChatEvent.MessagesLoadMoreException::class.java)) {
			sendChatEvent(IQChatEvent.MessagesLoadMoreException(e))
		} else {
			showMessagesErrorAlert(e)
		}
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
				Manifest.permission.READ_EXTERNAL_STORAGE
			) == PackageManager.PERMISSION_DENIED
		) {
			requestAllPermissions.launch(
				arrayOf(
					Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE
				)
			)

			return
		}

		showAttachChooser(false)
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
				IQLog.e(
					TAG, String.format(
						"showAttachChooser: Failed to create a temp file for the camera, e=%s", e
					)
				)
			}
		}

		// Create a gallery intent.
		val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
		galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)
		galleryIntent.type = "*/*"

		galleryIntent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
			"image/*", "audio/*", "video/*", "text/*", "application/*"
		))

		galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

		// Create and start an intent chooser.
		val chooser = Intent.createChooser(galleryIntent, "")
		if (cameraIntent != null) {
			chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(cameraIntent))
		}

		requestPickImageFromFiles.launch(chooser)
	}

	// Gallery
	private fun onGalleryResult(uri: Uri) {
		selectedFile = viewModel.prepareFile(uri, requireActivity())

		lifecycleScope.launch(Dispatchers.Main) {
			clFile?.showSelectedFile(selectedFile)
			sendButton?.isVisible = true
		}
	}

	private fun onGalleryMutipleFilesResult(uri: Uri) {
		selectedFile = viewModel.prepareFile(uri, requireActivity())

		lifecycleScope.launch(Dispatchers.Main) {
			clFile?.showSelectedFile(selectedFile)
			sendButton?.isVisible = true
		}
	}

	private fun sendMultipleFiles(fileUris: List<Uri>) {
		lifecycleScope.launch {
			viewModel.addMultipleFilesQueue(fileUris.take(10))
			val uri = viewModel.getNextFileFromQueue()

			uri?.let { onGalleryMutipleFilesResult(it) }
		}
	}

	private fun hideReplying() {
		clReply?.visibility = View.GONE
		replyingMessage = null
	}

	private fun hideSelectedFile() {
		clFile?.visibility = View.GONE
		sendButton?.isVisible = !sendText?.text.isNullOrEmpty()
		selectedFile = null
	}

	// Camera
	private fun onCameraResult(resultCode: Int) {
		if (resultCode != Activity.RESULT_OK || cameraTempFile == null) {
			IQLog.i(
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
			IQLog.e(TAG, String.format("showCamera: Failed to save a captured file, error=%s", e))
			return
		}

		addCameraPhotoToGallery(file)
		val text = sendText?.text.toString()
		sendText?.setText("")

		IQChannels.sendFile(file, text, replyingMessage?.Id)
		hideReplying()
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
		if(sendingFile){
			return
		}
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

		if(selectedFile != null) {
			IQChannels.sendFile(selectedFile, text, replyToMessageId)
		} else {
			IQChannels.send(text, replyToMessageId)
		}
		hideSelectedFile()
		hideReplying()
		adapter?.deleteNewMsgHeader()
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
			.setTitle("Error")
			.setNeutralButton("Ok", null)

		if (e != null) {
			builder.setMessage(e.toString())
		} else {
			builder.setMessage("Unknown exception")
		}

		builder.show()
	}


	private fun onChannelChange(channel: String) {
		IQChannels.configureClient(
			IQChannelsConfig(
				address = IQChannelsConfigRepository.config?.address,
				channel = channel
			)
		)
		IQChannels.chatType = ChatType.REGULAR
		IQChannelsConfigRepository.credentials?.let { IQChannels.login(it) }
	}

	private fun sendChatEvent(event: IQChatEvent) {
		setFragmentResult(
			REQUEST_KEY,
			bundleOf(
				RESULT_KEY_EVENT to event
			)
		)
	}

	private fun checkEvent(event: Class<out IQChatEvent>): Boolean {
		return handledEvents?.any {
			it.isAssignableFrom(event)
		} ?: false
	}

	private fun checkForUnreadMessages(messages: List<ChatMessage>): MutableList<ChatMessage>? {
		var messagesMutable: MutableList<ChatMessage>? = null

		messages.find { !it.My && !it.Read && it.Text?.isNotBlank() == true }?.let { firstUnreadMsg ->
			val index = messages.indexOf(firstUnreadMsg)
			if (index == lastUnreadMsgIndex) {
				return@let
			}

			lastUnreadMsgIndex = index
			messagesMutable = messages.toMutableList()
			val newMsgHeader = ChatMessage().apply {
				Read = true
				System = true
				NewMsgHeader = true
				Date = Date()
			}
			messagesMutable?.add(index, newMsgHeader)
		}

		return messagesMutable
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
			if (!message.System) {
				message.Text?.let { text ->
					val clipboard =
						requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
					val clip = ClipData.newPlainText(text, text)
					clipboard.setPrimaryClip(clip)

					val isXiaomi = Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)
					val isBelowTiramisu = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

					if (isBelowTiramisu || isXiaomi) {
						tnwMsgCopied?.show()
					}
				}
			}
		}

		override fun fileUploadException(errorMessage: String?) {
			val backdrop = ErrorPageBackdropDialog.newInstance(errorMessage)
			backdrop.show(childFragmentManager, ErrorPageBackdropDialog.TRANSACTION_TAG)
		}

		override fun onReplyMessageClick(message: ChatMessage) {
			adapter?.getItemPosition(message)?.let {
				if (it >= 0) {
					recycler?.smoothScrollToPosition(it)
				}
			}
		}
	}

	private fun changeStyleButton(enabled: Boolean?) {
		if(enabled == true) {
			IQStyles.iqChannelsStyles?.signup?.button?.backgroundEnabled
				?.let {
					signupButton?.setBackgroundDrawable(it, null)
				}
			signupButton?.applyIQStyles(IQStyles.iqChannelsStyles?.signup?.button?.textEnabled)
		}else{
			IQStyles.iqChannelsStyles?.signup?.button?.backgroundDisabled
				?.let {
					signupButton?.setBackgroundDrawable(it, null)
				}
			signupButton?.applyIQStyles(IQStyles.iqChannelsStyles?.signup?.button?.textDisabled)
		}
	}
}