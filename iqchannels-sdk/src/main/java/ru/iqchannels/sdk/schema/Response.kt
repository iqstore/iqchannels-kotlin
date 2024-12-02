package ru.iqchannels.sdk.schema

import com.google.gson.Gson

class Response<T> {
	var OK = false
	var Error: ResponseError? = null
	var Result: T? = null
	var Data: T? = null
	var Rels: Relations? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}
