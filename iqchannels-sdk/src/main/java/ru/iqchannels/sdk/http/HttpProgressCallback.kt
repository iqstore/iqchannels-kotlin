/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.http

interface HttpProgressCallback {
	fun onProgress(progress: Int)
}
