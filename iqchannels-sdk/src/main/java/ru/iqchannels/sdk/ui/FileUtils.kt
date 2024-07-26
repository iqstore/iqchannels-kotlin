package ru.iqchannels.sdk.ui

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

object FileUtils {

	fun createGalleryTempFile(context: Context?, uri: Uri, ext: String?, dir: File? = null): File {
		var ext = ext
		var filename = getGalleryFilename(uri)
		if (filename != null) {
			val i = filename.lastIndexOf(".")
			if (i > -1) {
				ext = filename.substring(i + 1)
				filename = filename.substring(0, i - 1)
			}
		} else {
			filename = "file"
			val mimeType = context?.contentResolver?.getType(uri)
			ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
		}

		if (filename.length < 3) {
			filename = "file-$filename"
		}
		val file = File.createTempFile(filename, ".$ext", dir ?: context?.cacheDir)
		file.deleteOnExit()
		return file
	}

	fun getGalleryFilename(uri: Uri): String? {
		var path = uri.path
		val i = path?.lastIndexOf("/")
		if (i != null) {
			if (i > -1) {
				path = path?.substring(i + 1)
			}
		}

		return path
	}
}