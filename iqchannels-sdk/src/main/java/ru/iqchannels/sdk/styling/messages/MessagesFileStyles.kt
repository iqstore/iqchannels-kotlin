package ru.iqchannels.sdk.styling.messages

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Text

class MessagesFileStyles(
	@SerializedName("icon_file_client")
	val iconFileClient: String?,
	@SerializedName("icon_file_operator")
	val iconFileOperator: String?,
	@SerializedName("text_filename_client")
	val textFilenameClient: Text?,
	@SerializedName("text_filename_operator")
	val textFilenameOperator: Text?,
	@SerializedName("text_file_size_client")
	val textFileSizeClient: Text?,
	@SerializedName("text_file_size_operator")
	val textFileSizeOperator: Text?
)