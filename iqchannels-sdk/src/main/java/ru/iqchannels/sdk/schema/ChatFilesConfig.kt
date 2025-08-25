package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName


data class ChatFilesConfigResponse(
	@SerializedName("OK")
	val ok: Boolean?,
	@SerializedName("Error")
	val error: String?,
	@SerializedName("Result")
	val result: ChatFilesConfig?
)

data class ChatFilesConfig(
	@SerializedName("title_label")
	val titleLabel: String?,
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