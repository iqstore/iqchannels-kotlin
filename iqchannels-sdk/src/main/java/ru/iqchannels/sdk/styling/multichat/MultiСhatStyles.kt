package ru.iqchannels.sdk.styling.chat

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

class MultiСhatStyles(
	val background: Color?,
	val icon_regular_chat: String?,
	val icon_personal_manager_chat: String?,
	val icon_info_chat: String?,
	@SerializedName("background_icon")
	val backgroundIcon: Color?,
	val title: Text?,
	@SerializedName("last_message")
	val lastMessage: Text?
)