package ru.iqchannels.sdk.schema

import com.google.gson.Gson
import org.json.JSONObject

class TransferToChannel {
	val Id: Long = 0
	val ProjectId: Long = 0
	val Name: String? = null
	val Title: String? = null
	val Description: String? = null
	var CreatedAt: Long? = null
	var UpdatedAt: Long? = null
	val ProjectName: String? = null
	val ChatTitle: String? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}
