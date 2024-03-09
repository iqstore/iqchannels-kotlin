package ru.iqchannels.sdk.schema

class RatingInput {
	var Value = 0
	var Comment: String? = null

	constructor()
	constructor(value: Int) {
		Value = value
	}
}
