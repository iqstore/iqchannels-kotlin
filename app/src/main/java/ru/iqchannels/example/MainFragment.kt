/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalFocusManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.iqchannels.example.localizations.LocalizationsEditFragment
import ru.iqchannels.example.prefill.PreFillMsgFragment
import ru.iqchannels.example.shortcuts.ShortCutsFragment
import ru.iqchannels.example.styles.StylesEditFragment
import ru.iqchannels.sdk.ui.theming.IQChannelsCompose
import kotlin.system.exitProcess


class MainFragment : Fragment() {

	private val viewModel: PlusOneViewModel by viewModels()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	) = IQChannelsCompose(requireContext()) {

		Surface {
			val focusManager = LocalFocusManager.current

			val testingType by viewModel.testingType.collectAsState()
			val address by viewModel.address.collectAsState()
			val channels by viewModel.channels.collectAsState()
			val chatToOpen by viewModel.chatToOpen.collectAsState()

			Column(
				modifier = Modifier
					.padding(16.dp)
					.verticalScroll(rememberScrollState())
					.clickable(
						indication = null,
						interactionSource = remember { MutableInteractionSource() }
					) {
						focusManager.clearFocus()
					}
			) {

				Text(
					text = "Edit styles",
					textDecoration = TextDecoration.Underline,
					color = Color.Blue,
					fontSize = 20.sp,
					modifier = Modifier
						.fillMaxWidth()
						.clickable { openStylesEdit() }
						.padding(16.dp)
				)
				Divider(Modifier.fillMaxWidth())

				Text(
					text = "Edit languages",
					textDecoration = TextDecoration.Underline,
					color = Color.Blue,
					fontSize = 20.sp,
					modifier = Modifier
						.fillMaxWidth()
						.clickable { openLanguagesEdit() }
						.padding(16.dp)
				)
				Divider(Modifier.fillMaxWidth())

				Text(
					text = "Open short cuts",
					textDecoration = TextDecoration.Underline,
					color = Color.Blue,
					fontSize = 20.sp,
					modifier = Modifier
						.fillMaxWidth()
						.clickable { openShortCuts() }
						.padding(16.dp)
				)
				Divider(Modifier.fillMaxWidth())

				Text(
					text = "Предзаполненные сообщения",
					textDecoration = TextDecoration.Underline,
					color = Color.Blue,
					fontSize = 20.sp,
					modifier = Modifier
						.fillMaxWidth()
						.clickable { openPreFillMsg() }
						.padding(16.dp)
				)
				Divider(Modifier.fillMaxWidth())

				Spacer(modifier = Modifier.height(16.dp))

				Text(text = "Server:")
				TextField(value = address, onValueChange = viewModel::onAddressChange)

				Text(text = "Channels:")
				TextField(value = channels.joinToString(), onValueChange = viewModel::onChannelsChange)

				Text(text = "Chat to open:")
				TextField(value = chatToOpen, onValueChange = viewModel::onChatToOpenChange)

				Button(onClick = { saveConfigs(address, channels, chatToOpen) }) {
					Text(text = "Save configs")
				}

				Divider(Modifier.fillMaxWidth())

				Spacer(modifier = Modifier.height(16.dp))

				Text(text = "Testing:")
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable { viewModel.onTestingChange(TestingType.MultiChat) }
						.padding(horizontal = 8.dp)
				) {
					RadioButton(
						selected = testingType == TestingType.MultiChat,
						onClick = { viewModel.onTestingChange(TestingType.MultiChat) }
					)
					Text(
						text = TestingType.MultiChat.name,
						modifier = Modifier
							.padding(start = 8.dp)
							.align(Alignment.CenterVertically)
					)
				}
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable { viewModel.onTestingChange(TestingType.SingleChat) }
						.padding(horizontal = 8.dp)
				) {
					RadioButton(
						selected = testingType == TestingType.SingleChat,
						onClick = { viewModel.onTestingChange(TestingType.SingleChat)}
					)
					Text(
						text = TestingType.SingleChat.name,
						modifier = Modifier
							.padding(start = 8.dp)
							.align(Alignment.CenterVertically)
					)
				}
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val prefs = requireContext().getSharedPreferences(IQAppActivity.PREFS, Context.MODE_PRIVATE)

		prefs.getString(IQAppActivity.TESTING_TYPE, TestingType.MultiChat.name)?.let {
			viewModel.onTestingChange(TestingType.valueOf(it))
		}

		prefs.getString(IQAppActivity.ADDRESS, "https://sandbox.iqstore.ru")?.let {
			viewModel.onAddressChange(it)
		}

		prefs.getStringSet(IQAppActivity.CHANNELS, setOf("support", "finance"))?.let {
			viewModel.onChannelsChange(it.joinToString())
		}

		prefs.getString(IQAppActivity.CHATTOOPEN, "")?.let {
			viewModel.onChatToOpenChange(it)
		}

		viewModel.testingType
			.flowWithLifecycle(viewLifecycleOwner.lifecycle)
			.onEach {
				prefs
					.edit()
					.putString(IQAppActivity.TESTING_TYPE, it.name)
					.apply()
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)
	}

	private fun openShortCuts() {
		parentFragmentManager.beginTransaction()
			.replace(R.id.content, ShortCutsFragment())
			.addToBackStack(null)
			.commit()
	}

	private fun openStylesEdit() {
		parentFragmentManager.beginTransaction()
			.replace(R.id.content, StylesEditFragment())
			.addToBackStack(null)
			.commit()
	}

	private fun openLanguagesEdit() {
		parentFragmentManager.beginTransaction()
			.replace(R.id.content, LocalizationsEditFragment())
			.addToBackStack(null)
			.commit()
	}

	private fun openPreFillMsg() {
		parentFragmentManager.beginTransaction()
			.replace(R.id.content, PreFillMsgFragment())
			.addToBackStack(null)
			.commit()
	}

	private fun saveConfigs(address: String, channels: List<String>, chatToOpen: String) {
		requireContext().getSharedPreferences(IQAppActivity.PREFS, Context.MODE_PRIVATE)
			.edit()
			.putString(IQAppActivity.ADDRESS, address)
			.putStringSet(IQAppActivity.CHANNELS, channels.toSet())
			.putString(IQAppActivity.CHATTOOPEN, chatToOpen)
			.apply()

		Snackbar.make(
			requireView(),
			"Saved",
			Snackbar.LENGTH_SHORT
		).apply {
			setAction("Restart") {
				restartAppToApply()
			}
			show()
		}
	}

	private fun restartAppToApply() {
		activity?.finish()
		val intent = Intent(context?.applicationContext, IQAppActivity::class.java)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		context?.startActivity(intent)
		exitProcess(0)
	}
}
