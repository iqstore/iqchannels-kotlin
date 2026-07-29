package ru.iqchannels.sdk.schema

import com.google.gson.Gson

class Client {
	var Id: Long = 0
	var Name: String? = null
	var IntegrationId: String? = null
	var CreatedAt: Long = 0
	var UpdatedAt: Long = 0
	var PersonalManagerId: Long = 0
	var PersonalManagerGroupId: Long = 0
	var MultiChatsInfo: MultiChatsInfo? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}
