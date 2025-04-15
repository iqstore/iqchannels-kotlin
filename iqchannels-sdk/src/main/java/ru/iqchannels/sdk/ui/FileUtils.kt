package ru.iqchannels.sdk.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import ru.iqchannels.sdk.Log
import java.io.File

object FileUtils {

	fun createGalleryTempFile(context: Context?, uri: Uri, ext: String?, dir: File? = null): File {
		var ext = ext
		var filename = getFileNameFromUri(context, uri)

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

		try {
			context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
				file.outputStream().use { outputStream ->
					inputStream.copyTo(outputStream)
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			Log.e("createGalleryTempFile", "Failed to copy content from URI to temp file $e")
		}

		return file
	}

	fun getFileNameFromUri(context: Context?, uri: Uri): String? {
		val returnCursor = context?.contentResolver?.query(uri, null, null, null, null)
		returnCursor?.use { cursor ->
			val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
			if (cursor.moveToFirst()) {
				return cursor.getString(nameIndex)
			}
		}
		return null
	}
}