package ru.iqchannels.sdk.ui.rv

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.text.util.Linkify
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.helper.widget.Flow
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import io.noties.markwon.Markwon
import java.text.DateFormat
import java.text.DecimalFormat
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.applyIQStyles
import ru.iqchannels.sdk.databinding.ItemOtherMessageBinding
import ru.iqchannels.sdk.http.HttpCallback
import ru.iqchannels.sdk.schema.Action
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.ChatPayloadType
import ru.iqchannels.sdk.schema.FileValidState
import ru.iqchannels.sdk.schema.PollOptionType
import ru.iqchannels.sdk.schema.Rating
import ru.iqchannels.sdk.schema.RatingPollClientAnswerInput
import ru.iqchannels.sdk.schema.RatingState
import ru.iqchannels.sdk.schema.SingleChoice
import ru.iqchannels.sdk.schema.UploadedFile
import ru.iqchannels.sdk.setBackgroundDrawable
import ru.iqchannels.sdk.setBackgroundStyle
import ru.iqchannels.sdk.styling.IQStyles
import ru.iqchannels.sdk.styling.Text
import ru.iqchannels.sdk.ui.ActionsAdapter
import ru.iqchannels.sdk.ui.ButtonsAdapter
import ru.iqchannels.sdk.ui.ChatMessagesAdapter
import ru.iqchannels.sdk.ui.Colors
import ru.iqchannels.sdk.ui.UiUtils
import ru.iqchannels.sdk.ui.UiUtils.getRatingScaleMaxValue
import ru.iqchannels.sdk.ui.widgets.DropDownButton
import ru.iqchannels.sdk.Log
import kotlin.math.max

interface RatingPollListener {
	fun onRatingPollAnswersSend(
		answers: List<RatingPollClientAnswerInput>,
		ratingId: Long,
		pollId: Long,
		callback: HttpCallback<Void>
	)
	fun onRatingPollFinished(value: Int?)
}

