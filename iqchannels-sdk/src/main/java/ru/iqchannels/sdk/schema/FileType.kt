package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

enum class FileType {
	@SerializedName("")
	INVALID,
	@SerializedName("file")
	FILE,
	@SerializedName("image")
	IMAGE
}
