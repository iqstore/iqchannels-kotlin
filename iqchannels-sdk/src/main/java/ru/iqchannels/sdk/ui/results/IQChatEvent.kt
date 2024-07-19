package ru.iqchannels.sdk.ui.results

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class IQChatEvent : Parcelable {

	/**
	 * Sent when an error occurs while loading messages
	 */
	data class MessagesLoadException(val e: Exception) : IQChatEvent()
	data class MessagesLoadException2(val e: Exception) : IQChatEvent()
}