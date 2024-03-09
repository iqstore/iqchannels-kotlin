/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.app

internal object Retry {
	fun delaySeconds(attempt: Int): Int {
		return when (attempt) {
			0 -> 1
			1 -> 1
			2 -> 2
			3 -> 5
			4 -> 10
			5 -> 15
			6 -> 20
			else -> 30
		}
	}
}