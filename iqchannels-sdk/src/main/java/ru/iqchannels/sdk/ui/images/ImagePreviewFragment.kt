package ru.iqchannels.sdk.ui.images

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Build
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.R

class ImagePreviewFragment : Fragment() {

	companion object {
		private const val ARG_SENDER_NAME = "ImagePreviewFragment#senderName"
		private const val ARG_DATE = "ImagePreviewFragment#date"
		private const val ARG_IMAGE_URL = "ImagePreviewFragment#imageUrl"
		private const val ARG_MESSAGE = "ImagePreviewFragment#message"
		fun newInstance(
			senderName: String,
			date: Date,
			imageUrl: String,
			message: String
		): ImagePreviewFragment {
			val fragment = ImagePreviewFragment()
			val bundle = Bundle()
			bundle.putString(ARG_SENDER_NAME, senderName)
			bundle.putSerializable(ARG_DATE, date)
			bundle.putString(ARG_IMAGE_URL, imageUrl)
			bundle.putString(ARG_MESSAGE, message)
			fragment.arguments = bundle
			return fragment
		}
	}

	private var downloadSuccess = false

	private val requestStoragePermission =
		registerForActivityResult(RequestPermission()) { result: Boolean ->
			if (result) {
				val imageUrl = arguments?.getString(ARG_IMAGE_URL)
				val fileName = Uri.parse(imageUrl).lastPathSegment
				val image = view?.findViewById<ImageView>(R.id.iv_image)
				image?.let { startDownload(it, fileName) }
			}
		}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.inflate(R.layout.fragment_image_preview, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val tvName = view.findViewById<TextView>(R.id.tv_name)
		val tvDate = view.findViewById<TextView>(R.id.tv_date)
		val ibBack = view.findViewById<ImageButton>(R.id.ib_back)
		val ibSave = view.findViewById<ImageButton>(R.id.ib_save)
		val tvMessage = view.findViewById<TextView>(R.id.tv_message)
		val image = view.findViewById<ImageView>(R.id.iv_image)
		val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
		val senderName = requireArguments().getString(ARG_SENDER_NAME)
		val msgDate = requireArguments().getSerializable(ARG_DATE) as Date?
		val message = requireArguments().getString(ARG_MESSAGE)
		val imageUrl = requireArguments().getString(ARG_IMAGE_URL)
		val fileName = Uri.parse(imageUrl).lastPathSegment
		setDateText(tvDate, msgDate)
		tvName.text = senderName
		tvMessage.text = message

		Glide.with(requireContext())
			.load(imageUrl)
			.error(R.drawable.placeholder_load_image)
			.listener(object : RequestListener<Drawable?> {
				override fun onLoadFailed(
					e: GlideException?,
					model: Any?,
					target: Target<Drawable?>,
					isFirstResource: Boolean
				): Boolean {
					progressBar.visibility = View.GONE
					return false
				}

				override fun onResourceReady(
					resource: Drawable,
					model: Any,
					target: Target<Drawable?>?,
					dataSource: DataSource,
					isFirstResource: Boolean
				): Boolean {
					progressBar.visibility = View.GONE
					downloadSuccess = true
					return false
				}
			})
			.into(image)
		ibBack.setOnClickListener {
			parentFragmentManager.popBackStack()
		}

		ibSave.setOnClickListener {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
				// Android 10 and below — request WRITE_EXTERNAL_STORAGE
				if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
					== PackageManager.PERMISSION_GRANTED) {
					startDownload(image, fileName)
				} else {
					requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
				}
			} else {
				// Android 11 and above — WRITE_EXTERNAL_STORAGE permission is not required
				startDownload(image, fileName)
			}
		}
	}

	private fun startDownload(image: ImageView, fileName: String?) {
		if (downloadSuccess) {
			val bitmapDrawable = image.drawable as BitmapDrawable
			viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
				val result = saveImage(bitmapDrawable.bitmap, fileName)
				if (result) {
					withContext(Dispatchers.Main) {
						Toast.makeText(context, getString(R.string.image_saved), Toast.LENGTH_LONG).show()
					}
				}
			}
		}
	}

	private fun setDateText(tvDate: TextView, msgDate: Date?) {
		val dateFormat = DateFormat.getDateFormat(context)
		val timeFormat = DateFormat.getTimeFormat(context)
		val time = timeFormat.format(msgDate)
		var dateStr = dateFormat.format(msgDate)
		val today = Date()
		val todayCal = Calendar.getInstance()
		todayCal.time = today
		val msgCal = Calendar.getInstance()
		msgCal.time = msgDate
		val areSameDay =
			todayCal[Calendar.YEAR] == msgCal[Calendar.YEAR] && todayCal[Calendar.MONTH] == msgCal[Calendar.MONTH] && todayCal[Calendar.DAY_OF_MONTH] == msgCal[Calendar.DAY_OF_MONTH]
		if (areSameDay) {
			dateStr = getString(R.string.today)
		}
		tvDate.text = getString(R.string.msg_date_time, dateStr, time)
	}

	private fun saveImage(image: Bitmap, imageFileName: String?): Boolean {
		var savedImagePath: String? = null
		val storageDir = File(
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
				.toString() + "/" + context?.packageName
		)
		var success = true
		if (!storageDir.exists()) {
			success = storageDir.mkdirs()
		}
		if (success) {
			val imageFile = File(storageDir, imageFileName)
			if (imageFile.exists()) {
				return true
			}
			savedImagePath = imageFile.absolutePath
			try {
				val fOut: OutputStream = FileOutputStream(imageFile)
				image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
				fOut.close()
			} catch (e: Exception) {
				Log.e(this.javaClass.name, e.message, e)
				return false
			}

			// Add the image to the system gallery
			galleryAddPic(savedImagePath)
			return true
		}

		return false
	}

	private fun galleryAddPic(imagePath: String?) {
		try {
			val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
			val f = File(imagePath)
			val contentUri = Uri.fromFile(f)
			mediaScanIntent.setData(contentUri)
			context?.sendBroadcast(mediaScanIntent)
		} catch (e: Exception) {
			Log.e(this.javaClass.name, e.message, e)
		}
	}
}