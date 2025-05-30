package ru.iqchannels.sdk.schema

import com.google.gson.Gson

class ChatSettings {
    var Id: Long = 0
    var Message: String = "Здравствуйте!"
    var Enabled: Boolean = true
    var GreetFrom: String = "user"
    var Lifetime: Int = 300
    var Pseudonym: String = "Оператор"
    var AvatarId: String = ""
    var TotalOpenedTickets: Int = 0

    override fun toString(): String {
        return Gson().toJson(this)
    }
}