package ru.iqchannels.sdk.styling.chat

import com.google.gson.annotations.SerializedName
import ru.iqchannels.sdk.styling.ButtonStyles
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.ContainerStyles
import ru.iqchannels.sdk.styling.Text

class SignupStyles(
	val background: Color?,
	val title: Text?,
	val subtitle: Text?,
	@SerializedName("input_background")
	val inputBackground: ContainerStyles?,
	@SerializedName("input_text")
	val inputText: Text?,
	@SerializedName("check_box_disabled")
	val checkBoxDisabled: Color?,
	@SerializedName("check_box_enabled")
	val checkBoxEnabled: Color?,
	@SerializedName("check_box_text")
	val checkBoxText: Text?,
	val button: ButtonStyles?,
	@SerializedName("error_text")
	val errorText: Text?
)