package ru.iqchannels.sdk.schema

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class ChatSettings {
    var Id: Long = 0
    var Message: String = "Здравствуйте!"
    var Enabled: Boolean = true
    var GreetFrom: String = "user"
    var Lifetime: Int = 300
    var Pseudonym: String = "Оператор"
    var AvatarId: String = ""
    var TotalOpenedTickets: Int = 0
    var ChatTitle: String? = null

    @SerializedName(value = "chat_subtitle", alternate = ["ChatSubtitle"])
    var ChatSubtitle: String? = null

    override fun toString(): String {
        return Gson().toJson(this)
    }
}