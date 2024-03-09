/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.ui

internal object Colors {

	private const val BLUE_GREY_400 = -0x876f64

	private val COLORS = intArrayOf(
		-0x10acb0,  // red-400
		-0x13bf86,  // pink-400
		-0x54b844,  // purple-400
		-0x81a83e,  // deep-purple-400
		-0xa39440,  // indigo-400
		-0xbd5a0b,  // blue-400
		-0xd6490a,  // light-blue-400
		-0xd93926,  // cyan-400
		-0xd95966,  // teal-400
		-0x994496,  // green-400
		-0x63339b,  // light-green-400
		-0x2b1ea9,  // lime-400
		-0x35d8,  // amber-400
		-0x58da,  // orange-400
		-0x8fbd
	)

	fun paletteColor(letter: String?): Int {
		if (letter.isNullOrEmpty()) {
			return BLUE_GREY_400
		}
		val ch = letter[0]
		return COLORS[ch.code % COLORS.size]
	}

	fun linkColor(): Int {
		return -0x81a83e
	}

	fun textColor(): Int {
		return -0xb3b3b4
	}
}
