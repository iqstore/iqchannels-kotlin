package ru.iqchannels.sdk.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi

object FileDownloader {

	@RequiresApi(Build.VERSION_CODES.Q)
	fun saveFileToDownloads(context: Context?, fileName: String, fileContent: ByteArray) {
		val contentResolver = context?.contentResolver
		val contentValues = ContentValues().apply {
			put(MediaStore.Downloads.DISPLAY_NAME, fileName)
			put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
		}

		val uri = contentResolver?.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
		uri?.let {
			contentResolver.openOutputStream(it)?.use { outputStream ->
				outputStream.write(fileContent)
			}
		}
	}
}