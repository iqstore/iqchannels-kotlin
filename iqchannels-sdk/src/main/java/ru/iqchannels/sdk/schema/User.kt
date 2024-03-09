package ru.iqchannels.sdk.schema

class User {
	var Id: Long = 0
	var Name: String? = null
	var DisplayName: String? = null
	var Email: String? = null
	var Online = false
	var Deleted = false
	var AvatarId: String? = null
	var CreatedAt: Long = 0
	var LoggedInAt: Long? = null
	var LastSeenAt: Long? = null

	// Local
	var AvatarUrl: String? = null
}
