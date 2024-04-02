package ru.iqchannels.sdk.ui.rv

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.helper.widget.Flow
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import java.text.DateFormat
import java.text.DecimalFormat
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.databinding.ItemOtherMessageBinding
import ru.iqchannels.sdk.schema.Action
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ChatPayloadType
import ru.iqchannels.sdk.schema.RatingState
import ru.iqchannels.sdk.schema.SingleChoice
import ru.iqchannels.sdk.ui.ActionsAdapter
import ru.iqchannels.sdk.ui.ButtonsAdapter
import ru.iqchannels.sdk.ui.ChatMessagesAdapter
import ru.iqchannels.sdk.ui.Colors
import ru.iqchannels.sdk.ui.UiUtils
import ru.iqchannels.sdk.ui.widgets.DropDownButton

internal class OtherMessageViewHolder(
	private val binding: ItemOtherMessageBinding,
	private val itemClickListener: ChatMessagesAdapter.ItemClickListener
) : ViewHolder(binding.root) {

	private val dateFormat: DateFormat = android.text.format.DateFormat.getDateFormat(binding.root.context)
	private val timeFormat: DateFormat = android.text.format.DateFormat.getTimeFormat(binding.root.context)

	fun bind(message: ChatMessage, rootViewDimens: Pair<Int, Int>) = with(binding) {
		val adapter = bindingAdapter as? ChatMessagesAdapter ?: return@with

		// Day
		if (adapter.isNewDay(bindingAdapterPosition) && message.Payload !== ChatPayloadType.TYPING) {
			date.text = message.Date?.let { dateFormat.format(it) }
			date.visibility = View.VISIBLE
		} else {
			date.visibility = View.GONE
		}

		val groupStart = adapter.isGroupStart(bindingAdapterPosition)
		val groupEnd = adapter.isGroupEnd(bindingAdapterPosition)
		other.visibility = View.VISIBLE

		otherImageSrc.setOnClickListener { adapter.onImageClicked(bindingAdapterPosition) }
		tvOtherFileName.setOnClickListener { adapter.onTextMessageClicked(bindingAdapterPosition) }

		// Name and avatar
		val user = message.User
		if (groupStart && user != null) {
			val name = user.DisplayName
			val letter = if (name?.isEmpty() == true) "" else name?.substring(0, 1)
			otherName.text = name
			otherName.visibility = View.VISIBLE
			otherAvatar.visibility = View.VISIBLE
			val avatarUrl = user.AvatarUrl
			if (!avatarUrl.isNullOrEmpty()) {
				// Avatar image
				otherAvatarText.visibility = View.GONE
				otherAvatarImage.visibility = View.VISIBLE

				Glide.with(root.context)
					.load(avatarUrl)
					.placeholder(R.drawable.avatar_placeholder)
					.into(otherAvatarImage)
			} else {
				// Avatar circle with a letter inside
				otherAvatarImage.visibility = View.GONE
				otherAvatarText.visibility = View.VISIBLE
				otherAvatarText.text = letter
				otherAvatarText.setBackgroundColor(Colors.paletteColor(letter))
			}
		} else {
			otherName.visibility = View.GONE
			otherAvatar.visibility = View.INVISIBLE
		}

		// Time
		if (groupEnd && message.Date != null) {
			otherDate.text = message.Date?.let { timeFormat.format(it) } ?: ""
			otherDate.visibility = View.VISIBLE
		} else {
			otherDate.visibility = View.GONE
		}

		// Message

		// Reset the visibility.
		run {
			clTexts.visibility = View.GONE
			otherText.visibility = View.GONE
			tvOtherFileName.visibility = View.GONE
			tvOtherFileSize.visibility = View.GONE
			otherImageFrame.visibility = View.GONE
			rating.root.visibility = View.GONE
			otherReply.visibility = View.GONE
			rvButtons.visibility = View.GONE
			clDropdownBtns.visibility = View.GONE
			rvCardButtons.visibility = View.GONE
		}

		val file = message.File
		val msgRating = message.Rating
		if (file != null) {
			val imageUrl = file.ImagePreviewUrl
			if (imageUrl != null) {
				val size = Utils.computeImageSizeFromFile(file, rootViewDimens)
				val text = message.Text
				if (!text.isNullOrEmpty()) {
					clTexts.visibility = View.VISIBLE
					clTexts.setBackgroundResource(R.drawable.other_msg_reply_text_bg)
					otherText.visibility = View.VISIBLE
					otherText.text = message.Text
				} else {
					otherText.visibility = View.GONE
				}
				otherImageFrame.visibility = View.VISIBLE

				val withText = !text.isNullOrEmpty()
				if (withText) {
					otherImageFrame.setBackgroundResource(R.drawable.other_msg_reply_bg)
				} else {
					otherImageFrame.setBackgroundResource(R.drawable.other_msg_bg)
				}

				otherImageFrame.layoutParams.width = size[0]
				otherImageFrame.layoutParams.height = size[1]
				otherImageFrame.requestLayout()
				val radius = UiUtils.toPx(12).toFloat()
				Glide.with(otherImageFrame.context)
					.load(imageUrl)
					.transform(
						GranularRoundedCorners(
							radius,
							radius,
							if (withText) 0f else radius,
							if (withText) 0f else radius,
						)
					)
					.into(otherImageSrc)
				otherImageSrc.post {
					val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
						otherImageFrame.width,
						LinearLayout.LayoutParams.WRAP_CONTENT
					)
					layoutParams.setMargins(0, 0, UiUtils.toPx(40), 0)
					clTexts.layoutParams = layoutParams
				}
			} else {
				otherImageFrame.visibility = View.GONE
				clTexts.visibility = View.VISIBLE
				clTexts.setBackgroundResource(R.drawable.other_msg_bg)
				tvOtherFileName.visibility = View.VISIBLE
				tvOtherFileName.autoLinkMask = 0
				tvOtherFileName.movementMethod = LinkMovementMethod.getInstance()
				tvOtherFileName.text = file.Name
				if (file.Size > 0) {
					tvOtherFileSize.visibility = View.VISIBLE
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
					tvOtherFileSize.text = root.resources.getString(
						strRes,
						fileSize
					)
				} else {
					tvOtherFileSize.text = null
				}

				val text = message.Text
				if (!text.isNullOrEmpty()) {
					otherText.visibility = View.VISIBLE
					otherText.text = message.Text
				}

//                holder.otherText.setText(makeFileLink(file));
			}
		} else if (msgRating != null) {
			this.rating.root.visibility = View.VISIBLE
			this.rating.ratingRate.visibility = View.GONE
			this.rating.ratingRated.visibility = View.GONE
			if (ChatMessagesAdapter.objectEquals(msgRating.State, RatingState.PENDING)) {
				this.rating.ratingRate.visibility = View.VISIBLE
				val value = msgRating.Value ?: 0
				val ratingButtons = arrayOf(
					this.rating.ratingRate1,
					this.rating.ratingRate2,
					this.rating.ratingRate3,
					this.rating.ratingRate4,
					this.rating.ratingRate5
				)
				for (i in ratingButtons.indices) {
					val button = ratingButtons[i]
					if (value >= i + 1) {
						button.setImageResource(R.drawable.star_filled)
					} else {
						button.setImageResource(R.drawable.star_empty)
					}
				}

				for (button: ImageButton in ratingButtons) {
					button.setOnTouchListener { view, motionEvent ->
						onRateButtonTouch(
							view,
							motionEvent
						)
					}
				}

				message.Rating?.Value?.takeIf { it > 0 }?.let { rate ->
					this.rating.btnSendRating.isEnabled = message.Rating?.Sent != true
					this.rating.btnSendRating.setOnClickListener {
						onRateButtonClick(rate)
					}
				}

			} else if (ChatMessagesAdapter.objectEquals(msgRating.State, RatingState.RATED)) {
				val value = msgRating.Value ?: 0
				val text = root.resources.getString(R.string.chat_ratings_rated, value)
				this.rating.ratingRated.visibility = View.VISIBLE
				this.rating.ratingRated.text = text
			} else {
				rating.root.visibility = View.GONE
			}
		} else {
			clTexts.visibility = View.VISIBLE
			clTexts.setBackgroundResource(R.drawable.other_msg_bg)
			otherText.visibility = View.VISIBLE
			otherText.autoLinkMask = Linkify.ALL
			otherText.text = message.Text
			otherText.setTextColor(Colors.textColor())
		}
		val lp = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.WRAP_CONTENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)
		lp.setMargins(0, 0, UiUtils.toPx(40), 0)
		clTexts.layoutParams = lp

		// Reply message (attached message)
		val replyToMessageId = message.ReplyToMessageId
		if (replyToMessageId != null && replyToMessageId > 0) {
			val replyMsg = adapter.findMessage(message)
			if (replyMsg != null) {
				otherReply.showReplyingMessage(replyMsg)
				otherReply.setCloseBtnVisibility(View.GONE)
				otherReply.setVerticalDividerColor(R.color.other_reply_text)
				otherReply.setTvSenderNameColor(R.color.other_reply_text)
				otherReply.setTvTextColor(R.color.other_reply_text)
				otherReply.layoutParams = lp
				clTexts.setBackgroundResource(R.drawable.other_msg_reply_text_bg)
				otherReply.post {
					if (otherReply.width > clTexts.width) {
						val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
							otherReply.width,
							LinearLayout.LayoutParams.WRAP_CONTENT
						)
						layoutParams.setMargins(0, 0, UiUtils.toPx(40), 0)
						clTexts.layoutParams = layoutParams
					} else {
						val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
							clTexts.width,
							LinearLayout.LayoutParams.WRAP_CONTENT
						)
						layoutParams.setMargins(0, 0, UiUtils.toPx(40), 0)
						otherReply.layoutParams = layoutParams
					}
				}
			}
		}

		// buttons
		val singleChoices = message.SingleChoices
		if (((message.Payload == ChatPayloadType.SINGLE_CHOICE) && !singleChoices.isNullOrEmpty())) {
			val isDropDown = message.IsDropDown
			if (isDropDown != null && isDropDown) {
				if (adapter.isLast(message)) {
					val flow = Flow(itemView.context)
					flow.layoutParams = ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT
					)
					flow.setHorizontalStyle(Flow.CHAIN_PACKED)
					flow.setHorizontalAlign(Flow.HORIZONTAL_ALIGN_END)
					flow.setHorizontalGap(UiUtils.toPx(4))
					flow.setVerticalGap(UiUtils.toPx(4))
					flow.setWrapMode(Flow.WRAP_CHAIN)
					flow.setHorizontalBias(1f)
					clDropdownBtns.removeAllViews()
					clDropdownBtns.addView(flow)

					for (singleChoice: SingleChoice in singleChoices) {
						val btn = DropDownButton(itemView.context)
						btn.id = View.generateViewId()
						btn.setSingleChoice(singleChoice)
						btn.setOnClickListener { v: View? ->
							itemClickListener.onButtonClick(
								message,
								singleChoice
							)
						}
						clDropdownBtns.addView(btn)
						flow.addView(btn)
					}
					clDropdownBtns.visibility = View.VISIBLE
				}
			} else {
				val adapter = ButtonsAdapter(object : ButtonsAdapter.ClickListener {
					override fun onClick(item: SingleChoice) {
						itemClickListener.onButtonClick(message, item)
					}
				})
				adapter.setItems(message.SingleChoices)
				rvButtons.adapter = adapter
				rvButtons.visibility = View.VISIBLE
			}
		} else if ((((message.Payload == ChatPayloadType.CARD) || ((message.Payload == ChatPayloadType.CAROUSEL)))
					&& (message.Actions != null) && message.Actions?.isNotEmpty() == true)
		) {
			val actionsAdapter = ActionsAdapter(object : ActionsAdapter.ClickListener {
				override fun onClick(item: Action) {
					itemClickListener.onActionClick(
						message, item
					)
				}
			})

			actionsAdapter.setItems(message.Actions)
			rvCardButtons.adapter = actionsAdapter
			rvCardButtons.visibility = View.VISIBLE
		}

		binding.root.setOnLongClickListener {
			itemClickListener.onMessageLongClick(message)
			true
		}
	}

	private fun onRateButtonTouch(view: View, event: MotionEvent): Boolean {
		if (event.action != MotionEvent.ACTION_DOWN) {
			return false
		}
		val value = getRateButtonValue(view)
		(bindingAdapter as? ChatMessagesAdapter)?.onRateDown(adapterPosition, value)
		return false
	}

	private fun onRateButtonClick(value: Int) {
		if (value == 0) {
			return
		}

		(bindingAdapter as? ChatMessagesAdapter)?.onRateClicked(adapterPosition, value)
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