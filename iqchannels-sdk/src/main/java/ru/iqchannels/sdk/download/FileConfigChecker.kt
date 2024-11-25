package ru.iqchannels.sdk.download

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.fragment.app.FragmentManager
import java.io.File
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.app.IQChannelsConfigRepository
import ru.iqchannels.sdk.ui.FileUtils
import ru.iqchannels.sdk.ui.backdrop.ErrorPageBackdropDialog


internal object FileConfigChecker {

	fun checkFiles(context: Context, files: List<Uri>, childFragmentManager: FragmentManager): List<Uri> {
		if (IQChannelsConfigRepository.chatFilesConfig == null) return files

		val tempDir = context.tempDir() ?: return files
		val res = mutableListOf<Uri>()

		files.forEach { uri ->
			val resolver = context.contentResolver
			val mimeTypeMap = MimeTypeMap.getSingleton()
			val mtype = resolver.getType(uri)
			val ext = if (mtype == "image/jpeg") "jpeg" else mimeTypeMap.getExtensionFromMimeType(mtype)
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
			Log.d("FileConfigChecker", "notAllowedFileSize: $fileSizeMb")

			val backdrop = ErrorPageBackdropDialog.newInstance(context.getString(R.string.file_size_too_large))
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
					Log.d("FileConfigChecker", "notAllowedImageSize: ${bitmap.width}  ${bitmap.height}")

					val backdrop = ErrorPageBackdropDialog.newInstance(context.getString(R.string.image_size_too_large))
					backdrop.show(childFragmentManager, ErrorPageBackdropDialog.TRANSACTION_TAG)
					return null
				}
			} catch (e: Exception) {
				Log.d("FileConfigChecker", "error on reading bitmap: ${e.message}")
			}
		}

		// check extensions
		ext?.let {
			val allowedExtensions = configs.allowedExtensions
			if (!allowedExtensions.isNullOrEmpty()) {
				if (!allowedExtensions.contains(ext)) {
					Log.d("FileConfigChecker", "notAllowedExtension: $ext")

					val backdrop = ErrorPageBackdropDialog.newInstance(context.getString(R.string.file_extension_not_allowed))
					backdrop.show(childFragmentManager, ErrorPageBackdropDialog.TRANSACTION_TAG)
					return null
				}
			}

			val forbiddenExtensions = configs.forbiddenExtensions
			if (!forbiddenExtensions.isNullOrEmpty()) {
				if (forbiddenExtensions.contains(ext)) {
					Log.d("FileConfigChecker", "forbiddenExtension: $ext")

					val backdrop = ErrorPageBackdropDialog.newInstance(context.getString(R.string.file_extension_forbidden))
					backdrop.show(childFragmentManager, ErrorPageBackdropDialog.TRANSACTION_TAG)
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