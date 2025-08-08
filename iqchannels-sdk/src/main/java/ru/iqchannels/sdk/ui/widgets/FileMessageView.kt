package ru.iqchannels.sdk.ui.widgets

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.schema.ChatMessage
import ru.iqchannels.sdk.ui.UiUtils.getRatingScaleMaxValue
import java.io.File
import java.text.DecimalFormat

class FileMessageView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

	val tvFileSize: TextView
	val tvFileName: TextView
	val ibClose: ImageButton
	val imageView: ImageView

	init {
		inflate(context, R.layout.layout_file_to_message, this)
		imageView = findViewById(R.id.iv_file_image)
		tvFileName = findViewById(R.id.tv_file_name)
		tvFileSize = findViewById(R.id.tv_file_size)
		ibClose = findViewById(R.id.ib_file_close)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			tvFileSize.setTypeface(Typeface.create(null, 400, false))
			tvFileName.setTypeface(Typeface.create(null, 400, false))
		}
	}

	fun showSelectedFile(file: File?) {
		visibility = VISIBLE

		if (file != null) {
			imageView.visibility = VISIBLE
			tvFileName.visibility = VISIBLE
			tvFileName.text = file.name

			val size = file.length()

			if (size > 0) {
				tvFileSize.visibility = View.VISIBLE
				val sizeKb = size.toFloat() / 1024
				var sizeMb = 0f
				if (sizeKb > 1024) {
					sizeMb = sizeKb / 1024
				}

				val str: String
				val fileSize: String
				if (sizeMb > 0) {
					str = "mb"
					val df = DecimalFormat("0.00")
					fileSize = df.format(sizeMb.toDouble())
				} else {
					str = "kb"
					fileSize = sizeKb.toString()
				}
				tvFileSize.text = "$fileSize $str"
			} else {
				tvFileSize.text = null
			}

		} else {
			visibility = GONE
		}
	}

	fun setCloseBtnClickListener(listener: OnClickListener?) {
		ibClose.setOnClickListener(listener)
	}

}