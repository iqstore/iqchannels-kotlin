package ru.iqchannels.sdk.ui.results

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class IQChatEvent : Parcelable {

	/**
	 * Sent when an error occurs while loading messages
	 */
	data class MessagesLoadException(val e: Exception) : IQChatEvent()

	/**
	 * Sent when an error occurs while loading messages
	 */
	data class MessagesLoadMoreException(val e: Exception) : IQChatEvent()

	/**
	 * Sent when clicked on top nav bar (toolbar) back button pressed
	 */
	data object NavBarBackButtonPressed : IQChatEvent()

	/**
	 * Sent when clicked on RETURN button on error page"
	 */
	data object ErrorGoBackButtonPressed : IQChatEvent()
}