internal class OtherMessageViewHolder(
	private val binding: ItemOtherMessageBinding,
	private val itemClickListener: ChatMessagesAdapter.ItemClickListener
) : ViewHolder(binding.root), RatingPollListener {

	private val dateFormat: DateFormat =
		android.text.format.DateFormat.getDateFormat(binding.root.context)
	private val timeFormat: DateFormat =
		android.text.format.DateFormat.getTimeFormat(binding.root.context)

	fun bind(message: ChatMessage, rootViewDimens: Pair<Int, Int>, markwon: Markwon) =
		with(binding) {
			val adapter = bindingAdapter as? ChatMessagesAdapter ?: return@with

			// Day
			if (adapter.isNewDay(bindingAdapterPosition) && message.Payload !== ChatPayloadType.TYPING) {
				date.text = message.Date?.let { dateFormat.format(it) }
				date.visibility = View.VISIBLE
				date.applyIQStyles(IQStyles.iqChannelsStyles?.chat?.dateText)
			} else {
				date.visibility = View.GONE
			}

			val groupStart = adapter.isGroupStart(bindingAdapterPosition)
//			val groupEnd = adapter.isGroupEnd(bindingAdapterPosition)
			other.visibility = View.VISIBLE

			otherImageSrc.setOnClickListener { adapter.onImageClicked(bindingAdapterPosition) }
			tvOtherFileName.setOnClickListener { adapter.onTextMessageClicked(bindingAdapterPosition) }

			// Name and avatar
			val user = message.User
			if (groupStart && user != null && message.Rating == null) {
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
//			if (groupEnd && message.Date != null) {
			otherDate.text = message.Date?.let { timeFormat.format(it) } ?: ""
//				otherDate.visibility = View.VISIBLE
//			} else {
//				otherDate.visibility = View.GONE
//			}

			run {
				IQStyles.iqChannelsStyles?.messages?.backgroundOperator
					?.let {
						otherReply.setBackgroundDrawable(it, R.drawable.other_msg_reply_bg)
						rating.root.setBackgroundDrawable(it, R.drawable.other_msg_bg)
					}
				IQStyles.iqChannelsStyles?.rating?.backgroundContainer
					?.let { rating.root.setBackgroundDrawable(it, R.drawable.other_msg_bg) }

				otherText.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.textOperator)
				otherDate.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.textTimeOperator)
				otherName.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.textUp)

				otherReply.tvSenderName.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.replySenderTextOperator)
				otherReply.tvText.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.replyTextOperator)
				otherReply.tvFileName.applyIQStyles(IQStyles.iqChannelsStyles?.messages?.replyTextOperator)

				tvOtherFileSize.applyIQStyles(IQStyles.iqChannelsStyles?.messageFile?.textFileSizeOperator)
				tvOtherFileName.applyIQStyles(IQStyles.iqChannelsStyles?.messageFile?.textFilenameOperator)

				IQStyles.iqChannelsStyles?.messageFile?.iconFileOperator?.let {
					Glide.with(root.context)
						.load(it)
						.into(ivFile)
				} ?: run {
					ivFile.imageTintList = ColorStateList.valueOf(
						ContextCompat.getColor(root.context, R.color.other_file_icon)
					)
				}
			}

			// Reset the visibility.
			run {
				clTexts.visibility = View.GONE
				otherText.visibility = View.GONE
				tvOtherFileName.visibility = View.GONE
				ivFile.visibility = View.GONE
				tvOtherFileSize.visibility = View.GONE
				otherImageFrame.visibility = View.GONE
				rating.root.visibility = View.GONE
				otherReply.visibility = View.GONE
				rvButtons.visibility = View.GONE
				clDropdownBtns.visibility = View.GONE
				rvCardButtons.visibility = View.GONE
				ratingPoll.root.visibility = View.GONE
			}

			val file = message.File
			val msgRating = message.Rating
			if (file != null) {
				when (message.File?.State) {
					FileValidState.Rejected -> showFileStateMsg(
						R.string.unsecure_file,
						R.color.red,
						IQStyles.iqChannelsStyles?.messages?.textFileStateRejectedOperator
					)

					FileValidState.OnChecking -> showFileStateMsg(
						R.string.file_on_checking,
						R.color.blue,
						IQStyles.iqChannelsStyles?.messages?.textFileStateOnCheckingOperator
					)

					FileValidState.SentForChecking -> showFileStateMsg(
						R.string.file_sent_to_check,
						R.color.blue,
						IQStyles.iqChannelsStyles?.messages?.textFileStateSentForCheckingOperator
					)

					FileValidState.CheckError -> showFileStateMsg(
						R.string.error_on_checking,
						R.color.red,
						IQStyles.iqChannelsStyles?.messages?.textFileStateCheckErrorOperator
					)

					else -> showApprovedState(message, file, rootViewDimens, markwon)
				}
			} else if (msgRating != null) {
				message.System = true
				if (msgRating.State == RatingState.POLL && msgRating.RatingPoll != null) {
					showRatingPoll(msgRating, ratingPoll)
				} else {
					showRating(msgRating, rating)
				}
			} else {
				clTexts.visibility = View.VISIBLE
				clTexts.setBackgroundDrawable(
					IQStyles.iqChannelsStyles?.messages?.backgroundOperator,
					R.drawable.other_msg_bg
				)

				otherText.visibility = View.VISIBLE
				otherText.autoLinkMask = Linkify.ALL
				otherText.setTextColor(Colors.textColor())
				otherText.text = message.Text
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
					otherReply.setVerticalDividerColor(R.color.red)
					otherReply.setTvSenderNameColor(R.color.dark_text_color)
					otherReply.setTvTextColor(R.color.other_name)
					otherReply.layoutParams = lp
					clTexts.setBackgroundDrawable(
						IQStyles.iqChannelsStyles?.messages?.backgroundOperator,
						R.drawable.other_msg_reply_text_bg
					)

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

					otherReply.setOnClickListener {
						itemClickListener.onReplyMessageClick(replyMsg)
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
							val btn = DropDownButton(itemView.context).apply {
								setBackgroundStyle(
									IQStyles.iqChannelsStyles?.singleChoiceBtnStyles?.backgroundIvr,
									IQStyles.iqChannelsStyles?.singleChoiceBtnStyles?.borderIvr,
									android.R.color.transparent,
									R.color.drop_down_btn_border,
									1,
									10f
								)

								applyIQStyles(IQStyles.iqChannelsStyles?.singleChoiceBtnStyles?.textIvr)
							}
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
		(bindingAdapter as? ChatMessagesAdapter)?.onRateDown(bindingAdapterPosition, value)
		return false
	}

	private fun onRateButtonClick(value: Int) {
		if (value == 0) {
			return
		}

		(bindingAdapter as? ChatMessagesAdapter)?.onRateClicked(adapterPosition, value)
	}

	override fun onRatingPollAnswersSend(
		answers: List<RatingPollClientAnswerInput>,
		ratingId: Long,
		pollId: Long,
		callback: HttpCallback<Void>,
	) {
		(bindingAdapter as? ChatMessagesAdapter)?.onRatingPollAnswersSend(
			answers,
			ratingId,
			pollId,
			callback
		)
	}

	override fun onRatingPollFinished(value: Int?) {
		if (value != null) {
			(bindingAdapter as? ChatMessagesAdapter)?.onRatePollClicked(adapterPosition, value)
		}
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

	private fun showRatingPoll(
		msgRating: Rating,
		ratingPollBinding: ru.iqchannels.sdk.databinding.ChatRatingPollBinding,
	) = with(binding) {
		IQStyles.iqChannelsStyles?.messages?.backgroundOperator
			?.let {
				rating.root.setBackgroundDrawable(it, R.drawable.other_msg_rating_poll_bg)
			}
		val poll = msgRating.RatingPoll
		val pollViewHolder = RatingPollViewHolder(ratingPollBinding)
		binding.otherAvatar.visibility = View.GONE
		binding.date.visibility = View.GONE
		pollViewHolder.bindPoll(poll!!, msgRating)
		pollViewHolder.setRatingPollListener(this@OtherMessageViewHolder)
		ratingPoll.root.visibility = View.VISIBLE
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun showRating(
		msgRating: Rating,
		rating: ru.iqchannels.sdk.databinding.ChatRatingBinding,
	) = with(binding) {
		rating.root.visibility = View.VISIBLE
		rating.ratingRate.visibility = View.GONE
		rating.ratingRated.visibility = View.GONE
		rating.ratingRateText.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.ratingTitle)

		when (msgRating.State) {
			RatingState.FINISHED, RatingState.RATED -> {
				val value = msgRating.Value ?: 0
				val text = root.resources.getString(R.string.chat_ratings_rated_custom, value, getRatingScaleMaxValue(msgRating))
				rating.ratingRated.visibility = View.VISIBLE
				rating.ratingRated.text = text
				rating.ratingRated.applyIQStyles(IQStyles.iqChannelsStyles?.chat?.systemText)
			}
			RatingState.IGNORED -> {
				rating.ratingRated.visibility = View.VISIBLE
				val text = root.resources.getString(R.string.chat_ratings_ignored)
				rating.ratingRated.text = text
				rating.ratingRated.applyIQStyles(IQStyles.iqChannelsStyles?.chat?.systemText)
			}
			RatingState.PENDING -> {
				IQStyles.iqChannelsStyles?.rating?.sentRating?.let {
					val states = arrayOf(
						intArrayOf(android.R.attr.state_enabled),
						intArrayOf(-android.R.attr.state_enabled),
					)

					val btnColors = intArrayOf(
						it.backgroundEnabled?.color?.getColorInt(root.context) ?: ContextCompat.getColor(
							root.context,
							R.color.red
						),
						it.backgroundDisabled?.color?.getColorInt(root.context) ?: ContextCompat.getColor(
							root.context,
							R.color.disabled_grey
						)
					)

					rating.btnSendRating.backgroundTintList = ColorStateList(states, btnColors)
				}

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
						IQStyles.iqChannelsStyles?.rating?.fullStar?.let {
							Glide.with(root.context)
								.load(it)
								.into(button)
						} ?: run {
							button.setImageResource(R.drawable.star_filled)
						}
					} else {
						IQStyles.iqChannelsStyles?.rating?.emptyStar?.let {
							Glide.with(root.context)
								.load(it)
								.into(button)
						} ?: run {
							button.setImageResource(R.drawable.star_empty)
						}
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

				msgRating.Value?.takeIf { it > 0 }?.let { rate ->
					this.rating.btnSendRating.isEnabled = msgRating.Sent != true
					this.rating.btnSendRating.setOnClickListener {
						onRateButtonClick(rate)
					}
				}

				run {
					if (rating.btnSendRating.isEnabled) {
						rating.btnSendRating.applyIQStyles(
							IQStyles.iqChannelsStyles?.rating?.sentRating?.textEnabled
						)
					} else {
						rating.btnSendRating.applyIQStyles(
							IQStyles.iqChannelsStyles?.rating?.sentRating?.textDisabled
						)
					}
				}
			}
			else -> {
				rating.root.visibility = View.GONE
			}
		}
	}

	private fun showApprovedState(
		message: ChatMessage,
		file: UploadedFile,
		rootViewDimens: Pair<Int, Int>,
		markwon: Markwon
	) {
		binding.apply {
			val imageUrl = file.ImagePreviewUrl
			if (imageUrl != null) {
				val size = Utils.computeImageSizeFromFile(file, rootViewDimens)
				val text = message.Text
				if (!text.isNullOrEmpty()) {
					clTexts.visibility = View.VISIBLE
					clTexts.setBackgroundDrawable(
						IQStyles.iqChannelsStyles?.messages?.backgroundOperator,
						R.drawable.other_msg_reply_text_bg
					)
					otherText.visibility = View.VISIBLE
					message.Text?.let {
						markwon.setMarkdown(otherText, it)
					}
				} else {
					otherText.visibility = View.GONE
				}
				otherImageFrame.visibility = View.VISIBLE

				val withText = !text.isNullOrEmpty()
				if (withText) {
					otherImageFrame.setBackgroundDrawable(
						IQStyles.iqChannelsStyles?.messages?.backgroundOperator,
						R.drawable.other_msg_reply_bg
					)
				} else {
					otherImageFrame.setBackgroundDrawable(
						IQStyles.iqChannelsStyles?.messages?.backgroundOperator,
						R.drawable.other_msg_bg
					)
				}

				otherImageFrame.layoutParams.width = size[0]
				otherImageFrame.layoutParams.height = size[1]
				otherImageFrame.requestLayout()
				val radius = UiUtils.toPx(12).toFloat()
				Glide.with(otherImageFrame.context)
					.load(imageUrl)
					.transform(
						CenterCrop(),
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
				val showImgFlags = message.Text == null || message?.Text == ""
				if (showImgFlags) {
					otherImgDate.text = message.Date?.let { timeFormat.format(it) } ?: ""
					otherImgDate.visibility = if (message.Date != null) View.VISIBLE else View.GONE
					otherImgReceived.visibility = if (message.Received) View.VISIBLE else View.GONE
					otherImgRead.visibility = if (message.Read) View.VISIBLE else View.GONE
					val isRead = message.Read
					otherImgRead.isVisible = isRead
					otherImgReceived.isVisible = !isRead && message.Received == true
				}
				otherImgFlags.isVisible = showImgFlags
			} else {
				otherImageFrame.visibility = View.GONE
				clTexts.visibility = View.VISIBLE

				clTexts.setBackgroundDrawable(
					IQStyles.iqChannelsStyles?.messages?.backgroundOperator,
					R.drawable.other_msg_bg
				)

				tvOtherFileName.visibility = View.VISIBLE
				ivFile.visibility = View.VISIBLE
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
					message.Text?.let {
						markwon.setMarkdown(otherText, it)
					}
				}

//                holder.otherText.setText(makeFileLink(file));
			}
		}
	}

	private fun ItemOtherMessageBinding.showFileStateMsg(
		@StringRes strRes: Int,
		@ColorRes colorRes: Int,
		text: Text?
	) {
		otherImageFrame.visibility = View.GONE
		clTexts.visibility = View.VISIBLE
		otherText.visibility = View.VISIBLE
		otherText.text = root.context.getString(strRes)
		otherText.setTextColor(
			ContextCompat.getColor(root.context, colorRes)
		)
		otherText.applyIQStyles(text)
	}

}