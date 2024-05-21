/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import ru.iqchannels.example.shortcuts.ShortCutsFragment
import ru.iqchannels.sdk.ui.theming.IQChannelsCompose

class PlusOneFragment : Fragment() {

	companion object {
		fun newInstance(): PlusOneFragment {
			val fragment = PlusOneFragment()
			val args = Bundle()
			fragment.setArguments(args)
			return fragment
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	) = IQChannelsCompose(requireContext()) {

		Surface {
			Column {
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
			}
		}
	}

	private fun openShortCuts() {
		parentFragmentManager.beginTransaction()
			.replace(R.id.content, ShortCutsFragment())
			.addToBackStack(null)
			.commit()
	}

}
