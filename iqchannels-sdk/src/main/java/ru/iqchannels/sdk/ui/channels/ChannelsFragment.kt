package ru.iqchannels.sdk.ui.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
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

	companion object {

		private const val ARG_NAV_BAR_ENABLED = "ChannelsFragment#navBarEnabled"

		fun newInstance(navBarEnabled: Boolean = true) = ChannelsFragment().apply {
			arguments = bundleOf(
				ARG_NAV_BAR_ENABLED to navBarEnabled
			)
		}
	}

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
		val navBarEnabled = requireArguments().getBoolean(ARG_NAV_BAR_ENABLED)

		viewModel.events
			.flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
			.onEach {
				when(it) {
					is ChannelsViewModel.Navigate2Chat -> {
						parentFragmentManager.commit {
							setReorderingAllowed(true)
							val title = if (navBarEnabled) {
								it.channel.name
							} else {
								null
							}

							val fragment = ChatFragment.newInstance(title)
							replace((this@ChannelsFragment.view?.parent as ViewGroup).id, fragment)

							if (!it.clearStack) {
								addToBackStack(null)
							}
						}
					}
				}
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)

		viewModel.onViewCreated()
	}
}