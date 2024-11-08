package ru.iqchannels.sdk.styling.messages

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.Text

class MessagesStyles(
	@SerializedName("background_operator")
	val backgroundOperator: Color?,
	@SerializedName("background_client")
	val backgroundClient: Color?,
	@SerializedName("system_text")
	val systemText: Text?,
	@SerializedName("text_operator")
	val textOperator: Text?,
	@SerializedName("text_client")
	val textClient: Text?,
	@SerializedName("reply_text_client")
	val replyTextClient: Text?,
	@SerializedName("reply_sender_text_client")
	val replySenderTextClient: Text?,
	@SerializedName("reply_text_operator")
	val replyTextOperator: Text?,
	@SerializedName("reply_sender_text_operator")
	val replySenderTextOperator: Text?,
	@SerializedName("text_time")
	val textTime: Text?,
	@SerializedName("text_up")
	val textUp: Text?,
	@SerializedName("text_file_state_rejected_client")
	val textFileStateRejectedClient: Text?,
	@SerializedName("text_file_state_on_checking_client")
	val textFileStateOnCheckingClient: Text?,
	@SerializedName("text_file_state_sent_for_checking_client")
	val textFileStateSentForCheckingClient: Text?,
	@SerializedName("text_file_state_check_error_client")
	val textFileStateCheckErrorClient: Text?,
	@SerializedName("text_file_state_rejected_operator")
	val textFileStateRejectedOperator: Text?,
	@SerializedName("text_file_state_on_checking_operator")
	val textFileStateOnCheckingOperator: Text?,
	@SerializedName("text_file_state_sent_for_checking_operator")
	val textFileStateSentForCheckingOperator: Text?,
	@SerializedName("text_file_state_check_error_operator")
	val textFileStateCheckErrorOperator: Text?
)