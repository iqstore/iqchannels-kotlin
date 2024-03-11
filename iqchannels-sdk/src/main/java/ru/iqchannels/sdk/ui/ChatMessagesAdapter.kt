/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.http.HttpCallback
import ru.iqchannels.sdk.http.HttpException
import ru.iqchannels.sdk.schema.Action
import ru.iqchannels.sdk.schema.ActorType
import ru.iqchannels.sdk.schema.ChatEvent
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ChatPayloadType
import ru.iqchannels.sdk.schema.RatingState
import ru.iqchannels.sdk.schema.SingleChoice
import ru.iqchannels.sdk.schema.UploadedFile
import ru.iqchannels.sdk.ui.Colors.linkColor
import ru.iqchannels.sdk.ui.Colors.paletteColor
import ru.iqchannels.sdk.ui.Colors.textColor
import ru.iqchannels.sdk.ui.UiUtils.toPx
import ru.iqchannels.sdk.ui.rv.MarginItemDecoration
import ru.iqchannels.sdk.ui.widgets.DropDownButton
import ru.iqchannels.sdk.ui.widgets.ReplyMessageView

internal class ChatMessagesAdapter(
	private val iqchannels: IQChannels,
	private val rootView: View,
	private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder>() {

	companion object {
		private val GROUP_TIME_DELTA_MS = 60000

		private fun objectEquals(a: Any?, b: Any?): Boolean {
			return (a === b) || (a != null && (a == b))
		}
	}

	private val dateFormat: DateFormat = android.text.format.DateFormat.getDateFormat(rootView.context)
	private val timeFormat: DateFormat = android.text.format.DateFormat.getTimeFormat(rootView.context)
	private val messages: MutableList<ChatMessage> = ArrayList()
	private var agentTyping = false

	fun clear() {
		messages.clear()
		agentTyping = false
		notifyDataSetChanged()
	}

	fun loaded(messages: List<ChatMessage>) {
		this.messages.clear()
		this.messages.addAll(messages)
		notifyDataSetChanged()
	}

	fun loadedMore(moreMessages: List<ChatMessage>) {
		messages.addAll(0, moreMessages)
		notifyItemRangeInserted(0, moreMessages.size)
	}

	fun received(message: ChatMessage) {
		messages.add(message)
		if (messages.size > 1) {
			notifyItemChanged(messages.size - 2)
		}
		notifyItemInserted(messages.size - 1)
	}

	fun sent(message: ChatMessage) {
		messages.add(message)
		if (messages.size > 1) {
			notifyItemChanged(messages.size - 2)
		}
		notifyItemInserted(messages.size - 1)
	}

	fun cancelled(message: ChatMessage) {
		val i = messages.indexOf(message)
		if (i < 0) {
			return
		}
		messages.removeAt(i)
		notifyItemRemoved(i)
	}

	fun deleted(messageToDelete: ChatMessage) {
		var oldMessage: ChatMessage? = null
		for (message: ChatMessage in messages) {
			if (message.Id == messageToDelete.Id) {
				oldMessage = message
			}
		}
		if (oldMessage != null) {
			val i = messages.indexOf(oldMessage)
			if (i < 0) {
				return
			}
			messages.removeAt(i)
			notifyItemRemoved(i)
		}
	}

	fun typing(event: ChatEvent) {
		if (agentTyping) {
			return
		}

		if (event.Actor == ActorType.CLIENT) {
			return
		}

		agentTyping = true
		val msg = ChatMessage()
		msg.Author = ActorType.USER
		val name = if (event.User != null) event.User?.DisplayName else null
		msg.Text = "$name печатает..."
		msg.Payload = ChatPayloadType.TYPING
		msg.Date = Date()
		messages.add(msg)

		if (messages.size > 1) {
			notifyItemChanged(messages.size - 2)
		}

		notifyItemInserted(messages.size - 1)
		Handler(Looper.getMainLooper()).postDelayed(
			{
				agentTyping = false
				val i: Int = messages.indexOf(msg)
				messages.remove(msg)
				notifyItemRemoved(i)
			},
			3000
		)
	}

	fun updated(message: ChatMessage) {
		val i = getIndexByMessage(message)
		if (i < 0) {
			return
		}
		messages[i] = message
		notifyItemChanged(i)
	}

	fun getItem(position: Int): ChatMessage {
		return messages[position]
	}

	private fun getIndexByMessage(message: ChatMessage): Int {
		if (message.My) {
			for (i in messages.indices) {
				val m = messages[i]
				if (m.My && m.LocalId == message.LocalId) {
					return i
				}
			}
		}

		for (i in messages.indices) {
			val m = messages[i]
			if (m.Id == message.Id) {
				return i
			}
		}

		return -1
	}

	override fun getItemCount(): Int {
		return messages.size
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val context = parent.context
		val inflater = LayoutInflater.from(context)
		val contactView = inflater.inflate(R.layout.chat_message, parent, false)

		return ViewHolder(this, contactView)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val message = messages[position]
		// Day
		if (isNewDay(position) && message.Payload !== ChatPayloadType.TYPING) {
			holder.date.text = dateFormat.format(message.Date)
			holder.date.visibility = View.VISIBLE
		} else {
			holder.date.visibility = View.GONE
		}

		// Message
		if (message.My) {
			onBindMyMessage(holder, position, message)
		} else {
			onBindOtherMessage(holder, position, message)
		}
		iqchannels.markAsRead(message)
	}

	private fun onBindMyMessage(holder: ViewHolder, position: Int, message: ChatMessage) {
		val groupEnd = isGroupEnd(position)
		holder.my.visibility = View.VISIBLE
		holder.other.visibility = View.GONE
		holder.clDropdownBtns.visibility = View.GONE

		// Time
		if (groupEnd) {
			if (message.Sending) {
				holder.mySending.visibility = View.VISIBLE
				holder.myDate.visibility = View.INVISIBLE
				holder.myReceived.visibility = View.GONE
				holder.myRead.visibility = View.GONE
			} else {
				holder.mySending.visibility = View.GONE
				holder.myFlags.visibility = View.VISIBLE
				holder.myDate.text =
					if (message.Date != null) timeFormat.format(message.Date) else ""
				holder.myDate.visibility = if (message.Date != null) View.VISIBLE else View.GONE
				holder.myReceived.visibility = if (message.Received) View.VISIBLE else View.GONE
				holder.myRead.visibility =
					if (message.Read) View.VISIBLE else View.GONE
			}
		} else {
			holder.mySending.visibility = View.GONE
			holder.myFlags.visibility = View.GONE
		}

		// Reset the visibility.
		run {
			holder.clTextsMy.visibility = View.GONE
			holder.tvMyFileName.visibility = View.GONE
			holder.tvMyFileSize.visibility = View.GONE
		}

		// Message
		if (message.Upload != null) {
			holder.myText.visibility = View.GONE
			holder.myImageFrame.visibility = View.GONE
			holder.myUpload.visibility = View.VISIBLE
			holder.myUploadProgress.max = 100
			holder.myUploadProgress.progress = message.UploadProgress
			if (message.UploadExc != null) {
				holder.myUploadProgress.visibility = View.GONE
				holder.myUploadError.visibility = View.VISIBLE
				val exception = message.UploadExc
				var errMessage = exception!!.localizedMessage

				if (exception is HttpException && exception.code == 413) {
					errMessage = rootView.resources.getString(R.string.file_size_too_large)
				}

				holder.myUploadError.text = errMessage
				holder.myUploadCancel.visibility = View.VISIBLE
				holder.myUploadRetry.visibility = View.VISIBLE
			} else {
				holder.myUploadError.visibility = View.GONE
				holder.myUploadRetry.visibility = View.GONE
				holder.myUploadProgress.visibility = View.VISIBLE
				holder.myUploadCancel.visibility = View.VISIBLE
			}
		} else if (message.File != null) {
			holder.myUpload.visibility = View.GONE
			val file = message.File
			val imageUrl = file?.ImagePreviewUrl
			if (imageUrl != null) {
				val size = computeImageSizeFromFile(file)

				if (message.Text != null && message.Text?.isNotEmpty() == true) {
					holder.clTextsMy.visibility = View.VISIBLE
					holder.myText.visibility = View.VISIBLE
					holder.myText.text = message.Text
				} else {
					holder.myText.visibility = View.GONE
				}

				holder.myImageFrame.visibility = View.VISIBLE
				holder.myImageFrame.layoutParams.width = size[0]
				holder.myImageFrame.layoutParams.height = size[1]
				holder.myImageFrame.requestLayout()

				Glide.with(holder.myImageFrame.context)
					.load(imageUrl)
					.transform(
						GranularRoundedCorners(
							toPx(12).toFloat(),
							toPx(12).toFloat(),
							0f,
							0f
						)
					)
					.into(holder.myImageSrc)
			} else {
				holder.myImageFrame.visibility = View.GONE
				holder.clTextsMy.visibility = View.VISIBLE
				holder.myText.visibility = View.GONE
				holder.tvMyFileName.visibility = View.VISIBLE
				holder.tvMyFileName.autoLinkMask = 0
				holder.tvMyFileName.movementMethod = LinkMovementMethod.getInstance()
				holder.tvMyFileName.text = file?.Name
				val size = file?.Size

				if (size != null && size > 0) {
					holder.tvMyFileSize.visibility = View.VISIBLE
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
					holder.tvMyFileSize.text = rootView.resources.getString(
						strRes,
						fileSize
					)
				} else {
					holder.tvMyFileSize.text = null
				}

				holder.myText.text = file?.Name
				holder.myText.setTextColor(linkColor())
				//                holder.myText.setText(makeFileLink(file));
			}
		} else {
			holder.myImageFrame.visibility = View.GONE
			holder.myUpload.visibility = View.GONE
			holder.clTextsMy.visibility = View.VISIBLE
			holder.myText.visibility = View.VISIBLE
			holder.myText.setBackgroundResource(R.drawable.my_msg_bg)
			holder.myText.autoLinkMask = Linkify.ALL
			holder.myText.text = message.Text
			holder.myText.setTextColor(textColor())
			holder.myText.minWidth = 0
			holder.myText.maxWidth = Int.MAX_VALUE
		}

		// Reply message (attached message)
		holder.myReply.visibility = View.GONE
		val replyToMessageId = message.ReplyToMessageId
		if (replyToMessageId != null && replyToMessageId > 0) {
			val replyMsg = findMessage(message)
			if (replyMsg != null) {
				holder.myReply.showReplyingMessage(replyMsg)
				holder.myReply.setCloseBtnVisibility(View.GONE)
				holder.myReply.setVerticalDividerColor(R.color.my_msg_reply_text)
				holder.myReply.setTvSenderNameColor(R.color.my_msg_reply_text)
				holder.myReply.setTvTextColor(R.color.my_msg_reply_text)
				val lp = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				)
				lp.gravity = Gravity.END
				holder.myReply.layoutParams = lp
				holder.myText.setBackgroundResource(R.drawable.my_msg_reply_text_bg)

				holder.myReply.post {
					if (holder.myReply.width > holder.myText.width) {
						holder.myText.width = holder.myReply.width
					} else {
						val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
							holder.myText.width,
							LinearLayout.LayoutParams.WRAP_CONTENT
						)
						lp.gravity = Gravity.END
						holder.myReply.layoutParams = layoutParams
					}
				}
			}
		}
	}

	private fun onBindOtherMessage(holder: ViewHolder, position: Int, message: ChatMessage) {
		val groupStart = isGroupStart(position)
		val groupEnd = isGroupEnd(position)
		holder.my.visibility = View.GONE
		holder.other.visibility = View.VISIBLE

		// Name and avatar
		val user = message.User
		if (groupStart && user != null) {
			val name = user.DisplayName
			val letter = if (name?.isEmpty() == true) "" else name?.substring(0, 1)
			holder.otherName.text = name
			holder.otherName.visibility = View.VISIBLE
			holder.otherAvatar.visibility = View.VISIBLE
			val avatarUrl = user.AvatarUrl
			if (!avatarUrl.isNullOrEmpty()) {
				// Avatar image
				holder.otherAvatarText.visibility = View.GONE
				holder.otherAvatarImage.visibility = View.VISIBLE
				iqchannels.picasso()
					?.load(avatarUrl)
					?.placeholder(R.drawable.avatar_placeholder)
					?.into(holder.otherAvatarImage)
			} else {
				// Avatar circle with a letter inside
				holder.otherAvatarImage.visibility = View.GONE
				holder.otherAvatarText.visibility = View.VISIBLE
				holder.otherAvatarText.text = letter
				holder.otherAvatarText.setBackgroundColor(paletteColor(letter))
			}
		} else {
			holder.otherName.visibility = View.GONE
			holder.otherAvatar.visibility = View.INVISIBLE
		}

		// Time
		if (groupEnd && message.Date != null) {
			holder.otherDate.text = timeFormat.format(message.Date)
			holder.otherDate.visibility = View.VISIBLE
		} else {
			holder.otherDate.visibility = View.GONE
		}

		// Message

		// Reset the visibility.
		run {
			holder.clTexts.visibility = View.GONE
			holder.otherText.visibility = View.GONE
			holder.tvOtherFileName.visibility = View.GONE
			holder.tvOtherFileSize.visibility = View.GONE
			holder.otherImageFrame.visibility = View.GONE
			holder.otherRating.visibility = View.GONE
			holder.otherReply.visibility = View.GONE
			holder.rvButtons.visibility = View.GONE
			holder.clDropdownBtns.visibility = View.GONE
			holder.rvCardBtns.visibility = View.GONE
		}

		val file = message.File
		val rating = message.Rating
		if (file != null) {
			val imageUrl = file.ImagePreviewUrl
			if (imageUrl != null) {
				val size = computeImageSizeFromFile(file)
				val text = message.Text
				if (!text.isNullOrEmpty()) {
					holder.clTexts.visibility = View.VISIBLE
					holder.clTexts.setBackgroundResource(R.drawable.other_msg_reply_text_bg)
					holder.otherText.visibility = View.VISIBLE
					holder.otherText.text = message.Text
				} else {
					holder.otherText.visibility = View.GONE
				}
				holder.otherImageFrame.visibility = View.VISIBLE

				if (!text.isNullOrEmpty()) {
					holder.otherImageFrame.setBackgroundResource(R.drawable.other_msg_reply_bg)
				} else {
					holder.otherImageFrame.setBackgroundResource(R.drawable.other_msg_bg)
				}

				holder.otherImageFrame.layoutParams.width = size[0]
				holder.otherImageFrame.layoutParams.height = size[1]
				holder.otherImageFrame.requestLayout()
				Glide.with(holder.otherImageFrame.context)
					.load(imageUrl)
					.transform(
						GranularRoundedCorners(
							toPx(12).toFloat(),
							toPx(12).toFloat(),
							0f,
							0f
						)
					)
					.into(holder.otherImageSrc)
				holder.otherImageSrc.post {
					val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
						holder.otherImageFrame.width,
						LinearLayout.LayoutParams.WRAP_CONTENT
					)
					layoutParams.setMargins(0, 0, toPx(40), 0)
					holder.clTexts.layoutParams = layoutParams
				}
			} else {
				holder.otherImageFrame.visibility = View.GONE
				holder.clTexts.visibility = View.VISIBLE
				holder.clTexts.setBackgroundResource(R.drawable.other_msg_bg)
				holder.tvOtherFileName.visibility = View.VISIBLE
				holder.tvOtherFileName.autoLinkMask = 0
				holder.tvOtherFileName.movementMethod = LinkMovementMethod.getInstance()
				holder.tvOtherFileName.text = file.Name
				if (file.Size > 0) {
					holder.tvOtherFileSize.visibility = View.VISIBLE
					val sizeKb = (file.Size / 1024).toFloat()
					var sizeMb = 0f
					if (sizeKb > 1024) {
						sizeMb = sizeKb / 1024
					}
					var strRes = 0
					val fileSize: String
					if (sizeMb > 0) {
						strRes = R.string.file_size_mb_placeholder
						val df = DecimalFormat("0.00")
						fileSize = df.format(sizeMb.toDouble())
					} else {
						strRes = R.string.file_size_kb_placeholder
						fileSize = sizeKb.toString()
					}
					holder.tvOtherFileSize.text = rootView.resources.getString(
						strRes,
						fileSize
					)
				} else {
					holder.tvOtherFileSize.text = null
				}

				val text = message.Text
				if (!text.isNullOrEmpty()) {
					holder.otherText.visibility = View.VISIBLE
					holder.otherText.text = message.Text
				}

//                holder.otherText.setText(makeFileLink(file));
			}
		} else if (rating != null) {
			holder.otherRating.visibility = View.VISIBLE
			holder.otherRatingRate.visibility = View.GONE
			holder.otherRatingRated.visibility = View.GONE
			if (objectEquals(rating.State, RatingState.PENDING)) {
				holder.otherRatingRate.visibility = View.VISIBLE
				val value = rating.Value ?: 0
				val ratingButtons = arrayOf(
					holder.otherRatingRate1,
					holder.otherRatingRate2,
					holder.otherRatingRate3,
					holder.otherRatingRate4,
					holder.otherRatingRate5
				)
				for (i in ratingButtons.indices) {
					val button = ratingButtons[i]
					if (value >= i + 1) {
						button.setImageResource(R.drawable.star_filled)
					} else {
						button.setImageResource(R.drawable.star_empty)
					}
				}
			} else if (objectEquals(rating.State, RatingState.RATED)) {
				val value = rating.Value ?: 0
				val text = rootView.resources.getString(R.string.chat_ratings_rated, value)
				holder.otherRatingRated.visibility = View.VISIBLE
				holder.otherRatingRated.text = text
			} else {
				holder.otherRating.visibility = View.GONE
			}
		} else {
			holder.clTexts.visibility = View.VISIBLE
			holder.clTexts.setBackgroundResource(R.drawable.other_msg_bg)
			holder.otherText.visibility = View.VISIBLE
			holder.otherText.autoLinkMask = Linkify.ALL
			holder.otherText.text = message.Text
			holder.otherText.setTextColor(textColor())
		}
		val lp = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.WRAP_CONTENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)
		lp.setMargins(0, 0, toPx(40), 0)
		holder.clTexts.layoutParams = lp

		// Reply message (attached message)
		val replyToMessageId = message.ReplyToMessageId
		if (replyToMessageId != null && replyToMessageId > 0) {
			val replyMsg = findMessage(message)
			if (replyMsg != null) {
				holder.otherReply.showReplyingMessage(replyMsg)
				holder.otherReply.setCloseBtnVisibility(View.GONE)
				holder.otherReply.setVerticalDividerColor(R.color.other_reply_text)
				holder.otherReply.setTvSenderNameColor(R.color.other_reply_text)
				holder.otherReply.setTvTextColor(R.color.other_reply_text)
				holder.otherReply.layoutParams = lp
				holder.clTexts.setBackgroundResource(R.drawable.other_msg_reply_text_bg)
				holder.otherReply.post {
					if (holder.otherReply.width > holder.clTexts.width) {
						val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
							holder.otherReply.width,
							LinearLayout.LayoutParams.WRAP_CONTENT
						)
						layoutParams.setMargins(0, 0, toPx(40), 0)
						holder.clTexts.layoutParams = layoutParams
					} else {
						val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
							holder.clTexts.width,
							LinearLayout.LayoutParams.WRAP_CONTENT
						)
						layoutParams.setMargins(0, 0, toPx(40), 0)
						holder.otherReply.layoutParams = layoutParams
					}
				}
			}
		}

		// buttons
		val singleChoices = message.SingleChoices
		if (((message.Payload == ChatPayloadType.SINGLE_CHOICE) && !singleChoices.isNullOrEmpty())) {
			val isDropDown = message.IsDropDown
			if (isDropDown != null && isDropDown) {
				if (messages.indexOf(message) == (messages.size - 1)) {
					val flow = Flow(holder.itemView.context)
					flow.layoutParams = ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT
					)
					flow.setHorizontalStyle(Flow.CHAIN_PACKED)
					flow.setHorizontalAlign(Flow.HORIZONTAL_ALIGN_END)
					flow.setHorizontalGap(toPx(4))
					flow.setVerticalGap(toPx(4))
					flow.setWrapMode(Flow.WRAP_CHAIN)
					flow.setHorizontalBias(1f)
					holder.clDropdownBtns.removeAllViews()
					holder.clDropdownBtns.addView(flow)

					for (singleChoice: SingleChoice in singleChoices) {
						val btn = DropDownButton(holder.itemView.context)
						btn.id = View.generateViewId()
						btn.setSingleChoice(singleChoice)
						btn.setOnClickListener { v: View? ->
							itemClickListener.onButtonClick(
								message,
								singleChoice
							)
						}
						holder.clDropdownBtns.addView(btn)
						flow.addView(btn)
					}
					holder.clDropdownBtns.visibility = View.VISIBLE
				}
			} else {
				val adapter = ButtonsAdapter(object : ButtonsAdapter.ClickListener {
					override fun onClick(item: SingleChoice) {
						itemClickListener.onButtonClick(messages[holder.adapterPosition], item)
					}
				})
				adapter.setItems(message.SingleChoices)
				holder.rvButtons.adapter = adapter
				holder.rvButtons.visibility = View.VISIBLE
			}
		} else if ((((message.Payload == ChatPayloadType.CARD) || ((message.Payload == ChatPayloadType.CAROUSEL)))
					&& (message.Actions != null) && message.Actions?.isNotEmpty() == true)
		) {
			val adapter = ActionsAdapter(object : ActionsAdapter.ClickListener {
				override fun onClick(item: Action) {
					itemClickListener.onActionClick(
						messages[holder.adapterPosition], item
					)
				}
			})

			adapter.setItems(message.Actions)
			holder.rvCardBtns.adapter = adapter
			holder.rvCardBtns.visibility = View.VISIBLE
		}
	}

	private fun findMessage(message: ChatMessage): ChatMessage? {
		for (msg: ChatMessage in messages) {
			if (msg.Id == message.ReplyToMessageId) {
				return msg
			}
		}

		return null
	}

	private fun isNewDay(position: Int): Boolean {
		if (position == 0) {
			return true
		}
		val curMessage = messages[position]
		val prevMessage = messages[position - 1]
		if (curMessage.Date == null || prevMessage.Date == null) {
			return true
		}
		val cur = Calendar.getInstance()
		val prev = Calendar.getInstance()
		cur.time = curMessage.Date
		prev.time = prevMessage.Date
		return (cur[Calendar.YEAR] != prev[Calendar.YEAR]
				) || (cur[Calendar.MONTH] != prev[Calendar.MONTH]
				) || (cur[Calendar.DAY_OF_MONTH] != prev[Calendar.DAY_OF_MONTH])
	}

	private fun isGroupStart(position: Int): Boolean {
		if (position == 0) {
			return true
		}

		val cur = messages[position]
		val prev = messages[position - 1]
		return ((cur.My != prev.My
				) || !objectEquals(cur.UserId, prev.UserId)
				|| ((cur.CreatedAt - prev.CreatedAt) > GROUP_TIME_DELTA_MS))
	}

	private fun isGroupEnd(position: Int): Boolean {
		if (position == messages.size - 1) {
			return true
		}
		val cur = messages[position]
		val next = messages[position + 1]
		return ((cur.My != next.My
				) || !objectEquals(cur.UserId, next.UserId)
				|| ((next.CreatedAt - cur.CreatedAt) > GROUP_TIME_DELTA_MS))
	}

	private fun computeImageSizeFromFile(file: UploadedFile?): IntArray {
		if (file == null) {
			return intArrayOf(0, 0)
		}
		val imageWidth = file.ImageWidth ?: 0
		val imageHeight = file.ImageHeight ?: 0

		return computeImageSize(imageWidth, imageHeight)
	}

	private fun computeImageSize(imageWidth: Int, imageHeight: Int): IntArray {
		if (imageWidth == 0 || imageHeight == 0) {
			return intArrayOf(0, 0)
		}
		val width = (Math.min(rootView.width, rootView.height) * 3) / 5
		var height = (imageHeight * width) / imageWidth
		if (height > (width * 2)) {
			height = width * 2
		}
		return intArrayOf(width, height)
	}

	private fun makeFileLink(file: UploadedFile?): Spanned? {
		if (file == null) {
			return null
		}
		if (file.Url == null) {
			return null
		}

		val html = ("<a href=\"" + TextUtils.htmlEncode(file.Url) + "\">"
				+ TextUtils.htmlEncode(file.Name) + "</a>")
		return Html.fromHtml(html)
	}

	private fun onUploadCancelClicked(position: Int) {
		val message = messages[position]
		iqchannels.cancelUpload(message)
	}

	private fun onUploadRetryClicked(position: Int) {
		val message = messages[position]
		iqchannels.sendFile(message)
	}

	private fun onRateDown(position: Int, value: Int) {
		val message = messages[position]
		val rating = message.Rating ?: return
		if (rating.Sent) {
			return
		}

		rating.Value = value
		notifyItemChanged(position)
	}

	private fun onRateClicked(position: Int, value: Int) {
		val message = messages[position]
		val rating = message.Rating ?: return
		if (rating.Sent) {
			return
		}

		rating.Sent = true
		rating.Value = value
		iqchannels.ratingsRate(rating.Id, value)
		notifyItemChanged(position)
	}

	private fun onTextMessageClicked(position: Int) {
		val message = messages[position]
		val file = message.File ?: return
		val fileId = file.Id ?: return
		val fileName = file.Name ?: return

		iqchannels.filesUrl(fileId, object : HttpCallback<String?> {
			override fun onResult(result: String?) {
				result?.let {
					itemClickListener.onFileClick(result, fileName)
				}
			}

			override fun onException(exception: Exception) {}
		})
	}

	private fun onImageClicked(position: Int) {
		val message = messages[position]
		val file = message.File ?: return
		itemClickListener.onImageClick(message)
	}

	internal class ViewHolder @SuppressLint("ClickableViewAccessibility") constructor(
		private val adapter: ChatMessagesAdapter, itemView: View
	) : RecyclerView.ViewHolder(itemView) {

		val date: TextView

		// My
		val my: LinearLayout
		val myText: TextView
		val myUpload: LinearLayout
		val myUploadProgress: ProgressBar
		val myUploadError: TextView
		val myUploadCancel: Button
		val myUploadRetry: Button
		val myImageFrame: FrameLayout
		val myImageSrc: ImageView
		val clTextsMy: ConstraintLayout
		val tvMyFileName: TextView
		val tvMyFileSize: TextView
		val myFlags: LinearLayout
		val myDate: TextView
		val mySending: ProgressBar
		val myReceived: TextView
		val myRead: TextView
		val myReply: ReplyMessageView

		// Other
		val other: LinearLayout
		val otherAvatar: FrameLayout
		val otherAvatarImage: ImageView
		val otherAvatarText: TextView
		val otherName: TextView
		val clTexts: ConstraintLayout
		val otherText: TextView
		val tvOtherFileName: TextView
		val tvOtherFileSize: TextView
		val otherImageFrame: FrameLayout
		val otherImageSrc: ImageView
		val otherDate: TextView
		val otherReply: ReplyMessageView

		//private final TextView typing;
		// Rating
		val otherRating: LinearLayout
		val otherRatingRate: LinearLayout
		val otherRatingRate1: ImageButton
		val otherRatingRate2: ImageButton
		val otherRatingRate3: ImageButton
		val otherRatingRate4: ImageButton
		val otherRatingRate5: ImageButton
		val otherRatingRated: TextView

		// Buttons
		val rvButtons: RecyclerView
		val clDropdownBtns: ConstraintLayout
		val rvCardBtns: RecyclerView

		init {
			date = itemView.findViewById<View>(R.id.date) as TextView

			// My
			my = itemView.findViewById<View>(R.id.my) as LinearLayout
			myText = itemView.findViewById<View>(R.id.myText) as TextView
			myUpload = itemView.findViewById<View>(R.id.myUpload) as LinearLayout
			myUploadProgress = itemView.findViewById<View>(R.id.myUploadProgress) as ProgressBar
			myUploadError = itemView.findViewById<View>(R.id.myUploadError) as TextView
			myUploadCancel = itemView.findViewById<View>(R.id.myUploadCancel) as Button
			myUploadRetry = itemView.findViewById<View>(R.id.myUploadRetry) as Button
			clTextsMy = itemView.findViewById(R.id.cl_texts_my)
			tvMyFileName = itemView.findViewById<View>(R.id.tvMyFileName) as TextView
			tvMyFileSize = itemView.findViewById<View>(R.id.tvMyFileSize) as TextView
			if (tvMyFileName.visibility == View.VISIBLE) {
				tvMyFileName.setOnClickListener {
					adapter.onTextMessageClicked(
						adapterPosition
					)
				}
			}
			myImageFrame = itemView.findViewById<View>(R.id.myImageFrame) as FrameLayout
			myImageSrc = itemView.findViewById<View>(R.id.myImageSrc) as ImageView
			myImageSrc.setOnClickListener { v: View? -> adapter.onImageClicked(adapterPosition) }
			myFlags = itemView.findViewById<View>(R.id.myFlags) as LinearLayout
			myDate = itemView.findViewById<View>(R.id.myDate) as TextView
			mySending = itemView.findViewById<View>(R.id.mySending) as ProgressBar
			myReceived = itemView.findViewById<View>(R.id.myReceived) as TextView
			myRead = itemView.findViewById<View>(R.id.myRead) as TextView
			myReply = itemView.findViewById(R.id.myReply)
			myUploadCancel.setOnClickListener { adapter.onUploadCancelClicked(adapterPosition) }
			myUploadRetry.setOnClickListener { adapter.onUploadRetryClicked(adapterPosition) }

			// Other
			other = itemView.findViewById<View>(R.id.other) as LinearLayout
			otherReply = itemView.findViewById(R.id.otherReply)
			otherAvatar = itemView.findViewById<View>(R.id.otherAvatar) as FrameLayout
			otherAvatarImage = itemView.findViewById<View>(R.id.otherAvatarImage) as ImageView
			otherAvatarText = itemView.findViewById<View>(R.id.otherAvatarText) as TextView
			otherName = itemView.findViewById<View>(R.id.otherName) as TextView
			clTexts = itemView.findViewById(R.id.cl_texts)
			otherText = itemView.findViewById<View>(R.id.otherText) as TextView
			tvOtherFileName = itemView.findViewById<View>(R.id.tvOtherFileName) as TextView
			tvOtherFileSize = itemView.findViewById<View>(R.id.tvOtherFileSize) as TextView
			tvOtherFileName.setOnClickListener { adapter.onTextMessageClicked(adapterPosition) }
			otherImageFrame = itemView.findViewById<View>(R.id.otherImageFrame) as FrameLayout
			otherImageSrc = itemView.findViewById<View>(R.id.otherImageSrc) as ImageView
			otherDate = itemView.findViewById<View>(R.id.otherDate) as TextView
			otherImageSrc.setOnClickListener { v: View? -> adapter.onImageClicked(adapterPosition) }

			//typing = (TextView) itemView.findViewById(R.id.typing);

			// Rating
			otherRating = itemView.findViewById(R.id.rating)
			otherRatingRate = itemView.findViewById(R.id.rating_rate)
			otherRatingRate1 = itemView.findViewById(R.id.rating_rate_1)
			otherRatingRate2 = itemView.findViewById(R.id.rating_rate_2)
			otherRatingRate3 = itemView.findViewById(R.id.rating_rate_3)
			otherRatingRate4 = itemView.findViewById(R.id.rating_rate_4)
			otherRatingRate5 = itemView.findViewById(R.id.rating_rate_5)
			otherRatingRated = itemView.findViewById(R.id.rating_rated)
			val ratingButtons = arrayOf(
				otherRatingRate1,
				otherRatingRate2,
				otherRatingRate3,
				otherRatingRate4,
				otherRatingRate5
			)
			for (button: ImageButton in ratingButtons) {
				button.setOnTouchListener { view, motionEvent ->
					onRateButtonTouch(
						view,
						motionEvent
					)
				}
				button.setOnClickListener { view -> onRateButtonClick(view) }
			}
			rvButtons = itemView.findViewById(R.id.rv_buttons)
			clDropdownBtns = itemView.findViewById(R.id.cl_dropdown_btns)
			rvCardBtns = itemView.findViewById(R.id.rv_card_buttons)
			rvCardBtns.addItemDecoration(MarginItemDecoration())
		}

		private fun onRateButtonTouch(view: View, event: MotionEvent): Boolean {
			if (event.action != MotionEvent.ACTION_DOWN) {
				return false
			}
			val value = getRateButtonValue(view)
			adapter.onRateDown(adapterPosition, value)
			return false
		}

		private fun onRateButtonClick(view: View) {
			val value = getRateButtonValue(view)
			if (value == 0) {
				return
			}
			adapter.onRateClicked(adapterPosition, value)
		}

		private fun getRateButtonValue(view: View): Int {
			var value = 0
			when (view.id) {
				R.id.rating_rate_1 -> {
					value = 1
				}
				R.id.rating_rate_2 -> {
					value = 2
				}
				R.id.rating_rate_3 -> {
					value = 3
				}
				R.id.rating_rate_4 -> {
					value = 4
				}
				R.id.rating_rate_5 -> {
					value = 5
				}
			}

			return value
		}
	}

	internal interface ItemClickListener {
		fun onFileClick(url: String, fileName: String)
		fun onImageClick(message: ChatMessage)
		fun onButtonClick(message: ChatMessage, singleChoice: SingleChoice)
		fun onActionClick(message: ChatMessage, action: Action)
	}
}
