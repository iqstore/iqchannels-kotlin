package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

enum class ActorType {
	@SerializedName("")
	ANONYMOUS,
	@SerializedName("client")
	CLIENT,
	@SerializedName("user")
	USER,
	@SerializedName("system")
	SYSTEM
}
