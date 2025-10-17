package ru.iqchannels.sdk.download

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.fragment.app.FragmentManager
import java.io.File
import ru.iqchannels.sdk.IQLog
import ru.iqchannels.sdk.app.IQChannelsConfigRepository
import ru.iqchannels.sdk.localization.IQChannelsLanguage
import ru.iqchannels.sdk.ui.FileUtils
import ru.iqchannels.sdk.ui.backdrop.ErrorPageBackdropDialog


internal object FileConfigChecker {

	fun checkFiles(context: Context, files: List<Uri>, childFragmentManager: FragmentManager): List<Uri> {
		if (IQChannelsConfigRepository.chatFilesConfig == null) return files

		val tempDir = context.tempDir() ?: return files
		val res = mutableListOf<Uri>()

		files.forEach { uri ->
			val filenameFromUri = getFilenameFromUri(context, uri)
			val ext = filenameFromUri.substring(filenameFromUri.lastIndexOf(".") + 1).lowercase()
			val file = FileUtils.createGalleryTempFile(context, uri, ext, tempDir)
			checkFile(context, file, ext, childFragmentManager)?.let {
				res.add(uri)
			}
		}

		return res
	}

	fun checkFile(context: Context, file: File, ext: String?, childFragmentManager: FragmentManager): File? {
		val configs = IQChannelsConfigRepository.chatFilesConfig ?: return file

		// check file size
		val fileSizeMb = file.length() / 1024 / 1024

		if (configs.maxFileSizeMb != null && configs.maxFileSizeMb < fileSizeMb) {
			IQLog.d("FileConfigChecker", "notAllowedFileSize: $fileSizeMb")

			val backdrop = ErrorPageBackdropDialog.newInstance("${IQChannelsLanguage.iqChannelsLanguage.fileWeightError} ${configs.maxFileSizeMb}Mb")
			backdrop.show(childFragmentManager, ErrorPageBackdropDialog.TRANSACTION_TAG)
			return null
		}

		// check image
		if (configs.maxImageHeight != null || configs.maxImageWidth != null) {
			try {
				val options = BitmapFactory.Options()
				options.inJustDecodeBounds = true
				BitmapFactory.decodeFile(file.path, options)

				val bitmap = BitmapFactory.decodeFile(file.absolutePath)
				val allowedWidth = configs.maxImageWidth != null && configs.maxImageWidth > bitmap.width
				val allowedHeight = configs.maxImageHeight != null && configs.maxImageHeight > bitmap.height


				if (!allowedWidth || !allowedHeight) {
					IQLog.d("FileConfigChecker", "notAllowedImageSize: ${bitmap.width}  ${bitmap.height}")

					val backdrop = ErrorPageBackdropDialog.newInstance(IQChannelsLanguage.iqChannelsLanguage.fileSizeError)
					backdrop.show(childFragmentManager, ErrorPageBackdropDialog.TRANSACTION_TAG)
					return null
				}
			} catch (e: Exception) {
				IQLog.d("FileConfigChecker", "error on reading bitmap: ${e.message}")
			}
		}

		// check extensions
		ext?.let {
			val allowedExtensions = configs.allowedExtensions
			if (!allowedExtensions.isNullOrEmpty()) {
				if (!allowedExtensions.contains(ext)) {
					IQLog.d("FileConfigChecker", "notAllowedExtension: $ext")

					val backdrop = ErrorPageBackdropDialog.newInstance(IQChannelsLanguage.iqChannelsLanguage.fileNotAllowed)
					backdrop.show(childFragmentManager, ErrorPageBackdropDialog.TRANSACTION_TAG)
					return null
				}
			}

			val forbiddenExtensions = configs.forbiddenExtensions
			if (!forbiddenExtensions.isNullOrEmpty()) {
				if (forbiddenExtensions.contains(ext)) {
					IQLog.d("FileConfigChecker", "forbiddenExtension: $ext")

					val backdrop = ErrorPageBackdropDialog.newInstance(IQChannelsLanguage.iqChannelsLanguage.fileForbidden)
					backdrop.show(childFragmentManager, ErrorPageBackdropDialog.TRANSACTION_TAG)
					return null
				}
			}
		}

		return file
	}

	fun getFilenameFromUri(context: Context, uri: Uri): String {
		var filename = ""
		if (uri.scheme == "content") {
			val cursor = context.contentResolver.query(uri, null, null, null, null)
			cursor?.use {
				if (it.moveToFirst()) {
					val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
					if (nameIndex != -1) {
						filename = it.getString(nameIndex)
					}
				}
			}
		} else if (uri.scheme == "file") {
			filename = File(uri.path!!).name
		}
		return filename
	}

	fun Context.tempDir(): File? {
		val dir = File(cacheDir, "common/temp")
		return when {
			dir.isDirectory || dir.mkdirs() -> dir

			else -> {
				IQLog.e("FileConfigChecker", "Failed to get directory for path: ${dir.path}")
				null
			}
		}
	}
}