package ru.iqchannels.sdk.ui.rv

import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import java.text.DecimalFormat
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.applyIQStyles
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.schema.UploadedFile
import ru.iqchannels.sdk.styling.IQStyles
import ru.iqchannels.sdk.styling.messages.MessagesFileStyles
import ru.iqchannels.sdk.ui.ChatMessagesAdapter

internal object MultiFilesView {

	private const val TAG_ROW = "iq_multi_file_row"

	fun clear(container: LinearLayout) {
		val rows = (0 until container.childCount)
			.map { container.getChildAt(it) }
			.filter { it.tag == TAG_ROW }
		rows.forEach { container.removeView(it) }
	}

	fun render(
		container: LinearLayout,
		message: ChatMessage,
		files: List<UploadedFile>,
		isMy: Boolean,
		itemClickListener: ChatMessagesAdapter.ItemClickListener
	) {
		clear(container)

		val context = container.context
		val density = context.resources.displayMetrics.density
		val pad8 = (8 * density).toInt()
		val iconSize = (32 * density).toInt()

		val messageFileStyles = IQStyles.iqChannelsStyles?.messageFile

		files.forEach { file ->
			val isImage = file.ContentType?.startsWith("image/") == true

			val row = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = android.view.Gravity.CENTER_VERTICAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				)
				setPadding(0, pad8 / 2, 0, pad8 / 2)
			}

			val icon = ImageView(context).apply {
				layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
			}

			loadIcon(icon, file, isImage, isMy, messageFileStyles)

			val texts = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				)
				setPadding(pad8, 0, 0, 0)
			}

			val name = TextView(context).apply {
				text = file.Name ?: file.Id
				textSize = 16f
				maxLines = 2
				maxWidth = (246 * density).toInt()
				setTextColor(
					if (isMy) {
						ContextCompat.getColor(context, R.color.my_text_color)
					} else {
						android.graphics.Color.parseColor("#3F51B5")
					}
				)
				applyIQStyles(
					if (isMy) {
						messageFileStyles?.textFilenameClient
					} else {
						messageFileStyles?.textFilenameOperator
					}
				)
			}

			val size = TextView(context).apply {
				text = formatFileSize(file.Size)
				textSize = 15f
				setTextColor(android.graphics.Color.parseColor("#ff979fa7"))
				applyIQStyles(
					if (isMy) {
						messageFileStyles?.textFileSizeClient
					} else {
						messageFileStyles?.textFileSizeOperator
					}
				)
			}

			texts.addView(name)
			texts.addView(size)

			row.addView(icon)
			row.addView(texts)

			row.tag = TAG_ROW
			row.setOnClickListener {
				val imageUrl = file.imageUrl ?: file.Url
				if (isImage && imageUrl != null) {
					itemClickListener.onImageFileClick(message, file)
				} else {
					itemClickListener.onFileClick(file.Url ?: "", file.Name ?: "")
				}
			}

			container.addView(row)
		}
	}

	private fun loadIcon(
		icon: ImageView,
		file: UploadedFile,
		isImage: Boolean,
		isMy: Boolean,
		messageFileStyles: MessagesFileStyles?
	) {
		val context = icon.context

		if (isImage && file.ImagePreviewUrl != null) {
			Glide.with(context)
				.load(withSessionCookie(file.ImagePreviewUrl))
				.placeholder(R.drawable.placeholder_load_image)
				.into(icon)
			return
		}

		val customIconUrl = if (isMy) {
			messageFileStyles?.iconFileClient
		} else {
			messageFileStyles?.iconFileOperator
		}

		if (customIconUrl != null) {
			Glide.with(context)
				.load(withSessionCookie(customIconUrl))
				.into(icon)
		} else {
			icon.setImageResource(R.drawable.doc_32)
			if (!isMy) {
				icon.imageTintList = android.content.res.ColorStateList.valueOf(
					ContextCompat.getColor(context, R.color.other_file_icon)
				)
			}
		}
	}

	private fun withSessionCookie(url: String?): GlideUrl {
		return GlideUrl(
			url,
			LazyHeaders.Builder()
				.addHeader("Cookie", "client-session=${IQChannels.getCurrentToken()}")
				.build()
		)
	}

	private fun formatFileSize(size: Long): String {
		if (size <= 0) {
			return ""
		}
		val sizeKb = size.toFloat() / 1024
		var sizeMb = 0f
		if (sizeKb > 1024) {
			sizeMb = sizeKb / 1024
		}

		return if (sizeMb > 0) {
			"${DecimalFormat("0.00").format(sizeMb.toDouble())} mb"
		} else {
			"${DecimalFormat("0.00").format(sizeKb.toDouble())} kb"
		}
	}
}
