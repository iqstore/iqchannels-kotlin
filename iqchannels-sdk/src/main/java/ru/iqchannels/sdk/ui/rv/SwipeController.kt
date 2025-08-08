package ru.iqchannels.sdk.ui.rv

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Canvas
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ru.iqchannels.sdk.Log
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.ui.ChatMessagesAdapter

class SwipeController(private val swipeListener: SwipeListener) : ItemTouchHelper.Callback() {

	private var swipeBack = false

	override fun getMovementFlags(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder
	): Int {
		val position = viewHolder.bindingAdapterPosition

		if (position == RecyclerView.NO_POSITION) {
			return makeMovementFlags(0, 0)
		}

		val chatMessage = (recyclerView.adapter as ChatMessagesAdapter).getItem(position)

		return if (!chatMessage.System && !chatMessage.AutoGreeting) {
			makeMovementFlags(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.LEFT)
		} else {
			makeMovementFlags(0, 0) // Отключение свайпа влево
		}
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
		val itemView = viewHolder.itemView
		val itemWidth = itemView.width
		val maxDx = itemWidth * 0.3f
		val limitedDx = when {
			dX < -maxDx -> -maxDx  // влево, ограничиваем до -50% ширины
			dX > 0 -> 0f  // вправо, не разрешаем движение
			else -> dX  // если в пределах порога, оставляем dX как есть
		}

		if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
			recyclerView.setOnTouchListener { v: View?, event: MotionEvent ->
				swipeBack =
					event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
				if (swipeBack) {
					if (Math.abs(viewHolder.itemView.translationX) >= maxDx) {
						swipeListener.onSwiped(viewHolder.bindingAdapterPosition)
					}
				}
				false
			}
		}

		// Позиционирование иконки для свайпа влево
		if (limitedDx < 0) {
			val editIcon =
				ContextCompat.getDrawable(viewHolder.itemView.context, R.drawable.reply_32)
			val intrinsicWidth = editIcon?.intrinsicWidth ?: 0
			val intrinsicHeight = editIcon?.intrinsicHeight ?: 0

			val itemHeight = itemView.bottom - itemView.top
			val iconTop = viewHolder.itemView.top + (itemHeight - intrinsicHeight) / 2
			val iconMargin = (itemHeight - intrinsicHeight) / 2
			val iconRight = itemView.right - iconMargin
			val iconLeft = iconRight - intrinsicWidth
			val iconBottom = iconTop + intrinsicHeight

			editIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
			editIcon?.draw(c)
		}


		// Передаем ограниченное значение limitedDx в super.onChildDraw
		super.onChildDraw(
			c,
			recyclerView,
			viewHolder,
			limitedDx,
			dY,
			actionState,
			isCurrentlyActive
		)
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
