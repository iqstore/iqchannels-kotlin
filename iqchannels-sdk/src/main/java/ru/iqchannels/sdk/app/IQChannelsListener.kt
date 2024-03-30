/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.app

import ru.iqchannels.sdk.schema.ClientAuth

interface IQChannelsListener {
	fun authenticating()
	fun authComplete(auth: ClientAuth)
	fun authFailed(e: Exception, attempt: Int)
}
