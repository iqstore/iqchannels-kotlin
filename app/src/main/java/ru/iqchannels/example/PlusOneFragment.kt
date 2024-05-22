/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.iqchannels.example.shortcuts.ShortCutsFragment
import ru.iqchannels.sdk.domain.models.ChatType
import ru.iqchannels.sdk.ui.theming.IQChannelsCompose

class PlusOneFragment : Fragment() {

	companion object {

	}

	private val viewModel: PlusOneViewModel by viewModels()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	) = IQChannelsCompose(requireContext()) {

		Surface {

			val testingType by viewModel.testingType.collectAsState()

			Column(
				modifier = Modifier.padding(16.dp)
			) {
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

		viewModel.testingType
			.flowWithLifecycle(viewLifecycleOwner.lifecycle)
			.onEach {
				requireContext().getSharedPreferences(IQAppActivity.PREFS, Context.MODE_PRIVATE)
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

}
