package ru.iqchannels.sdk.styling

import com.google.gson.annotations.SerializedName

enum class Theme {
	@SerializedName("dark")
    DARK,
	@SerializedName("light")
	LIGHT,
	@SerializedName("system")
	SYSTEM
}