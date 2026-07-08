package ru.iqchannels.sdk.schema

import com.google.gson.Gson

class SingleChoice {
	var Id: Long? = null
	var title: String? = null
	var value: String? = null
	var tag: String? = null
	var ChatMessageId: Long? = null
	var Deleted: Boolean? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}
