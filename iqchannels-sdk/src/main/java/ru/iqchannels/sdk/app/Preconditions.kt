/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.app

object Preconditions {
	fun <T> checkNotNull(v: T): T {
		return checkNotNull<T>(v, "null value")
	}

	fun <T> checkNotNull(v: T?, message: String?): T {
		if (v == null) {
			throw NullPointerException(message ?: "null value")
		}
		return v
	}
}
