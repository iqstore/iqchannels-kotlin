package ru.iqchannels.sdk.ui.theming

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

fun IQChannelsCompose(context: Context, content: @Composable () -> Unit) = ComposeView(context).apply {
	setViewCompositionStrategy(
		ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
	)

	setContent {
		IQChannelsTheme {
			content()
		}
	}
}