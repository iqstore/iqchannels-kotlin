package ru.iqchannels.sdk

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object IQLogFileManager {

	private const val LOG_DIR = "iq_logs"
	private const val RETENTION_DAYS = 7

	private lateinit var context: Context

	private val fileDateFormat = SimpleDateFormat(
		"yyyy-MM-dd",
		Locale.US
	)

	private val logDateFormat = SimpleDateFormat(
		"yyyy-MM-dd HH:mm:ss.SSS",
		Locale.US
	)

	fun init(context: Context) {
		this.context = context.applicationContext

		getLogsDir().mkdirs()
		cleanupOldLogs()
	}

	fun append(level: String, tag: String, message: String) {
		if (!::context.isInitialized) return

		try {
			val file = getTodayLogFile()

			val line = buildString {
				append("[")
				append(logDateFormat.format(Date()))
				append("] [")
				append(level)
				append("] [")
				append(tag)
				append("] ")
				append(message)
				append("\n")
			}

			file.appendText(line)
		} catch (_: Throwable) {
		}
	}

	fun getTodayLogFile(): File {
		val fileName = "iqchannels_${fileDateFormat.format(Date())}.txt"
		return File(getLogsDir(), fileName)
	}

	fun getAllLogFiles(): List<File> {
		return getLogsDir()
			.listFiles()
			?.filter { it.isFile }
			?.sortedByDescending { it.lastModified() }
			?: emptyList()
	}

	fun clearAllLogs() {
		getLogsDir().listFiles()?.forEach {
			it.delete()
		}
	}

	private fun getLogsDir(): File {
//		return File(context.filesDir, LOG_DIR)
		return File(context.getExternalFilesDir(null), LOG_DIR)
	}

	private fun cleanupOldLogs() {
		val now = System.currentTimeMillis()
		val maxAge = RETENTION_DAYS * 24 * 60 * 60 * 1000L

		getLogsDir().listFiles()?.forEach { file ->
			val age = now - file.lastModified()

			if (age > maxAge) {
				file.delete()
			}
		}
	}
}