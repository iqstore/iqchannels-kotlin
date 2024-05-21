package ru.iqchannels.example.shortcuts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ru.iqchannels.example.R
import ru.iqchannels.sdk.app.IQChannelsShortCuts
import ru.iqchannels.sdk.domain.models.ChatType
import ru.iqchannels.sdk.ui.theming.IQChannelsCompose

class ShortCutsFragment : Fragment() {

	private val viewModel: ShortCutsViewModel by viewModels()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return IQChannelsCompose(requireContext()) {
			Surface {

				val chatType by viewModel.chatType.collectAsState()
				val channel by viewModel.channel.collectAsState()

				Column(
					modifier = Modifier.padding(16.dp)
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { showChat(channel, chatType) }
							.padding(16.dp)
					) {
						Text(
							text = "showChat()",
							textDecoration = TextDecoration.Underline,
							color = Color.Blue,
							fontSize = 16.sp,
						)
					}

					Text(text = "Chat type:")
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { viewModel.onChatTypeChange(ChatType.REGULAR) }
							.padding(horizontal = 8.dp)
					) {
						RadioButton(
							selected = chatType == ChatType.REGULAR,
							onClick = { viewModel.onChatTypeChange(ChatType.REGULAR) }
						)
						Text(
							text = "Regular",
							modifier = Modifier
								.padding(start = 8.dp)
								.align(Alignment.CenterVertically)
						)
					}
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { viewModel.onChatTypeChange(ChatType.PERSONAL_MANAGER) }
							.padding(horizontal = 8.dp)
					) {
						RadioButton(
							selected = chatType == ChatType.PERSONAL_MANAGER,
							onClick = { viewModel.onChatTypeChange(ChatType.PERSONAL_MANAGER) }
						)
						Text(
							text = "Personal manager",
							modifier = Modifier
								.padding(start = 8.dp)
								.align(Alignment.CenterVertically)
						)
					}
					Spacer(modifier = Modifier.height(16.dp))

					Text(text = "Channel")
					TextField(
						value = channel,
						onValueChange = { viewModel.onChannelChange(it) },
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp)
					)
				}
			}
		}
	}

	private fun showChat(channelName: String, chatType: ChatType) {
		IQChannelsShortCuts.showChat(channelName, chatType, parentFragmentManager, R.id.content)
	}
}