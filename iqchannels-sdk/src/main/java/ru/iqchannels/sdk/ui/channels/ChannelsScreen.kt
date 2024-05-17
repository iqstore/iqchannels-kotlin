package ru.iqchannels.sdk.ui.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.domain.models.Channel

@Composable
fun ChannelsScreen(channelsViewModel: ChannelsViewModel) {

	val channels by channelsViewModel.channels.collectAsState()

	ChannelsScreenContent(
		channels = channels,
		onChannelClick = channelsViewModel::onChannelClick
	)
}

@Composable
private fun ChannelsScreenContent(
	channels: List<Channel>,
	onChannelClick: (Channel) -> Unit
) {
	Surface {
		LazyColumn(
			modifier = Modifier.fillMaxWidth()
		) {
			items(channels) {
				ChannelItem(channel = it, onClick = onChannelClick)
			}
		}
	}
}

@Composable
private fun ChannelItem(channel: Channel, onClick: (Channel) -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth()
			.clickable { onClick(channel) }
	) {
		Box(
			modifier = Modifier
				.size(52.dp)
				.background(
					color = colorResource(id = R.color.channel_red),
					shape = CircleShape
				)
		) {
			Icon(
				painter = painterResource(id = R.drawable.ic_chat_common),
				contentDescription = null
			)
		}

		Column(
			modifier = Modifier.padding(start = 16.dp)
		) {
			FirstRow(channel = channel)
			
			Text(text = "message")
		}
	}
}

@Composable
private fun FirstRow(channel: Channel) {
	Row {
		Text(text = channel.name ?: "")

//		if (channel.lastMessage?.My == true) {
//			Icon(
//				painter = painterResource(id = R.drawable.ic_channel_read),
//				contentDescription = null
//			)
//		}

		//ChannelTime(channel = channel)
	}
}

@Composable
private fun ChannelTime(channel: Channel) {
	Text(text = "12:44")
}

@Composable
private fun SecondRow(channel: Channel) {
	Row {
		//Text(text = channel.lastMessage?.Text ?: "")

		Text(
			text = "1",
			modifier = Modifier
				.clip(CircleShape)
				.background(color = colorResource(id = R.color.unread_msg_count_bg))
		)
	}
}