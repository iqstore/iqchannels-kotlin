package ru.iqchannels.sdk.ui.rv

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import ru.iqchannels.sdk.ui.UiUtils

class MarginItemDecoration : ItemDecoration() {
	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		super.getItemOffsets(outRect, view, parent, state)
		val margin = UiUtils.toPx(5)
		outRect[0, 0, 0] = margin
	}
}
