package ru.iqchannels.sdk.domain.models

import android.net.Uri

data class PreFilledMessages(
	val textMsg: List<String>? = null,
	val fileMsg: List<Uri>? = null
)