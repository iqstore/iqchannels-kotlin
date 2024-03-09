package ru.iqchannels.sdk.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.download.FileDownloader.downloadFile
import ru.iqchannels.sdk.ui.Colors.textColor

class FileActionsChooseFragment : BottomSheetDialogFragment() {

	companion object {
		private const val ARG_URL = "FileActionsChooseFragment#argUrl"
		private const val ARG_FILE_NAME = "FileActionsChooseFragment#argFileName"
		private const val REQUEST_STORAGE_PERMISSION = 1
		const val REQUEST_KEY = "FileActionsChooseFragment#requestKey"
		const val KEY_DOWNLOAD_ID = "FileActionsChooseFragment#downloadId"
		const val KEY_FILE_NAME = "FileActionsChooseFragment#resultFileName"

		fun newInstance(url: String, fileName: String): FileActionsChooseFragment {
			val fragment = FileActionsChooseFragment()
			val args = Bundle()
			args.putString(ARG_URL, url)
			args.putString(ARG_FILE_NAME, fileName)
			fragment.arguments = args

			return fragment
		}
	}

	override fun getTheme(): Int {
		return R.style.Theme_BottomNavBar
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		super.onCreateView(inflater, container, savedInstanceState)
		return inflater.inflate(R.layout.fragment_file_actions_choose, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val tvOpenFile = view.findViewById<TextView>(R.id.tv_open_file)
		val tvSaveFile = view.findViewById<TextView>(R.id.tv_save_file)
		val url = requireArguments().getString(ARG_URL)
		val fileName = requireArguments().getString(ARG_FILE_NAME)
		tvOpenFile.setTextColor(textColor())
		tvSaveFile.setTextColor(textColor())

		tvOpenFile.setOnClickListener {
			val i = Intent(Intent.ACTION_VIEW)
			i.setData(Uri.parse(url))
			context?.startActivity(i)
			dismiss()
		}

		tvSaveFile.setOnClickListener {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
				if (ContextCompat.checkSelfPermission(
						requireContext(),
						Manifest.permission.WRITE_EXTERNAL_STORAGE
					)
					== PackageManager.PERMISSION_GRANTED
				) {
					downloadAndFinish(url, fileName)
				} else {
					ActivityCompat.requestPermissions(
						requireActivity(), arrayOf(
							Manifest.permission.WRITE_EXTERNAL_STORAGE
						), REQUEST_STORAGE_PERMISSION
					)
				}
			} else {
				downloadAndFinish(url, fileName)
			}
		}
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == REQUEST_STORAGE_PERMISSION) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				val url = requireArguments().getString(ARG_URL)
				val fileName = requireArguments().getString(ARG_FILE_NAME)
				downloadAndFinish(url, fileName)
			}
		}
	}

	private fun downloadAndFinish(url: String?, fileName: String?) {
		val id = downloadFile(requireContext(), url, fileName)
		val bundle = Bundle()
		bundle.putLong(KEY_DOWNLOAD_ID, id)
		bundle.putString(KEY_FILE_NAME, fileName)
		parentFragmentManager.setFragmentResult(REQUEST_KEY, bundle)
		dismiss()
	}
}
