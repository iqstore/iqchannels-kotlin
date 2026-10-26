package ru.iqchannels.sdk.domain.models

import com.google.gson.annotations.SerializedName

enum class ChatType {
	@SerializedName("regular")
	REGULAR,
	@SerializedName("personal_manager")
	PERSONAL_MANAGER,
	@SerializedName("info")
	INFO
}