package ru.iqchannels.sdk.ui.widgets;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import ru.iqchannels.sdk.R;
import ru.iqchannels.sdk.app.IQChannels;
import ru.iqchannels.sdk.schema.ChatMessage;

public class ReplyMessageView extends ConstraintLayout {

    private final TextView tvSenderName;
    private final TextView tvText;
    private final TextView tvFileName;
    private final ImageView imageView;
    private final ImageButton ibClose;

    private final IQChannels iqchannels;

    public ReplyMessageView(Context context) {
        super(context);
        inflate(context, R.layout.layout_reply_to_message, this);
        tvSenderName = findViewById(R.id.tvSenderName);
        tvText = findViewById(R.id.tv_text);
        tvFileName = findViewById(R.id.tvFileName);
        imageView = findViewById(R.id.iv_image);
        ibClose = findViewById(R.id.ib_close);
        iqchannels = IQChannels.instance();
    }

    public ReplyMessageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.layout_reply_to_message, this);
        tvSenderName = findViewById(R.id.tvSenderName);
        tvText = findViewById(R.id.tv_text);
        tvFileName = findViewById(R.id.tvFileName);
        imageView = findViewById(R.id.iv_image);
        ibClose = findViewById(R.id.ib_close);
        iqchannels = IQChannels.instance();
    }

    public ReplyMessageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.layout_reply_to_message, this);
        tvSenderName = findViewById(R.id.tvSenderName);
        tvText = findViewById(R.id.tv_text);
        tvFileName = findViewById(R.id.tvFileName);
        imageView = findViewById(R.id.iv_image);
        ibClose = findViewById(R.id.ib_close);
        iqchannels = IQChannels.instance();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ReplyMessageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        inflate(context, R.layout.layout_reply_to_message, this);
        tvSenderName = findViewById(R.id.tvSenderName);
        tvText = findViewById(R.id.tv_text);
        tvFileName = findViewById(R.id.tvFileName);
        imageView = findViewById(R.id.iv_image);
        ibClose = findViewById(R.id.ib_close);
        iqchannels = IQChannels.instance();
    }

    public void showReplyingMessage(ChatMessage message) {
        setVisibility(View.VISIBLE);
        if (message.User != null) {
            tvSenderName.setText(message.User.DisplayName);
        }

        if (message.Text != null && !message.Text.isEmpty()) {
            tvText.setText(message.Text);
        } else {
            tvText.setVisibility(View.GONE);
        }

        if (message.File != null) {
            String imageUrl = message.File.ImagePreviewUrl;

            if (imageUrl != null) {
                tvFileName.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                iqchannels.picasso(getContext())
                        .load(imageUrl)
                        .into(imageView);
            } else {
                imageView.setVisibility(View.GONE);
                tvFileName.setVisibility(View.VISIBLE);
                tvFileName.setText(message.File.Name);
            }
        } else {
            tvFileName.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
        }

        ibClose.setOnClickListener(v -> {
            setVisibility(View.GONE);
        });
    }

}
