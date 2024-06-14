package ru.iqchannels.sdk.ui.nav_bar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.ui.theming.Medium15
import ru.iqchannels.sdk.ui.theming.Regular13

@Composable
fun NavBar(
	title: String,
	onBackClick: () -> Unit
) {
	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.height(44.dp)
	) {
		Box(
			Modifier.fillMaxWidth()
		) {
			Icon(
				painter = painterResource(id = R.drawable.ic_arrow_left),
				contentDescription = null,
				tint = colorResource(id = R.color.dark_text_color),
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

				Text(
					text = title,
					textAlign = TextAlign.Center,
					color = colorResource(id = R.color.dark_text_color),
					style = Medium15,
					maxLines = 1,
					modifier = Modifier
						.fillMaxWidth()
				)

				Text(
					text = stringResource(id = R.string.online),
					color = colorResource(id = R.color.other_name),
					textAlign = TextAlign.Center,
					style = Regular13,
					maxLines = 1,
					modifier = Modifier
						.fillMaxWidth()
				)
			}

		}
	}
}