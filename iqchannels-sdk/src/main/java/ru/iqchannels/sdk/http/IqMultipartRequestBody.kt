package ru.iqchannels.sdk.http

import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.Charset
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Buffer
import okio.ForwardingSink
import okio.buffer

class IqMultipartRequestBody(
	private val boundary: String,
	private val params: Map<String, String>,
	private val files: Map<String, HttpFile>,
	private val progressCallback: HttpProgressCallback?
) : RequestBody() {

	companion object {
		private val UTF8 = Charset.forName("UTF-8")
	}

	private val fileLengths: Map<String, Long> =
		files.mapValues { (_, httpFile) -> httpFile.file.length() }

	override fun contentType(): MediaType {
		return "multipart/form-data;boundary=$boundary".toMediaType()
	}

	override fun contentLength(): Long {
		var length = 0L
		for ((key, value) in params) {
			length += "--$boundary\r\n".toByteArray(UTF8).size
			length += "Content-Disposition: form-data; name=\"$key\"\r\n\r\n".toByteArray(UTF8).size
			length += value?.toByteArray(UTF8)?.size ?: 0
			length += "\r\n".toByteArray(UTF8).size
		}
		for ((key, httpFile) in files) {
			length += "--$boundary\r\n".toByteArray(UTF8).size
			length += "Content-Disposition: form-data; name=\"$key\"; filename=\"${httpFile.file.name}\"\r\n".toByteArray(UTF8).size
			length += "Content-Type: ${httpFile.mimeType}\r\n\r\n".toByteArray(UTF8).size
			length += fileLengths[key] ?: 0L
			length += "\r\n".toByteArray(UTF8).size
		}
		length += "--$boundary--\r\n".toByteArray(UTF8).size
		return length
	}

	override fun writeTo(sink: BufferedSink) {
		val total = contentLength()

		for ((key, httpFile) in files) {
			val expected = fileLengths[key] ?: 0L
			val actual = httpFile.file.length()
			if (actual != expected) {
				throw IOException(
					"File ${httpFile.file.name} changed before upload: " +
						"expected $expected bytes, actual $actual"
				)
			}
		}

		var sent = 0L
		val progressSink = object : ForwardingSink(sink) {
			override fun write(source: Buffer, byteCount: Long) {
				super.write(source, byteCount)
				sent += byteCount
				if (total > 0) {
					progressCallback?.onProgress(((sent * 100) / total).toInt())
				}
			}
		}
		val out = progressSink.buffer()

		for ((key, value) in params) {
			out.writeUtf8("--$boundary\r\n")
			out.writeUtf8("Content-Disposition: form-data; name=\"$key\"\r\n\r\n")
			if (value != null) {
				out.writeUtf8(value)
			}
			out.writeUtf8("\r\n")
		}

		for ((key, httpFile) in files) {
			out.writeUtf8("--$boundary\r\n")
			out.writeUtf8("Content-Disposition: form-data; name=\"$key\"; filename=\"${httpFile.file.name}\"\r\n")
			out.writeUtf8("Content-Type: ${httpFile.mimeType}\r\n\r\n")
			FileInputStream(httpFile.file).use { input ->
				val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
				var read: Int
				while (input.read(buffer).also { read = it } != -1) {
					out.write(buffer, 0, read)
				}
			}
			out.writeUtf8("\r\n")
		}

		out.writeUtf8("--$boundary--\r\n")

		out.flush()

		if (sent != total) {
			throw IOException(
				"Multipart body length mismatch: declared $total bytes, wrote $sent"
			)
		}
	}
}
