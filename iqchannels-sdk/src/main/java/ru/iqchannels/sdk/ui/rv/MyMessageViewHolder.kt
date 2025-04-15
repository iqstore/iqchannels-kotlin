package ru.iqchannels.sdk.ui.rv

import android.content.res.Resources
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.DecimalFormat
import kotlin.math.roundToInt
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.applyIQStyles
import ru.iqchannels.sdk.databinding.ItemMyMessageBinding
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ChatPayloadType
import ru.iqchannels.sdk.schema.FileValidState
import ru.iqchannels.sdk.setBackgroundDrawable
import ru.iqchannels.sdk.styling.IQStyles
import ru.iqchannels.sdk.styling.Text
import ru.iqchannels.sdk.ui.ChatMessagesAdapter
import ru.iqchannels.sdk.ui.Colors
import ru.iqchannels.sdk.ui.widgets.toPx
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.http.HttpCallback
import ru.iqchannels.sdk.room.toDatabaseMessage
import ru.iqchannels.sdk.schema.ChatMessageForm


internal class MyMessageViewHolder(
	private val binding: ItemMyMessageBinding,
	private val itemClickListener: ChatMessagesAdapter.ItemClickListener
) : ViewHolder(binding.root) {

	private val dateFormat: DateFormat =
		android.text.format.DateFormat.getDateFormat(binding.root.context)
	private val timeFormat: DateFormat =
		android.text.format.DateFormat.getTimeFormat(binding.root.context)

	fun bind(message: ChatMessage, rootViewDimens: Pair<Int, Int>) = with(binding) {
		my.visibility = View.VISIBLE

		val adapter = bindingAdapter as? ChatMessagesAdapter ?: return@with

		// Day
		if (adapter.isNewDay(bindingAdapterPosition) && message.Payload !== ChatPayloadType.TYPING) {
			date.text = message.Date?.let { dateFormat.format(it) }
			date.visibility = View.VISIBLE
			date.applyIQStyles(IQStyles.iqChannelsStyles?.chat?.dateText)
		} else {
			date.visibility = View.GONE
		}

		myImageSrc.setOnClickListener { adapter.onImageClicked(bindingAdapterPosition) }
		tvMyFileName.setOnClickListener {
			adapter.onTextMessageClicked(
				bindingAdapterPosition
			)
		}

		myUpload.setOnClickListener { adapter.onUploadCancelClicked(bindingAdapterPosition) }

		// Time
		if (message.Sending) {
			mySending.visibility = View.VISIBLE
			myDate.visibility = View.INVISIBLE
			myReceived.visibility = View.GONE
			myRead.visibility = View.GONE
			myDate.text = message.Date?.let { timeFormat.format(it) } ?: ""
		} else {
			mySending.visibility = View.GONE
			myDate.text = message.Date?.let { timeFormat.format(it) } ?: ""
			myDate.visibility = if (message.Date != null) View.VISIBLE else View.GONE
			myReceived.visibility = if (message.Id > 0 && !message.Read) View.VISIBLE else View.GONE
			myRead.visibility = if (message.Read) View.VISIBLE else View.GONE
		}

		// Error icon
		if (message.Error) {
			errorIcon.visibility = View.VISIBLE
		} else {
			errorIcon.visibility = View.GONE
		}

		run {
			IQStyles.iqChannelsStyles?.messages?.backgroundClient
				?.let {
					myMsgContainer.setBackgroundDrawable(it, R.drawable.my_msg_bg)
				}

			myDate.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.textTimeClient)

			myReply.tvSenderName.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.replySenderTextClient)
			myReply.tvText.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.replyTextClient)
			myReply.tvFileName.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.replyTextClient)

			tvMyFileSize.applyIQStyles(IQStyles.iqChannelsStyles?.messageFile?.textFileSizeClient)
			tvMyFileName.applyIQStyles(IQStyles.iqChannelsStyles?.messageFile?.textFilenameClient)

			IQStyles.iqChannelsStyles?.messageFile?.iconFileClient?.let {
				Glide.with(root.context)
					.load(it)
					.into(ivFile)
			}

			errorIcon.setOnClickListener { view ->
				showPopupMenu(view, message)
			}
		}

		// Reset the visibility.
		run {
			clTextsMy.visibility = View.GONE
			tvMyFileName.visibility = View.GONE
			tvMyFileSize.visibility = View.GONE
			ivFile.isVisible = false
		}

		// Message
		if (message.Upload != null) {
			myText.visibility = View.VISIBLE
			myImageFrame.visibility = View.GONE
			clTextsMy.visibility = View.VISIBLE
			myUpload.visibility = View.VISIBLE
			mySending.isVisible = false

			if (message.UploadExc != null) {
				myUpload.visibility = View.GONE
			}

			val file = message.Upload ?: return@with
			myImageFrame.visibility = View.GONE
			clTextsMy.visibility = View.VISIBLE
			tvMyFileName.visibility = View.VISIBLE
			tvMyFileName.text = file.name
			val size = file.length()

			showFileSize(size)

			myText.text = message.Text
		} else if (message.File != null) {
			when (message.File?.State) {
				FileValidState.Rejected -> showFileStateMsg(
					R.string.unsecure_file,
					R.color.red,
					IQStyles.iqChannelsStyles?.messages?.textFileStateRejectedClient
				)

				FileValidState.OnChecking -> {
					myText.visibility = View.VISIBLE
					myImageFrame . visibility = View . GONE
					clTextsMy.visibility = View.VISIBLE
					myUpload . visibility = View . VISIBLE
					mySending.isVisible = false

					if (message.UploadExc != null) {
						myUpload.visibility = View.GONE
					}

					val file = message.File ?: return@with

					myImageFrame.visibility = View.GONE
					clTextsMy . visibility = View . VISIBLE
					tvMyFileName.visibility = View.VISIBLE
					tvMyFileName . text = file . Name
					val size = file.Size

					showFileSize (size)

					myText . text = message . Text
				}



				FileValidState.SentForChecking -> showFileStateMsg(
					R.string.file_sent_to_check,
					R.color.blue,
					IQStyles.iqChannelsStyles?.messages?.textFileStateSentForCheckingClient
				)

				FileValidState.CheckError -> showFileStateMsg(
					R.string.error_on_checking,
					R.color.red,
					IQStyles.iqChannelsStyles?.messages?.textFileStateCheckErrorClient
				)

				else -> showApprovedFile(message, rootViewDimens)
			}
		} else {
			myImageFrame.visibility = View.GONE
			myUpload.visibility = View.GONE
			clTextsMy.visibility = View.VISIBLE
			myText.visibility = View.VISIBLE
			myText.autoLinkMask = Linkify.ALL
			myText.text = message.Text
			myText.setTextColor(ContextCompat.getColor(root.context, R.color.my_text_color))
			myText.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.textClient)
			myText.minWidth = 0
			myText.maxWidth = Int.MAX_VALUE
		}

		// Reply message (attached message)
		myReply.visibility = View.GONE
		val replyToMessageId = message.ReplyToMessageId
		if (replyToMessageId != null && replyToMessageId > 0) {
			val replyMsg = (bindingAdapter as? ChatMessagesAdapter)?.findMessage(message)
			if (replyMsg != null) {
				myReply.showReplyingMessage(replyMsg)
				myReply.setCloseBtnVisibility(View.GONE)
				myReply.setVerticalDividerColor(R.color.white)
				myReply.setTvSenderNameColor(R.color.white)
				myReply.setTvTextColor(R.color.white_transparent_54)
				val lp = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				)
				lp.gravity = Gravity.END
				myReply.layoutParams = lp

				myReply.post {
					if (myReply.width > myText.width) {
						myText.width = myReply.width
					} else {
						val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
							myText.width,
							LinearLayout.LayoutParams.WRAP_CONTENT
						)
						lp.gravity = Gravity.END
						myReply.layoutParams = layoutParams
					}
				}

				myReply.setOnClickListener {
					itemClickListener.onReplyMessageClick(replyMsg)
				}
			}
		}

		binding.root.setOnLongClickListener {
			itemClickListener.onMessageLongClick(message)
			true
		}
	}

	private fun showPopupMenu(view: View, message: ChatMessage) {
		val inflater = LayoutInflater.from(view.context)
		val popupView = inflater.inflate(R.layout.popup_menu, null)

		val popupWindow = PopupWindow(
			popupView,
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT,
			true
		)
		popupWindow.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
		popupWindow.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING

		val retryButton: Button = popupView.findViewById(R.id.retryButton)
		val deleteButton: Button = popupView.findViewById(R.id.deleteButton)

		retryButton.setOnClickListener {
			val form = ChatMessageForm.text(message.LocalId, message.Text, message.ReplyToMessageId)
			IQChannels.resend( form, 0, true)

			popupWindow.dismiss()
		}

		deleteButton.setOnClickListener {
			IQChannels.messageDelete(message)
			popupWindow.dismiss()
		}

		popupView.startAnimation(AnimationUtils.loadAnimation(view.context, R.anim.popup_enter))

		val location = IntArray(2)
		view.getLocationOnScreen(location)

		val screenWidth = Resources.getSystem().displayMetrics.widthPixels

		popupView.measure(
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
		)
		val menuWidth = popupView.measuredWidth
		val menuHeight = popupView.measuredHeight

		val buttonY = location[1]
		val spaceAbove = buttonY - 40

		val offsetX = screenWidth - menuWidth - 10

		if (spaceAbove >= menuHeight) {
			popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, offsetX, buttonY - menuHeight)
		} else {
			popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, offsetX, buttonY + view.height)
		}
	}

	private fun ItemMyMessageBinding.showFileSize(size: Long) {
		if (size != null && size > 0) {
			tvMyFileSize.visibility = View.VISIBLE
			val sizeKb = size.toFloat() / 1024
			var sizeMb = 0f
			if (sizeKb > 1024) {
				sizeMb = sizeKb / 1024
			}
			val strRes: Int
			val fileSize: String
			if (sizeMb > 0) {
				strRes = R.string.file_size_mb_placeholder
				val df = DecimalFormat("0.00")
				fileSize = df.format(sizeMb.toDouble())
			} else {
				strRes = R.string.file_size_kb_placeholder
				fileSize = String.format("%.2f", sizeKb)
			}

			tvMyFileSize.text = root.resources.getString(
				strRes,
				fileSize
			)
		} else {
			tvMyFileSize.text = null
		}
	}

	private fun showApprovedFile(
		message: ChatMessage,
		rootViewDimens: Pair<Int, Int>
	) {
		binding.apply {
			myUpload.visibility = View.GONE
			val file = message.File
			val imageUrl = file?.ImagePreviewUrl
			if (imageUrl != null) {
				val size = Utils.computeImageSizeFromFile(file, rootViewDimens)

				if (!message.Text.isNullOrEmpty()) {
					clTextsMy.visibility = View.VISIBLE
					myText.visibility = View.VISIBLE
					myText.text = message.Text
					myFlags.isVisible = true

					myImgFlags.isVisible = false
					myDate.text = message.Date?.let { timeFormat.format(it) } ?: ""
					myDate.visibility = if (message.Date != null) View.VISIBLE else View.GONE
					myReceived.visibility = if (message.Received) View.VISIBLE else View.GONE
					myRead.visibility = if (message.Read) View.VISIBLE else View.GONE
					val isRead = message.Read
					myRead.isVisible = isRead
					myReceived.isVisible = !isRead && message.Received == true
				} else {
					clTextsMy.visibility = View.GONE
					myText.visibility = View.GONE
					myFlags.isVisible = false

					myImgFlags.isVisible = true
					myImgDate.text = message.Date?.let { timeFormat.format(it) } ?: ""
					myImgDate.visibility = if (message.Date != null) View.VISIBLE else View.GONE
					myImgReceived.visibility = if (message.Received) View.VISIBLE else View.GONE
					myImgRead.visibility = if (message.Read) View.VISIBLE else View.GONE
					val isRead = message.Read
					myImgRead.isVisible = isRead
					myImgReceived.isVisible = !isRead && message.Received == true
				}

				myImageFrame.visibility = View.VISIBLE
				myImageFrame.layoutParams.width = size[0]
				myImageFrame.layoutParams.height = size[1]
				myImageFrame.requestLayout()

				myImageFrame.post {
					Glide.with(root.context)
						.load(imageUrl)
						.transform(
							CenterCrop(),
							RoundedCorners(11.toPx.roundToInt())
						)
						.into(myImageSrc)
				}
			} else {
				if (!message.Text.isNullOrEmpty()) {
					myText.visibility = View.VISIBLE
					myText.text = message.Text
				} else {
					myText.visibility = View.GONE
				}
				myFlags.isVisible = true
				myImageFrame.visibility = View.GONE
				clTextsMy.visibility = View.VISIBLE
				tvMyFileName.visibility = View.VISIBLE
				tvMyFileName.text = file?.Name
				ivFile.isVisible = true
				val size = file?.Size

				size?.let { showFileSize(it) }
			}
		}
	}

	private fun ItemMyMessageBinding.showFileStateMsg(
		@StringRes strRes: Int,
		@ColorRes colorRes: Int,
		text: Text?
	) {
		myImageFrame.visibility = View.GONE
		myUpload.visibility = View.GONE
		clTextsMy.visibility = View.VISIBLE
		myText.visibility = View.VISIBLE
		myText.text = root.context.getString(strRes)
		myText.setTextColor(
			ContextCompat.getColor(root.context, colorRes)
		)
		myText.applyIQStyles(text)
	}
}