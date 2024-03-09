package ru.iqchannels.sdk.ui

import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.schema.Action

class ActionsAdapter internal constructor(
	private val clickListener: ClickListener
) : RecyclerView.Adapter<ActionsAdapter.ButtonsVH>() {

	private var items: List<Action>? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonsVH {
		val btn = Button(parent.context)
		val lp = MarginLayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)
		btn.layoutParams = lp
		btn.setBackgroundResource(R.drawable.bg_action_btn)
		btn.setTextColor(ContextCompat.getColor(parent.context, R.color.dark_text_color))
		btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
		btn.isAllCaps = false
		btn.elevation = 0f
		btn.stateListAnimator = null

		return ButtonsVH(btn)
	}

	override fun onBindViewHolder(holder: ButtonsVH, position: Int) {
		val item = items?.get(position) ?: return
		holder.bind(item)
		holder.itemView.setOnClickListener { clickListener.onClick(item) }
	}

	override fun getItemCount(): Int {
		return items?.size ?: 0
	}

	fun setItems(items: List<Action>?) {
		this.items = items
		notifyDataSetChanged()
	}

	class ButtonsVH(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
		fun bind(item: Action) {
			val btn = itemView as Button
			btn.text = item.Title
		}
	}

	internal interface ClickListener {
		fun onClick(item: Action?)
	}
}
