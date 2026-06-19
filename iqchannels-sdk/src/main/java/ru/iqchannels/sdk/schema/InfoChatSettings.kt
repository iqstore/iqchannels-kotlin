package ru.iqchannels.sdk.schema

import com.google.gson.Gson

class InfoChatSettings(
    var BlockerText: String? = null,
    var BlockerIcon: String? = null,
    var IsVisibleBlocker: Boolean = false
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}

class InfoChatSettingsResponse {
    var Text: String? = null
    var BlockerFileId: String? = null

    override fun toString(): String {
        return Gson().toJson(this)
    }
}