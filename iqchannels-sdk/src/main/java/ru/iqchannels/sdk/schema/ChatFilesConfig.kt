package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

data class ChatFilesConfig(
	@SerializedName("MaxFileSizeMb")
	val maxFileSizeMb: Int?,
	@SerializedName("MaxImageHeight")
	val maxImageHeight: Int?,
	@SerializedName("MaxImageWidth")
	val maxImageWidth: Int?,
	@SerializedName("AllowedExtensions")
	val allowedExtensions: List<String>?,
	@SerializedName("ForbiddenExtensions")
	val forbiddenExtensions: List<String>?
)