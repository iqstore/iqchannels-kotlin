package ru.iqchannels.example.prefill

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.MutableStateFlow
import ru.iqchannels.example.R
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.domain.models.PreFilledMessages
import ru.iqchannels.sdk.ui.ChatFragment
import ru.iqchannels.sdk.ui.theming.IQChannelsCompose

class PreFillMsgFragment : Fragment() {

	private val requestAllPermissions =
		registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
			showAttachChooser(false)
		}

	private val requestPickImageFromFiles =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
			if (it.resultCode == Activity.RESULT_OK) {
				val intent = it.data
				val uri = intent?.data

				when (uri == null) {
					true -> { // multiple choice
						it.data?.clipData?.let { clipData ->
							val uris = java.util.ArrayList<Uri>()
							val itemCount = clipData.itemCount
							for (i in 0 until itemCount) {
								uris.add(clipData.getItemAt(i).uri)
							}

							selectedFiles.value = uris
						}
					}

					false -> { // single choice
						selectedFiles.value = listOf(uri)
					}
				}
			}
		}

	private val selectedFiles = MutableStateFlow<List<Uri>>(emptyList())

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return IQChannelsCompose(requireContext()) {
			Surface {

				var textMsg by rememberSaveable {
					mutableStateOf("")
				}

				val selectedFiles by selectedFiles.collectAsState()

				Column(
					modifier = Modifier.padding(16.dp)
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { showChat(textMsg) }
							.padding(16.dp)
					) {
						Text(
							text = "showChat()",
							textDecoration = TextDecoration.Underline,
							color = Color.Blue,
							fontSize = 16.sp,
						)
					}

					Spacer(modifier = Modifier.height(16.dp))

					Text(text = "Текст сообщения:")
					TextField(
						value = textMsg,
						onValueChange = { textMsg = it },
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp)
					)

					Spacer(modifier = Modifier.height(16.dp))

					Button(onClick = { showAttachChooser() }) {
						Text(
							text = "Выбрать файлы"
						)
					}
					Text(
						text = "Выбранные файлы:"
					)
					Text(text = selectedFiles.joinToString(separator = ";\n"))
				}
			}
		}
	}

	private fun showAttachChooser() {
		if (ContextCompat.checkSelfPermission(
				requireContext(),
				Manifest.permission.READ_EXTERNAL_STORAGE
			) == PackageManager.PERMISSION_DENIED
		) {
			requestAllPermissions.launch(
				arrayOf(
					Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE
				)
			)

			return
		}

		showAttachChooser(false)
	}

	private fun showAttachChooser(withCamera: Boolean) {
		// Create a gallery intent.
		val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
		galleryIntent.setType("image/*")
		galleryIntent.putExtra(
			Intent.EXTRA_MIME_TYPES, arrayOf(
				"audio/*", "video/*", "text/*", "application/*", "file/*"
			)
		)
		galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

		// Create and start an intent chooser.
		val chooser = Intent.createChooser(galleryIntent, "")

		requestPickImageFromFiles.launch(chooser)
	}

	private fun showChat(text: String) {
		IQChannels.login("3")

		val fragment = ChatFragment.newInstance(
			preFilledMessages = PreFilledMessages(
				textMsg = listOf(text),
				fileMsg = selectedFiles.value
			)
		)

		parentFragmentManager
			.beginTransaction()
			.replace(R.id.content, fragment).addToBackStack(null)
			.commit()
	}
}