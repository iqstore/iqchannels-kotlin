package ru.iqchannels.sdk.schema

class ClientIntegrationAuthRequest {
	var Credentials: String? = null
	var Channel: String? = null

	constructor()
	constructor(credentials: String?, channel: String?) {
		Credentials = credentials
		Channel = channel
	}
}
