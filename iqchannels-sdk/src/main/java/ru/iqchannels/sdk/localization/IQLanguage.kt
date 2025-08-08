package ru.iqchannels.sdk.localization
import com.google.gson.Gson
import ru.iqchannels.sdk.schema.Language

class IQLanguage {
	var Code: String? = null
	var Name: String? = null
	var Default: Boolean? = null
	var IconURL: String? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}