/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.sdk.lib

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object InternalIO {
	@Throws(IOException::class)
	fun copy(src: File?, dst: File?) {
		val `in`: InputStream = FileInputStream(src)
		try {
			val out: OutputStream = FileOutputStream(dst)
			try {
				copy(`in`, out)
			} finally {
				out.close()
			}
		} finally {
			`in`.close()
		}
	}

	@JvmOverloads
	@Throws(IOException::class)
	fun copy(
		src: ByteArray,
		dst: OutputStream,
		callback: ProgressCallback? = null
	) {
		val input = ByteArrayInputStream(src)
		var n: Int
		var total = 0
		var progress = 0
		val buf = ByteArray(16 * 1024)
		while (input.read(buf).also { n = it } > -1) {
			dst.write(buf, 0, n)
			dst.flush()
			total += n
			val newProgress = total * 100 / src.size
			if (newProgress != progress) {
				progress = newProgress
				callback?.onProgress(progress)
			}
		}
	}

	@Throws(IOException::class)
	fun copy(src: InputStream, dst: OutputStream) {
		var n: Int
		val buf = ByteArray(16 * 1024)
		while (src.read(buf).also { n = it } > -1) {
			dst.write(buf, 0, n)
			dst.flush()
		}
	}

	interface ProgressCallback {
		fun onProgress(progress: Int)
	}
}
