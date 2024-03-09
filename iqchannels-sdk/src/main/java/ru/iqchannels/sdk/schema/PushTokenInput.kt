package ru.iqchannels.sdk.schema

class PushTokenInput {
	var Token: String? = null

	constructor()
	constructor(token: String?) {
		Token = token
	}
}
