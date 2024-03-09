/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.schema

import com.google.gson.annotations.SerializedName

enum class FileImageSize(val sizeName: String) {
	@SerializedName("")
	ORIGINAL(""),
	@SerializedName("avatar")
	AVATAR("avatar"),
	@SerializedName("thumbnail")
	THUMBNAIL("thumbnail"),
	@SerializedName("preview")
	PREVIEW("preview");

	override fun toString(): String {
		return sizeName
	}
}
