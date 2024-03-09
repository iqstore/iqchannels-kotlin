package ru.iqchannels.sdk.schema

class Response<T> {
	var OK = false
	var Error: ResponseError? = null
	var Result: T? = null
	var Rels: Relations? = null
}
