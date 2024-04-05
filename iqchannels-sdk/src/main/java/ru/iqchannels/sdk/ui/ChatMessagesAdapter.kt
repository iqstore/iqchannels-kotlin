/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.ui

import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.databinding.ItemMyMessageBinding
import ru.iqchannels.sdk.databinding.ItemOtherMessageBinding
import ru.iqchannels.sdk.http.HttpCallback
import ru.iqchannels.sdk.schema.Action
import ru.iqchannels.sdk.schema.ActorType
import ru.iqchannels.sdk.schema.ChatEvent
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ChatPayloadType
import ru.iqchannels.sdk.schema.SingleChoice
import ru.iqchannels.sdk.schema.UploadedFile
import ru.iqchannels.sdk.ui.rv.MyMessageViewHolder
import ru.iqchannels.sdk.ui.rv.OtherMessageViewHolder

internal class ChatMessagesAdapter(
	private val iqchannels: IQChannels,
	private val rootView: View,
	private val rootViewDimens: () -> Pair<Int, Int>,
	private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	companion object {
		private val GROUP_TIME_DELTA_MS = 60000

		fun objectEquals(a: Any?, b: Any?): Boolean {
			return (a === b) || (a != null && (a == b))
		}
	}

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

	fun getItemPosition(message: ChatMessage): Int {
		return messages.find { it.Id == message.Id }?.let {
			messages.indexOf(it)
		} ?: -1
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

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val context = parent.context
		val inflater = LayoutInflater.from(context)

		return when(viewType) {
			R.layout.item_my_message -> {
				val binding = ItemMyMessageBinding.inflate(inflater, parent, false)
				MyMessageViewHolder(binding, itemClickListener)
			}
			else -> {
				val binding = ItemOtherMessageBinding.inflate(inflater, parent, false)
				OtherMessageViewHolder(binding, itemClickListener)
			}
		}
	}

	override fun getItemViewType(position: Int): Int {
		return if (getItem(position).My) {
			R.layout.item_my_message
		} else {
			R.layout.item_other_message
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val message = messages[position]

		when(holder) {
			is MyMessageViewHolder -> holder.bind(message, rootViewDimens())
			is OtherMessageViewHolder -> holder.bind(message, rootViewDimens())
			else -> Unit
		}

		iqchannels.markAsRead(message)
	}

	fun findMessage(message: ChatMessage): ChatMessage? {
		for (msg: ChatMessage in messages) {
			if (msg.Id == message.ReplyToMessageId) {
				return msg
			}
		}

		return null
	}

	fun isNewDay(position: Int): Boolean {
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

	fun isGroupStart(position: Int): Boolean {
		if (position == 0) {
			return true
		}

		val cur = messages[position]
		val prev = messages[position - 1]
		return ((cur.My != prev.My
				) || !objectEquals(cur.UserId, prev.UserId)
				|| ((cur.CreatedAt - prev.CreatedAt) > GROUP_TIME_DELTA_MS))
	}

	fun isGroupEnd(position: Int): Boolean {
		if (position == messages.size - 1) {
			return true
		}
		val cur = messages[position]
		val next = messages[position + 1]
		return ((cur.My != next.My
				) || !objectEquals(cur.UserId, next.UserId)
				|| ((next.CreatedAt - cur.CreatedAt) > GROUP_TIME_DELTA_MS))
	}

	fun isLast(message: ChatMessage) = messages.indexOf(message) == (messages.size - 1)

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

	fun onUploadCancelClicked(position: Int) {
		val message = messages[position]
		iqchannels.cancelUpload(message)
	}

	fun onUploadRetryClicked(position: Int) {
		val message = messages[position]
		iqchannels.sendFile(message)
	}

	fun onRateDown(position: Int, value: Int) {
		val message = messages[position]
		val rating = message.Rating ?: return
		if (rating.Sent) {
			return
		}

		rating.Value = value
		notifyItemChanged(position)
	}

	fun onRateClicked(position: Int, value: Int) {
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

	fun onTextMessageClicked(position: Int) {
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

	fun onImageClicked(position: Int) {
		val message = messages[position]
		val file = message.File ?: return
		itemClickListener.onImageClick(message)
	}

	internal interface ItemClickListener {
		fun onFileClick(url: String, fileName: String)
		fun onImageClick(message: ChatMessage)
		fun onButtonClick(message: ChatMessage, singleChoice: SingleChoice)
		fun onActionClick(message: ChatMessage, action: Action)
		fun onMessageLongClick(message: ChatMessage)
		fun fileUploadException(errorMessage: String?)
		fun onReplyMessageClick(message: ChatMessage)
	}
}
