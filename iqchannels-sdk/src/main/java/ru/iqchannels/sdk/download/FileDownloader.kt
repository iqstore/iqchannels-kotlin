package ru.iqchannels.sdk.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import ru.iqchannels.sdk.R

object FileDownloader {

	fun downloadFile(context: Context, fileUrl: String?, fileName: String?): Long {
		val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		val uri = Uri.parse(fileUrl)
		val request = DownloadManager.Request(uri)
		request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
			.setAllowedOverRoaming(false)
			.setTitle(fileName)
			.setDescription(context.getString(R.string.downloading))
			.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

		return downloadManager.enqueue(request)
	}
}
