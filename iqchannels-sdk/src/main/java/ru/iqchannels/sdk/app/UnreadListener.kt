/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.app

interface UnreadListener {
	fun unreadChanged(unread: Int)
}
