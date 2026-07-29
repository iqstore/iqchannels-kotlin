package ru.iqchannels.sdk.schema

import com.google.gson.Gson

class ClientAuth {
	var Client: Client? = null
	var Session: ClientSession? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}
