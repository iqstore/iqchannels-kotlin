package ru.iqchannels.sdk.ui.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ru.iqchannels.sdk.ui.theming.IQChannelsCompose

class ChannelsFragment : Fragment() {

	private val viewModel: ChannelsViewModel by viewModels()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return IQChannelsCompose(requireContext()) {
			ChannelsScreen(channelsViewModel = viewModel)
		}
	}
}