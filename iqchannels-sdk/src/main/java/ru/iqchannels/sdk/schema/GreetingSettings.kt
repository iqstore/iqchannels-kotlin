package ru.iqchannels.sdk.schema

import com.google.gson.Gson

class GreetingSettings {
	var Greeting: String? = null
	var GreetingBold: String? = null
	var PersonalDataRequestType: DataRequestType = DataRequestType.default

	override fun toString(): String {
		return Gson().toJson(this)
	}
}

enum class DataRequestType {
	default,
	full_form,
	none
}