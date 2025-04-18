package ru.iqchannels.sdk.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import ru.iqchannels.sdk.Log
import java.io.File

object FileUtils {

	fun createGalleryTempFile(context: Context?, uri: Uri, ext: String?, dir: File? = null): File {
		val filename = getFileNameFromUri(context, uri)
		val file = File((dir ?: context?.cacheDir), "$filename")
		file.createNewFile()
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