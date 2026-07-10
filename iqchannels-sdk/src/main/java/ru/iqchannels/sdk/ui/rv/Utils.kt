package ru.iqchannels.sdk.ui.rv

import android.content.res.Resources
import ru.iqchannels.sdk.schema.UploadedFile
import ru.iqchannels.sdk.ui.UiUtils

object Utils {

	fun computeImageSizeFromFile(file: UploadedFile?, rootViewDimens: Pair<Int, Int>, isMy: Boolean): IntArray {
		if (file == null) {
			return intArrayOf(0, 0)
		}
		val imageWidth = file.ImageWidth ?: 0
		val imageHeight = file.ImageHeight ?: 0

		return computeImageSize(imageWidth, imageHeight, rootViewDimens, isMy)
	}

	private fun computeImageSize(imageWidth: Int, imageHeight: Int, rootViewDimens: Pair<Int, Int>, isMy: Boolean): IntArray {
		if (imageWidth == 0 || imageHeight == 0) {
			return intArrayOf(0, 0)
		}
		var width = 300
		val screenWidth = Resources.getSystem().displayMetrics.widthPixels

		if (isMy) {
			width = screenWidth - UiUtils.toPx(96)
		}else{
			width = screenWidth - UiUtils.toPx(104)
		}

		var height = (imageHeight * width) / imageWidth
		if (height > (width * 2)) {
			height = width * 2
		}
		return intArrayOf(width, height)
	}
}