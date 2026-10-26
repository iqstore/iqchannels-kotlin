package ru.iqchannels.sdk.ui.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.app.AdvancedUnread
import ru.iqchannels.sdk.app.Channel as UnreadChannel
import ru.iqchannels.sdk.domain.models.Channel
import ru.iqchannels.sdk.domain.models.ChatType
import ru.iqchannels.sdk.styling.IQStyles
import ru.iqchannels.sdk.ui.nav_bar.toComposeColor
import ru.iqchannels.sdk.ui.theming.Regular16

@Composable
fun ru.iqchannels.sdk.styling.Color.toComposeColor(): Color {
	return Color(getColorInt(LocalContext.current))
}

@Composable
fun ChannelsScreen(channelsViewModel: ChannelsViewModel) {

	val channels by channelsViewModel.channels.collectAsState()
	val advancedUnread by channelsViewModel.advancedUnread.collectAsState()

	ChannelsScreenContent(
		channels = channels,
		advancedUnread = advancedUnread,
		onChannelClick = channelsViewModel::onChannelClick
	)
}

@Composable
private fun ChannelsScreenContent(
	channels: List<Channel>,
	advancedUnread: AdvancedUnread?,
	onChannelClick: (Channel) -> Unit
) {
	Surface (
		color = IQStyles.iqChannelsStyles?.multiСhat?.background?.toComposeColor()
			?: MaterialTheme.colors.surface
	){
		LazyColumn(
			modifier = Modifier.fillMaxWidth()
		) {
			items(channels) { channel ->
				ChannelItem(
					channel = channel,
					unread = advancedUnread?.channels?.find {
						it.name == channel.id && it.chatType == channel.chatType.name.lowercase()
					},
					onClick = onChannelClick
				)
			}
		}
	}
}

@Composable
private fun ChannelItem(channel: Channel, unread: UnreadChannel?, onClick: (Channel) -> Unit) {
	val context = LocalContext.current
	val chatStyles = IQStyles.iqChannelsStyles?.multiСhat

	val style = when (channel.chatType) {
		ChatType.REGULAR -> Triple(
			chatStyles?.icon_regular_chat,
			chatStyles?.backgroundIcon,
			chatStyles?.lastMessage
		)

		ChatType.PERSONAL_MANAGER -> Triple(
			chatStyles?.icon_personal_manager_chat,
			chatStyles?.backgroundIcon,
			chatStyles?.lastMessage
		)

		ChatType.INFO -> Triple(
			chatStyles?.icon_info_chat,
			chatStyles?.backgroundIcon,
			chatStyles?.lastMessage
		)
	}

	val iconUrl = style.first
	val iconBackground = style.second
	val lastMessageStyle = style.third

	val timeText = unread?.lastMessage?.date
		?.let { parseLastMessageDate(it) }
		?.let { formatLastMessageTime(it) }
		.orEmpty()

	val messageText = when {
		unread?.lastMessage == null -> ""
		unread.lastMessage?.isSurvey == true -> "Опрос"
		unread.lastMessage?.text.isNullOrBlank() && unread.lastMessage?.fileId != null ->
			unread.lastMessage?.fileId.orEmpty()
		else -> unread.lastMessage?.text.orEmpty()
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { onClick(channel) }
			.padding(horizontal = 12.dp, vertical = 16.dp)
	) {
		Box(
			modifier = Modifier
				.size(52.dp)
				.background(
					color = channel.iconColor?.let { Color(android.graphics.Color.parseColor(it)) }
						?: iconBackground?.toComposeColor() ?: colorResource(id = R.color.channel_red),
					shape = CircleShape
				)
		) {
			if (iconUrl != null) {
				AsyncImage(
					model = iconUrl,
					contentDescription = null,
					modifier = Modifier
						.size(52.dp)
						.align(Alignment.Center)
				)
			} else {
				Icon(
					painter = painterResource(
						when (channel.chatType) {
							ChatType.REGULAR -> R.drawable.ic_chat_common
							ChatType.PERSONAL_MANAGER -> R.drawable.ic_pm_24
							ChatType.INFO -> R.drawable.ic_chat_common
						}
					),
					contentDescription = null,
					tint = Color.White,
					modifier = Modifier.align(Alignment.Center)
				)
			}
		}

		Spacer(modifier = Modifier.width(8.dp))

		Column(
			modifier = Modifier.weight(1f)
		) {
			FirstRow(channel = channel, timeText = timeText)

			Row(
				modifier = Modifier.padding(top = 4.dp),
				verticalAlignment = Alignment.Top
			) {
				Text(
					text = messageText,
					color = lastMessageStyle?.color?.let {
						Color(it.getColorInt(context))
					} ?: colorResource(id = R.color.text_color_description),
					style = lastMessageStyle?.let{
						TextStyle(
							fontSize = (it.textSize ?: 15f).sp,
							fontWeight = if (it.textStyle?.bold == true) FontWeight.Bold else FontWeight.Normal,
							fontStyle = if (it.textStyle?.italic == true) FontStyle.Italic else FontStyle.Normal
						)
					} ?: Regular16,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)

				unread?.unreadCount?.takeIf { it > 0 }?.let { count ->
					Box(
						modifier = Modifier
							.padding(start = 8.dp)
							.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
							.clip(CircleShape)
							.background(color = colorResource(id = R.color.unread_msg_count_bg)),
						contentAlignment = Alignment.Center
					) {
						Text(
							text = "$count",
							color = Color.White,
							fontSize = 13.sp,
							fontWeight = FontWeight.SemiBold
						)
					}
				}
			}
		}
	}
}

