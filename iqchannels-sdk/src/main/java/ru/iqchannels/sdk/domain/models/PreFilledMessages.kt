package ru.iqchannels.sdk.domain.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PreFilledMessages(
	val textMsg: List<String>? = null,
	val fileMsg: List<Uri>? = null
) : Parcelable