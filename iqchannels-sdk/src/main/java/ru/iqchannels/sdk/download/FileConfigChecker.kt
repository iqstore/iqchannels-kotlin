package ru.iqchannels.sdk.download

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.app.IQChannelsConfigRepository
import ru.iqchannels.sdk.ui.FileUtils

internal object FileConfigChecker {

	fun checkFiles(context: Context, files: List<Uri>): List<Uri> {
		if (IQChannelsConfigRepository.chatFilesConfig == null) return files

		val tempDir = context.tempDir() ?: return files
		val res = mutableListOf<Uri>()

		files.forEach { uri ->
			val resolver = context.contentResolver
			val mimeTypeMap = MimeTypeMap.getSingleton()
			val mtype = resolver.getType(uri)
			val ext = mimeTypeMap.getExtensionFromMimeType(mtype)
			val file = FileUtils.createGalleryTempFile(context, uri, ext, tempDir)
			checkFile(file, ext)?.let {
				res.add(uri)
			}
		}

		return res
	}

	fun checkFile(file: File, ext: String?): File? {
		val configs = IQChannelsConfigRepository.chatFilesConfig ?: return file

		// check file size
		val fileSizeMb = file.length() / 1024 / 1024
		if (configs.maxFileSizeMb != null && configs.maxFileSizeMb.toLong() < fileSizeMb) return null

		// check image
		if (configs.maxImageHeight != null || configs.maxImageWidth != null) {
			try {
				val bitmap = BitmapFactory.decodeFile(file.absolutePath)

				if (configs.maxImageWidth != null && configs.maxImageWidth < bitmap.width) return null
				if (configs.maxImageHeight != null && configs.maxImageHeight < bitmap.height) return null
			} catch (e: Exception) {
				Log.d("FileConfigChecker", "error on reading bitmap: ${e.message}")
			}
		}

		// check extensions
		ext?.let {
			val allowedExtensions = configs.allowedExtensions
			if (!allowedExtensions.isNullOrEmpty()) {
				if (!allowedExtensions.contains(ext)) {
					return null
				}
			}

			val forbiddenExtensions = configs.forbiddenExtensions
			if (!forbiddenExtensions.isNullOrEmpty()) {
				if (forbiddenExtensions.contains(ext)) {
					return null
				}
			}
		}

		return file
	}

	fun Context.tempDir(): File? {
		val dir = File(cacheDir, "common/temp")
		return when {
			dir.isDirectory || dir.mkdirs() -> dir

			else -> {
				Log.e("FileConfigChecker", "Failed to get directory for path: ${dir.path}")
				null
			}
		}
	}
}