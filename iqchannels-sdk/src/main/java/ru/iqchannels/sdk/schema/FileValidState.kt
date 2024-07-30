package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

enum class FileValidState {
	@SerializedName("approved")
	Approved,
	@SerializedName("rejected")
	Rejected,
	@SerializedName("on_checking")
	OnChecking,
	@SerializedName("sent_for_checking")
	SentForChecking,
	@SerializedName("check_error")
	CheckError
}