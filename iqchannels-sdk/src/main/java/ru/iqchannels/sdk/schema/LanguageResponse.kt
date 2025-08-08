package ru.iqchannels.sdk.schema

import com.google.gson.Gson
import ru.iqchannels.sdk.localization.IQLanguage

class LanguageResponse {
	val Languages: List<IQLanguage>? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}

class LanguageQuery {
	var Code: String? = null
}
