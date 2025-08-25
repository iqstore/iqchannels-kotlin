package ru.iqchannels.sdk.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import ru.iqchannels.sdk.Log
import java.io.File
import java.io.FileOutputStream

object FileUtils {

	fun createGalleryTempFile(context: Context, uri: Uri, ext: String?, dir: File? = null): File {
		val filename = getFileNameFromUri(context, uri) ?: "temp_${System.currentTimeMillis()}.$ext"
		val file = File((dir ?: context.cacheDir), filename)

		context.contentResolver.openInputStream(uri)?.use { input ->
			FileOutputStream(file).use { output ->
				input.copyTo(output)
			}
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