@Composable
private fun FirstRow(channel: Channel, timeText: String) {
	val context = LocalContext.current
	val title = IQStyles.iqChannelsStyles?.multiСhat?.title

	Row(verticalAlignment = Alignment.Top) {
		Text(
			text = channel.name ?: "",
			color = title?.color?.let {
				Color(it.getColorInt(context))
			} ?: colorResource(id = R.color.text_color_h1),
			style = title?.let {
				TextStyle(
					fontSize = (it.textSize ?: 15f).sp,
					fontWeight = if (it.textStyle?.bold == true) FontWeight.Bold else FontWeight.Normal,
					fontStyle = if (it.textStyle?.italic == true) FontStyle.Italic else FontStyle.Normal
				)
			} ?: MaterialTheme.typography.h1,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.weight(1f)
		)

		if (timeText.isNotEmpty()) {
			Text(
				text = timeText,
				color = colorResource(id = R.color.text_color_description),
				fontSize = 13.sp,
				maxLines = 1,
				modifier = Modifier.padding(start = 8.dp)
			)
		}
	}
}

private fun parseLastMessageDate(raw: String): Date? {
	raw.toLongOrNull()?.let { value ->
		return if (value > 1_000_000_000_000L) Date(value / 1000) else Date(value)
	}

	val normalized = raw
		.replace(Regex("\\.\\d+"), "")
		.replace(Regex("\\s+[A-Za-z]+\\s*$"), "")
		.trim()

	val patterns = listOf(
		"yyyy-MM-dd HH:mm:ss Z",
		"yyyy-MM-dd HH:mm:ss",
		"yyyy-MM-dd'T'HH:mm:ssZ",
		"yyyy-MM-dd'T'HH:mm:ss"
	)

	for (pattern in patterns) {
		try {
			val format = SimpleDateFormat(pattern, Locale.US)
			if (!pattern.endsWith("Z")) {
				format.timeZone = TimeZone.getTimeZone("UTC")
			}
			format.isLenient = false
			format.parse(normalized)?.let { return it }
		} catch (_: Exception) {
		}
	}

	return null
}

private fun formatLastMessageTime(date: Date): String {
	val message = Calendar.getInstance().apply { time = date }
	val now = Calendar.getInstance()

	val pattern = when {
		message.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
			message.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> "HH:mm"

		message.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> "dd.MM"

		else -> "dd.MM.yyyy"
	}

	return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}
