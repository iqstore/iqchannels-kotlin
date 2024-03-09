package ru.iqchannels.sdk.schema

class UploadedFile {
	var Id: String? = null
	var Type: FileType? = null
	var Owner: FileOwnerType? = null
	var OwnerClientId: Long? = null
	var Actor: ActorType? = null
	var ActorClientId: Long? = null
	var ActorUserId: Long? = null
	var Name: String? = null // Original file name.
	var Path: String? = null // Relative filesystem path;
	var Size: Long = 0 // Size in bytes.
	var ImageWidth: Int? = null
	var ImageHeight: Int? = null
	var ContentType: String? = null
	var CreatedAt: Long = 0

	// Local
	var Url: String? = null
	var ImagePreviewUrl: String? = null
	var imageUrl: String? = null
}
