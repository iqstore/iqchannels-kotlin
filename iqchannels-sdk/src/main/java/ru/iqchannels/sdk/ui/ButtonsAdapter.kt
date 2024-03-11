package ru.iqchannels.sdk.ui

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.schema.SingleChoice

class ButtonsAdapter internal constructor(
	private val clickListener: ClickListener
) : RecyclerView.Adapter<ButtonsAdapter.ButtonsVH>() {

	private var items: List<SingleChoice>? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonsVH {
		val btn = Button(parent.context)
		val lp = MarginLayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)
		lp.setMargins(0, UiUtils.toPx(5), 0, 0)
		btn.layoutParams = lp
		btn.setBackgroundResource(R.drawable.bg_single_choice_btn)
		btn.setTextColor(ContextCompat.getColor(parent.context, R.color.light_text_color))
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

	fun setItems(items: List<SingleChoice>?) {
		this.items = items
		notifyDataSetChanged()
	}

	class ButtonsVH(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
		fun bind(item: SingleChoice) {
			val btn = itemView as Button
			btn.text = item.title
		}
	}

	internal interface ClickListener {
		fun onClick(item: SingleChoice)
	}
}
