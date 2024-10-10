package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

enum class PollOptionType {
	@SerializedName("one_of_list")
	ONE_OF_LIST,
	@SerializedName("input")
	INPUT,
	@SerializedName("stars")
	STARS,
	@SerializedName("fcr")
	FCR,
	@SerializedName("scale")
	SCALE,
}