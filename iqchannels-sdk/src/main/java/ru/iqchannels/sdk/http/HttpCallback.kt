/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.http

interface HttpCallback<T> {
	fun onResult(result: T?)
	fun onException(exception: Exception)
}
