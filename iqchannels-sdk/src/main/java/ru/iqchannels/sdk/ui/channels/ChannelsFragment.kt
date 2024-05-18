package ru.iqchannels.sdk.ui.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.iqchannels.sdk.ui.ChatFragment
import ru.iqchannels.sdk.ui.images.ImagePreviewFragment
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

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		viewModel.events
			.flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
			.onEach {
				when(it) {
					is ChannelsViewModel.Navigate2Chat -> {
						parentFragmentManager.commit {
							val fragment = ChatFragment.newInstance()
							replace((this@ChannelsFragment.view?.parent as ViewGroup).id, fragment)
							addToBackStack(null)
						}
					}
				}
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)

		viewModel.onViewCreated()
	}
}