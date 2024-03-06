/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.app

interface Callback<T> {
	fun onResult(result: T)
	fun onException(e: Exception)
}
