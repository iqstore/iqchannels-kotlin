package ru.iqchannels.sdk.ui;

import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.iqchannels.sdk.R;
import ru.iqchannels.sdk.schema.SingleChoice;

public class ButtonsAdapter extends RecyclerView.Adapter<ButtonsAdapter.ButtonsVH> {

    private List<SingleChoice> items;

    private final ClickListener clickListener;

    ButtonsAdapter(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ButtonsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Button btn = new Button(parent.getContext());
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, UiUtils.toPx(4), 0, 0);
        btn.setLayoutParams(lp);
        btn.setBackgroundResource(R.drawable.bg_single_choice_btn);
        btn.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.light_text_color));
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        btn.setAllCaps(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btn.setElevation(0f);
        }
        return new ButtonsVH(btn);
    }

    @Override
    public void onBindViewHolder(@NonNull ButtonsVH holder, int position) {
        SingleChoice item = items.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> {
            clickListener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<SingleChoice> items) {
        this.items = items;
        this.notifyDataSetChanged();
    }

    static class ButtonsVH extends RecyclerView.ViewHolder {

        ButtonsVH(View itemView) {
            super(itemView);
        }

        public void bind(SingleChoice item) {
            Button btn = (Button) itemView;
            btn.setText(item.title);
        }
    }

    interface ClickListener {

        void onClick(SingleChoice item);
    }
}
