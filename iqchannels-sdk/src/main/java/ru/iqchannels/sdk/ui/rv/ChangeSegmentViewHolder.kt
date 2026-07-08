package ru.iqchannels.sdk.ui.rv

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.applyIQStyles
import ru.iqchannels.sdk.setBackgroundDrawable
import ru.iqchannels.sdk.styling.IQStyles
import ru.iqchannels.sdk.IQLog
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.app.IQChannelsConfig
import ru.iqchannels.sdk.app.IQChannelsConfigRepository
import ru.iqchannels.sdk.app.UIOptions
import ru.iqchannels.sdk.databinding.ChatChangeSegmentBinding
import ru.iqchannels.sdk.domain.models.ChatType
import ru.iqchannels.sdk.http.HttpCallback
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.RatingPoll
import ru.iqchannels.sdk.schema.RatingState
import ru.iqchannels.sdk.ui.ChatFragment

internal class ChangeSegmentViewHolder(
	private val binding: ChatChangeSegmentBinding
) : ViewHolder(binding.root) {

	private var listener: ChangeSegmentListener? = null

	fun bind(message: ChatMessage) {
		binding.button.setOnClickListener {
			onButtonTap(message)
		}

		binding.title.applyIQStyles(IQStyles.iqChannelsStyles?.changeSegment?.title)
		binding.title.text = message.Text

		binding.button.text = "Перейти в канал \"${message.TransferToChannel?.Title}\""

		IQStyles.iqChannelsStyles?.changeSegment?.backgroundContainer
			?.let {
				binding.background.setBackgroundDrawable(it, R.drawable.other_msg_bg)
			}

		IQStyles.iqChannelsStyles?.changeSegment?.backgroundButton
			?.let {
				binding.button.setBackgroundDrawable(it, R.drawable.bg_button_rate)
			}
		binding.button.applyIQStyles(IQStyles.iqChannelsStyles?.changeSegment?.textButton)
	}

	private fun onButtonTap(message: ChatMessage) {
		val messageId = message.Id
		listener?.onChangeSegment(
			messageId,
			object : HttpCallback<Void> {
				override fun onResult(result: Void?) {}
				override fun onException(exception: Exception) {
					IQLog.e("onChangeSegment", "$exception")
				}
			}
		)

		if(message.Client?.PersonalManagerId == 0L){
			IQChannels.chatType = ChatType.REGULAR
		} else {
			IQChannels.chatType = ChatType.PERSONAL_MANAGER
		}
		IQChannels.chatType = ChatType.PERSONAL_MANAGER

		IQChannels.configure(
			binding.root.context,
			IQChannelsConfig(
				IQChannels.config?.address,
				IQChannels.config?.channels,
				message.TransferToChannel?.Name,
				false,
				UIOptions(true))
		)
		IQChannels.login(IQChannels.credentials ?: "")

		ChatFragment.newInstance()
	}

	fun setChangeSegmentListener(listener: ChangeSegmentListener) {
		this.listener = listener
	}
}