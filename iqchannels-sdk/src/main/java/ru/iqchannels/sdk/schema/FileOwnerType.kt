package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

enum class FileOwnerType {
	@SerializedName("")
	INVALID,
	@SerializedName("public")
	PUBLIC,
	@SerializedName("client")
	CLIENT
}
