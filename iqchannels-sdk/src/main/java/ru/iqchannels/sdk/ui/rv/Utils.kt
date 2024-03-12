package ru.iqchannels.sdk.ui.rv

import ru.iqchannels.sdk.schema.UploadedFile

object Utils {

	fun computeImageSizeFromFile(file: UploadedFile?, rootViewDimens: Pair<Int, Int>): IntArray {
		if (file == null) {
			return intArrayOf(0, 0)
		}
		val imageWidth = file.ImageWidth ?: 0
		val imageHeight = file.ImageHeight ?: 0

		return computeImageSize(imageWidth, imageHeight, rootViewDimens)
	}

	private fun computeImageSize(imageWidth: Int, imageHeight: Int, rootViewDimens: Pair<Int, Int>): IntArray {
		if (imageWidth == 0 || imageHeight == 0) {
			return intArrayOf(0, 0)
		}
		val width = (Math.min(rootViewDimens.first, rootViewDimens.second) * 3) / 5
		var height = (imageHeight * width) / imageWidth
		if (height > (width * 2)) {
			height = width * 2
		}
		return intArrayOf(width, height)
	}
}