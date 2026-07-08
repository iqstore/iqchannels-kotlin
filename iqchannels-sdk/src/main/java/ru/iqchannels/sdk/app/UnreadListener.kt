/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.app

import com.google.gson.Gson

interface UnreadListener {
	fun unreadChanged(unread: Int)
}

interface AdvancedUnreadListener {
	fun advancedUnreadChanged(unread: AdvancedUnread?)
	fun advancedUnreadException(e: Exception)
}


class AdvancedUnread {
	var channels: List<Channel>? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}

class Channel {
	var name: String? = null
	var chatType: String? = null
	var lastMessage: LastMessage? = null
	var unreadCount: Int? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}

class LastMessage {
	var text: String? = null
	var fileId: String? = null
	var isSurvey: Boolean? = null
	var date: String? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}

class AdvancedUnreadResult {
	var id: Long? = null
	var type: String? = null
	var name: String? = null
	var lastMessage: LastMessage? = null
	var unreadCount: Int? = null

	override fun toString(): String {
		return Gson().toJson(this)
	}
}