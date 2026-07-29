package ru.iqchannels.sdk.ui.nav_bar


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color as ComposeColor
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.localization.IQChannelsLanguage
import ru.iqchannels.sdk.styling.Color
import ru.iqchannels.sdk.styling.IQStyles
import ru.iqchannels.sdk.ui.theming.Regular13

@Composable
fun Color.toComposeColor(): ComposeColor {
	return ComposeColor(getColorInt(LocalContext.current))
}

@Composable
fun NavBar(
	title: String,
	onBackClick: () -> Unit
) {
	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.height(44.dp),
		color = IQStyles.iqChannelsStyles?.appBar?.background?.toComposeColor()
			?: MaterialTheme.colors.surface
	) {
		Box(
			Modifier.fillMaxWidth()
		) {
			Icon(
				painter = painterResource(id = R.drawable.ic_arrow_left),
				contentDescription = null,
//				tint = colorResource(id = R.color.dark_text_color),
				tint = IQStyles.iqChannelsStyles?.appBar?.backButton?.toComposeColor()
					?: colorResource(id = R.color.dark_text_color),
				modifier = Modifier
					.align(Alignment.CenterStart)
					.padding(start = 16.dp)
					.clickable { onBackClick() }
			)

			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 25.dp)
			) {
//				Text(
//					text = title,
//					textAlign = TextAlign.Center,
//					color = colorResource(id = R.color.dark_text_color),
//					style = Medium15,
//					maxLines = 1,
//					modifier = Modifier
//						.fillMaxWidth()
//				)
				val context = LocalContext.current
				val titleLabel = IQStyles.iqChannelsStyles?.appBar?.titleLabel

				Text(
					text = title,
					textAlign = when (titleLabel?.textAlignment?.lowercase()) {
						"left" -> TextAlign.Start
						"right" -> TextAlign.End
						"center" -> TextAlign.Center
						else -> TextAlign.Center
					},
					color = titleLabel?.color?.let {
						ComposeColor(it.getColorInt(context))
					} ?: colorResource(id = R.color.dark_text_color),
					style = TextStyle(
						fontSize = (titleLabel?.textSize ?: 15f).sp,
						fontWeight = if (titleLabel?.textStyle?.bold == true) FontWeight.Bold else FontWeight.Normal,
						fontStyle = if (titleLabel?.textStyle?.italic == true) FontStyle.Italic else FontStyle.Normal
					),
					maxLines = 1,
					modifier = Modifier.fillMaxWidth()
				)


//				Text(
//					text = IQChannelsLanguage.iqChannelsLanguage.statusLabel,
//					color = colorResource(id = R.color.other_name),
//					textAlign = TextAlign.Center,
//					style = Regular13,
//					maxLines = 1,
//					modifier = Modifier
//						.fillMaxWidth()
//				)
				val statusLabel = IQStyles.iqChannelsStyles?.appBar?.statusLabel

				Text(
					text = IQChannelsLanguage.iqChannelsLanguage.statusLabel,
					textAlign = when (statusLabel?.textAlignment?.lowercase()) {
						"left" -> TextAlign.Start
						"right" -> TextAlign.End
						"center" -> TextAlign.Center
						"justify" -> TextAlign.Justify
						else -> TextAlign.Center
					},
					color = statusLabel?.color?.let {
						ComposeColor(it.getColorInt(context))
					} ?: colorResource(id = R.color.other_name),
					style = Regular13.copy(
						fontSize = (statusLabel?.textSize ?: 13f).sp,
						fontWeight = if (statusLabel?.textStyle?.bold == true) FontWeight.Bold else FontWeight.Normal,
						fontStyle = if (statusLabel?.textStyle?.italic == true) FontStyle.Italic else FontStyle.Normal
					),
					maxLines = 1,
					modifier = Modifier.fillMaxWidth()
				)
			}

		}
	}
}