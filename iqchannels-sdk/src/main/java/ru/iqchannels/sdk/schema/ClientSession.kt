package ru.iqchannels.sdk.schema

class ClientSession {
	var Id: Long = 0
	var ClientId: Long = 0
	var Token: String? = null
	var Integration = false
	var IntegrationHash: String? = null
	var IntegrationCredentials: String? = null
	var CreatedAt: Long = 0
}
