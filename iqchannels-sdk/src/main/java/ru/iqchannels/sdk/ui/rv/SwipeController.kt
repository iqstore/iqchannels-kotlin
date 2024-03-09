package ru.iqchannels.sdk.ui.rv

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Canvas
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeController(private val swipeListener: SwipeListener) : ItemTouchHelper.Callback() {

	private var swipeBack = false

	override fun getMovementFlags(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder
	): Int {
		return makeMovementFlags(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.RIGHT)
	}

	override fun onMove(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		target: RecyclerView.ViewHolder
	): Boolean {
		return false
	}

	override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
	override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
		if (swipeBack) {
			swipeBack = false
			return 0
		}
		return super.convertToAbsoluteDirection(flags, layoutDirection)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onChildDraw(
		c: Canvas,
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		dX: Float,
		dY: Float,
		actionState: Int,
		isCurrentlyActive: Boolean
	) {
		if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
			recyclerView.setOnTouchListener { v: View?, event: MotionEvent ->
				swipeBack =
					event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
				if (swipeBack) {
					if (Math.abs(viewHolder.itemView.translationX) >= toPx(100)) {
						swipeListener.onSwiped(viewHolder.adapterPosition)
					}
				}
				false
			}
		}
		super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
	}

	private fun toPx(dp: Int): Float {
		return TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
			Resources.getSystem().displayMetrics
		)
	}

	interface SwipeListener {
		fun onSwiped(position: Int)
	}
}
