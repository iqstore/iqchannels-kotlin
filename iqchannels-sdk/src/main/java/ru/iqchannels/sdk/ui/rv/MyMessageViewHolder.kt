package ru.iqchannels.sdk.ui.rv

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import java.text.DateFormat
import java.text.DecimalFormat
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.databinding.ItemMyMessageBinding
import ru.iqchannels.sdk.http.HttpException
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ChatPayloadType
import ru.iqchannels.sdk.ui.ChatMessagesAdapter
import ru.iqchannels.sdk.ui.Colors
import ru.iqchannels.sdk.ui.UiUtils

internal class MyMessageViewHolder(
	private val binding: ItemMyMessageBinding,
	private val itemClickListener: ChatMessagesAdapter.ItemClickListener
) : ViewHolder(binding.root) {

	private val dateFormat: DateFormat = android.text.format.DateFormat.getDateFormat(binding.root.context)
	private val timeFormat: DateFormat = android.text.format.DateFormat.getTimeFormat(binding.root.context)

	fun bind(message: ChatMessage, rootViewDimens: Pair<Int, Int>) = with(binding) {
		my.visibility = View.VISIBLE

		val adapter = bindingAdapter as? ChatMessagesAdapter ?: return@with

		// Day
		if (adapter.isNewDay(bindingAdapterPosition) && message.Payload !== ChatPayloadType.TYPING) {
			date.text = message.Date?.let { dateFormat.format(it) }
			date.visibility = View.VISIBLE
		} else {
			date.visibility = View.GONE
		}

		myImageSrc.setOnClickListener { adapter.onImageClicked(bindingAdapterPosition) }
		tvMyFileName.setOnClickListener {
			adapter.onTextMessageClicked(
				bindingAdapterPosition
			)
		}

		myUploadCancel.setOnClickListener { adapter.onUploadCancelClicked(adapterPosition) }
		myUploadRetry.setOnClickListener { adapter.onUploadRetryClicked(adapterPosition) }

		// Time
		if (adapter.isGroupEnd(bindingAdapterPosition)) {
			if (message.Sending) {
				mySending.visibility = View.VISIBLE
				myDate.visibility = View.INVISIBLE
				myReceived.visibility = View.GONE
				myRead.visibility = View.GONE
			} else {
				mySending.visibility = View.GONE
				myFlags.visibility = View.VISIBLE
				myDate.text = message.Date?.let { timeFormat.format(it) } ?: ""
				myDate.visibility = if (message.Date != null) View.VISIBLE else View.GONE
				myReceived.visibility = if (message.Received) View.VISIBLE else View.GONE
				myRead.visibility = if (message.Read) View.VISIBLE else View.GONE
			}
		} else {
			mySending.visibility = View.GONE
			myFlags.visibility = View.GONE
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
			myText.visibility = View.GONE
			myImageFrame.visibility = View.GONE
			myUpload.visibility = View.VISIBLE
			myUploadProgress.max = 100
			myUploadProgress.progress = message.UploadProgress
			if (message.UploadExc != null) {
				myUploadProgress.visibility = View.GONE
				myUploadError.visibility = View.VISIBLE
				val exception = message.UploadExc
				var errMessage = exception!!.localizedMessage

				if (exception is HttpException && exception.code == 413) {
					errMessage = root.resources.getString(R.string.file_size_too_large)
				}

				myUploadError.text = errMessage
				myUploadCancel.visibility = View.VISIBLE
				myUploadRetry.visibility = View.VISIBLE
			} else {
				myUploadError.visibility = View.GONE
				myUploadRetry.visibility = View.GONE
				myUploadProgress.visibility = View.VISIBLE
				myUploadCancel.visibility = View.VISIBLE
			}
		} else if (message.File != null) {
			myUpload.visibility = View.GONE
			val file = message.File
			val imageUrl = file?.ImagePreviewUrl
			if (imageUrl != null) {
				val size = Utils.computeImageSizeFromFile(file, rootViewDimens)

				if (message.Text != null && message.Text?.isNotEmpty() == true) {
					clTextsMy.visibility = View.VISIBLE
					myText.visibility = View.VISIBLE
					myText.text = message.Text
				} else {
					myText.visibility = View.GONE
				}

				myImageFrame.visibility = View.VISIBLE
				myImageFrame.layoutParams.width = size[0]
				myImageFrame.layoutParams.height = size[1]
				myImageFrame.requestLayout()

				Glide.with(root.context)
					.load(imageUrl)
					.transform(
						GranularRoundedCorners(
							UiUtils.toPx(12).toFloat(),
							UiUtils.toPx(12).toFloat(),
							0f,
							0f
						)
					)
					.into(myImageSrc)
			} else {
				myImageFrame.visibility = View.GONE
				clTextsMy.visibility = View.VISIBLE
				myText.visibility = View.GONE
				tvMyFileName.visibility = View.VISIBLE
				tvMyFileName.autoLinkMask = 0
				tvMyFileName.movementMethod = LinkMovementMethod.getInstance()
				tvMyFileName.text = file?.Name
				ivFile.isVisible = true
				val size = file?.Size

				if (size != null && size > 0) {
					tvMyFileSize.visibility = View.VISIBLE
					val sizeKb = (file.Size / 1024).toFloat()
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
						fileSize = sizeKb.toString()
					}

					tvMyFileSize.text = root.resources.getString(
						strRes,
						fileSize
					)
				} else {
					tvMyFileSize.text = null
				}

				myText.text = file?.Name
				myText.setTextColor(Colors.linkColor())
			}
		} else {
			myImageFrame.visibility = View.GONE
			myUpload.visibility = View.GONE
			clTextsMy.visibility = View.VISIBLE
			myText.visibility = View.VISIBLE
			myText.autoLinkMask = Linkify.ALL
			myText.text = message.Text
			myText.setTextColor(ContextCompat.getColor(root.context, R.color.my_text_color))
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
				myReply.setVerticalDividerColor(R.color.my_msg_reply_text)
				myReply.setTvSenderNameColor(R.color.my_msg_reply_text)
				myReply.setTvTextColor(R.color.my_msg_reply_text)
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
			}
		}

		binding.root.setOnLongClickListener {
			itemClickListener.onMessageLongClick(message)
			true
		}
	}
}