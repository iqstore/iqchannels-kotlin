/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.schema

class ClientSignupRequest {
	var Name: String? = null
	var Channel: String? = null

	constructor()
	constructor(name: String?, channel: String?) {
		Name = name
		Channel = channel
	}
}
