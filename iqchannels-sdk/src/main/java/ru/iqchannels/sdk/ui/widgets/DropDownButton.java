package ru.iqchannels.sdk.ui.widgets;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import ru.iqchannels.sdk.R;
import ru.iqchannels.sdk.schema.SingleChoice;
import ru.iqchannels.sdk.ui.UiUtils;

public class DropDownButton extends androidx.appcompat.widget.AppCompatButton {

    private SingleChoice singleChoice;

    public DropDownButton(Context context) {
        super(context);

        setBackgroundResource(R.drawable.bg_single_choice_btn_dropdown);
        setTextColor(ContextCompat.getColor(getContext(), R.color.drop_down_btn_text));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        setAllCaps(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(0f);
        }

        int vertical = UiUtils.toPx(4);
        int horizontal = UiUtils.toPx(6);
        setPadding(horizontal, vertical, horizontal, vertical);
    }

    public DropDownButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackgroundResource(R.drawable.bg_single_choice_btn_dropdown);
        setTextColor(ContextCompat.getColor(getContext(), R.color.drop_down_btn_text));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        setAllCaps(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(0f);
        }

        int vertical = UiUtils.toPx(4);
        int horizontal = UiUtils.toPx(4);
        setPadding(horizontal, vertical, horizontal, vertical);
    }

    public DropDownButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setBackgroundResource(R.drawable.bg_single_choice_btn_dropdown);
        setTextColor(ContextCompat.getColor(getContext(), R.color.drop_down_btn_text));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        setAllCaps(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(0f);
        }

        int vertical = UiUtils.toPx(4);
        int horizontal = UiUtils.toPx(4);
        setPadding(horizontal, vertical, horizontal, vertical);
    }

    public SingleChoice getSingleChoice() {
        return singleChoice;
    }

    public void setSingleChoice(SingleChoice singleChoice) {
        this.singleChoice = singleChoice;
        setText(singleChoice.title);

        ViewGroup.LayoutParams lp = new  ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            UiUtils.toPx(36)
        );

        setLayoutParams(lp);
    }
}
