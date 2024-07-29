package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

enum class FileValidState {
	@SerializedName("approved")
	FileStateApproved,
	@SerializedName("rejected")
	FileStateRejected,
	@SerializedName("on_checking")
	FileStateOnChecking,
	@SerializedName("sent_for_checking")
	FileStateSentForChecking,
	@SerializedName("check_error")
	FileStateCheckError
}