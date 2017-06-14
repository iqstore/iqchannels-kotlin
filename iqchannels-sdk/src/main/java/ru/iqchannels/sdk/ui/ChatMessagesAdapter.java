/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ru.iqchannels.sdk.R;
import ru.iqchannels.sdk.app.IQChannels;
import ru.iqchannels.sdk.schema.ChatMessage;
import ru.iqchannels.sdk.schema.UploadedFile;
import ru.iqchannels.sdk.schema.User;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 24/01/2017.
 */
class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder> {
    private static final int GROUP_TIME_DELTA_MS = 60000;

    private final IQChannels iqchannels;
    private final View rootView;
    private final java.text.DateFormat dateFormat;
    private final java.text.DateFormat timeFormat;
    private final List<ChatMessage> messages;

    ChatMessagesAdapter(IQChannels iqchannels, final View rootView) {
        this.iqchannels = iqchannels;
        this.rootView = rootView;

        dateFormat = DateFormat.getDateFormat(rootView.getContext());
        timeFormat = DateFormat.getTimeFormat(rootView.getContext());
        messages = new ArrayList<>();
    }

    void clear() {
        messages.clear();
        notifyDataSetChanged();
    }

    void loaded(List<ChatMessage> messages) {
        this.messages.clear();
        this.messages.addAll(messages);
        notifyDataSetChanged();
    }

    void loadedMore(List<ChatMessage> moreMessages) {
        messages.addAll(0, moreMessages);
        notifyItemRangeInserted(0, moreMessages.size());
    }

    void received(ChatMessage message) {
        messages.add(message);

        if (messages.size() > 1) {
            notifyItemChanged(messages.size() - 2);
        }
        notifyItemInserted(messages.size() - 1);
    }

    void sent(ChatMessage message) {
        messages.add(message);

        if (messages.size() > 1) {
            notifyItemChanged(messages.size() - 2);
        }
        notifyItemInserted(messages.size() - 1);
    }

    void cancelled(ChatMessage message) {
        int i = messages.indexOf(message);
        if (i < 0) {
            return;
        }

        messages.remove(i);
        notifyItemRemoved(i);;
    }

    void updated(ChatMessage message) {
        int i = getIndexByMessage(message);
        if (i < 0 ) {
            return;
        }

        messages.set(i, message);
        notifyItemChanged(i);
    }

    private int getIndexByMessage(ChatMessage message) {
        if (message.My) {
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage m = messages.get(i);
                if (m.My && m.LocalId == message.LocalId) {
                    return i;
                }
            }
        }

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (m.Id == message.Id) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.chat_message, parent, false);
        return new ViewHolder(this, contactView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        // Day
        if (isNewDay(position)) {
            holder.date.setText(dateFormat.format(message.Date));
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.date.setVisibility(View.GONE);
        }

        // Message
        if (message.My) {
            onBindMyMessage(holder, position, message);
        } else {
            onBindOtherMessage(holder, position, message);
        }

        iqchannels.markAsRead(message);
    }

    private void onBindMyMessage(ViewHolder holder, int position, ChatMessage message) {
        boolean groupEnd = isGroupEnd(position);

        holder.my.setVisibility(View.VISIBLE);
        holder.other.setVisibility(View.GONE);

        // Time
        if (groupEnd) {
            if (message.Sending) {
                holder.mySending.setVisibility(View.VISIBLE);
                holder.myDate.setVisibility(View.INVISIBLE);
                holder.myReceived.setVisibility(View.GONE);
                holder.myRead.setVisibility(View.GONE);

            } else {
                holder.mySending.setVisibility(View.GONE);
                holder.myFlags.setVisibility(View.VISIBLE);

                holder.myDate.setText(message.Date != null ? timeFormat.format(message.Date) : "");
                holder.myDate.setVisibility(message.Date != null ? View.VISIBLE : View.GONE);
                holder.myReceived.setVisibility(message.Received ? View.VISIBLE : View.GONE);
                holder.myRead.setVisibility(message.Read ? View.VISIBLE : View.GONE);
            }
        } else {
            holder.mySending.setVisibility(View.GONE);
            holder.myFlags.setVisibility(View.GONE);
        }

        // Message
        if (message.Upload != null) {
            holder.myText.setVisibility(View.GONE);
            holder.myImageFrame.setVisibility(View.GONE);
            holder.myUpload.setVisibility(View.VISIBLE);

            holder.myUploadProgress.setMax(100);
            holder.myUploadProgress.setProgress(message.UploadProgress);

            if (message.UploadExc != null) {
                holder.myUploadProgress.setVisibility(View.GONE);

                holder.myUploadError.setVisibility(View.VISIBLE);
                holder.myUploadError.setText(message.UploadExc.getLocalizedMessage());
                holder.myUploadCancel.setVisibility(View.VISIBLE);
                holder.myUploadRetry.setVisibility(View.VISIBLE);
            } else {
                holder.myUploadError.setVisibility(View.GONE);
                holder.myUploadRetry.setVisibility(View.GONE);

                holder.myUploadProgress.setVisibility(View.VISIBLE);
                holder.myUploadCancel.setVisibility(View.VISIBLE);
            }

        } else if (message.File != null) {
            holder.myUpload.setVisibility(View.GONE);

            UploadedFile file = message.File;
            String imageUrl = file.ImagePreviewUrl;
            if (imageUrl != null) {
                int[] size = computeImageSizeFromFile(file);

                holder.myText.setVisibility(View.GONE);
                holder.myImageFrame.setVisibility(View.VISIBLE);
                holder.myImageFrame.getLayoutParams().width = size[0];
                holder.myImageFrame.getLayoutParams().height = size[1];
                holder.myImageFrame.requestLayout();

                Picasso.with(holder.myImageFrame.getContext())
                        .load(imageUrl)
                        .into(holder.myImageSrc);
            } else {

                holder.myImageFrame.setVisibility(View.GONE);
                holder.myText.setVisibility(View.VISIBLE);
                holder.myText.setAutoLinkMask(0);
                holder.myText.setMovementMethod(LinkMovementMethod.getInstance());
                holder.myText.setText(makeFileLink(file));
            }

        } else {
            holder.myImageFrame.setVisibility(View.GONE);
            holder.myUpload.setVisibility(View.GONE);
            holder.myText.setVisibility(View.VISIBLE);
            holder.myText.setAutoLinkMask(Linkify.ALL);
            holder.myText.setText(message.Text);
        }
    }

    private void onBindOtherMessage(ViewHolder holder, int position, ChatMessage message) {
        boolean groupStart = isGroupStart(position);
        boolean groupEnd = isGroupEnd(position);

        holder.my.setVisibility(View.GONE);
        holder.other.setVisibility(View.VISIBLE);

        // Name and avatar
        User user = message.User;
        if (groupStart && user != null) {
            String name = user.DisplayName != null ? user.DisplayName : "";
            String letter = name.isEmpty() ? "" : name.substring(0, 1);

            holder.otherName.setText(name);
            holder.otherName.setVisibility(View.VISIBLE);
            holder.otherAvatar.setVisibility(View.VISIBLE);

            String avatarUrl = user.AvatarUrl;
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // Avatar image
                holder.otherAvatarText.setVisibility(View.GONE);

                holder.otherAvatarImage.setVisibility(View.VISIBLE);
                Picasso.with(holder.otherAvatarImage.getContext())
                        .load(avatarUrl)
                        .placeholder(R.drawable.avatar_placeholder)
                        .into(holder.otherAvatarImage);

            } else {
                // Avatar circle with a letter inside
                holder.otherAvatarImage.setVisibility(View.GONE);

                holder.otherAvatarText.setVisibility(View.VISIBLE);
                holder.otherAvatarText.setText(letter);
                holder.otherAvatarText.setBackgroundColor(Colors.paletteColor(letter));
            }

        } else {
            holder.otherName.setVisibility(View.GONE);
            holder.otherAvatar.setVisibility(View.INVISIBLE);
        }

        // Time
        if (groupEnd && message.Date != null) {
            holder.otherDate.setText(timeFormat.format(message.Date));
            holder.otherDate.setVisibility(View.VISIBLE);
        } else {
            holder.otherDate.setVisibility(View.GONE);
        }

        // Message
        UploadedFile file = message.File;
        if (file != null) {
            String imageUrl = file.ImagePreviewUrl;
            if (imageUrl != null) {
                int[] size = computeImageSizeFromFile(file);

                holder.otherText.setVisibility(View.GONE);
                holder.otherImageFrame.setVisibility(View.VISIBLE);
                holder.otherImageFrame.getLayoutParams().width = size[0];
                holder.otherImageFrame.getLayoutParams().height = size[1];
                holder.otherImageFrame.requestLayout();

                Picasso.with(holder.otherImageFrame.getContext())
                        .load(imageUrl)
                        .into(holder.otherImageSrc);
            } else {

                holder.otherImageFrame.setVisibility(View.GONE);
                holder.otherText.setVisibility(View.VISIBLE);
                holder.otherText.setAutoLinkMask(0);
                holder.otherText.setMovementMethod(LinkMovementMethod.getInstance());
                holder.otherText.setText(makeFileLink(file));
            }

        } else {
            holder.otherImageFrame.setVisibility(View.GONE);
            holder.otherText.setVisibility(View.VISIBLE);
            holder.otherText.setAutoLinkMask(Linkify.ALL);
            holder.otherText.setText(message.Text);
        }
    }

    private boolean isNewDay(int position) {
        if (position == 0) {
            return true;
        }

        ChatMessage curMessage = messages.get(position);
        ChatMessage prevMessage = messages.get(position - 1);
        if (curMessage.Date == null || prevMessage.Date == null) {
            return true;
        }

        Calendar cur = Calendar.getInstance();
        Calendar prev = Calendar.getInstance();
        cur.setTime(curMessage.Date);
        prev.setTime(prevMessage.Date);

        return cur.get(Calendar.YEAR) != prev.get(Calendar.YEAR)
                || cur.get(Calendar.MONTH) != prev.get(Calendar.MONTH)
                || cur.get(Calendar.DAY_OF_MONTH) != prev.get(Calendar.DAY_OF_MONTH);
    }

    private boolean isGroupStart(int position) {
        if (position == 0) {
            return true;
        }

        ChatMessage cur = messages.get(position);
        ChatMessage prev = messages.get(position - 1);
        return cur.My != prev.My
                || !objectEquals(cur.UserId, prev.UserId)
                || (cur.CreatedAt - prev.CreatedAt) > GROUP_TIME_DELTA_MS;
    }

    private boolean isGroupEnd(int position) {
        if (position == messages.size() - 1) {
            return true;
        }

        ChatMessage cur = messages.get(position);
        ChatMessage next = messages.get(position + 1);
        return cur.My != next.My
                || !objectEquals(cur.UserId, next.UserId)
                || (next.CreatedAt - cur.CreatedAt) > GROUP_TIME_DELTA_MS;
    }

    private int[] computeImageSizeFromFile(UploadedFile file) {
        if (file == null) {
            return new int[]{0, 0};
        }

        int imageWidth = file.ImageWidth != null ? file.ImageWidth : 0;
        int imageHeight = file.ImageHeight != null ? file.ImageHeight : 0;
        return computeImageSize(imageWidth, imageHeight);
    }

    private int[] computeImageSize(int imageWidth, int imageHeight) {
        if (imageWidth == 0 || imageHeight == 0) {
            return new int[]{0, 0};
        }

        int width = (Math.min(rootView.getWidth(), rootView.getHeight()) * 3) / 5;
        int height = (imageHeight * width) / imageWidth;
        if (height > (width * 2)) {
            height = width * 2;
        }

        return new int[]{width, height};
    }

    private Spanned makeFileLink(UploadedFile file) {
        if (file == null) {
            return null;
        }
        if (file.Url == null) {
            return null;
        }

        String html = "<a href=\"" + TextUtils.htmlEncode(file.Url) + "\">"
                + TextUtils.htmlEncode(file.Name) + "</a>";
        return Html.fromHtml(html);
    }

    private void onUploadCancelClicked(int position) {
        ChatMessage message = messages.get(position);
        iqchannels.cancelUpload(message);
    }

    private void onUploadRetryClicked(int position) {
        ChatMessage message = messages.get(position);
        iqchannels.sendFile(message);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ChatMessagesAdapter adapter;

        private final TextView date;

        // My
        private final LinearLayout my;
        private final TextView myText;

        private final LinearLayout myUpload;
        private final ProgressBar myUploadProgress;
        private final TextView myUploadError;
        private final Button myUploadCancel;
        private final Button myUploadRetry;

        private final FrameLayout myImageFrame;
        private final ImageView myImageSrc;

        private final LinearLayout myFlags;
        private final TextView myDate;
        private final ProgressBar mySending;
        private final TextView myReceived;
        private final TextView myRead;

        // Other
        private final LinearLayout other;
        private final FrameLayout otherAvatar;
        private final ImageView otherAvatarImage;
        private final TextView otherAvatarText;
        private final TextView otherName;
        private final TextView otherText;
        private final FrameLayout otherImageFrame;
        private final ImageView otherImageSrc;
        private final TextView otherDate;

        ViewHolder(final ChatMessagesAdapter adapter, final View itemView) {
            super(itemView);
            this.adapter = adapter;

            date = (TextView) itemView.findViewById(R.id.date);

            // My
            my = (LinearLayout) itemView.findViewById(R.id.my);
            myText = (TextView) itemView.findViewById(R.id.myText);

            myUpload = (LinearLayout) itemView.findViewById(R.id.myUpload);
            myUploadProgress = (ProgressBar) itemView.findViewById(R.id.myUploadProgress);
            myUploadError = (TextView) itemView.findViewById(R.id.myUploadError);
            myUploadCancel = (Button) itemView.findViewById(R.id.myUploadCancel);
            myUploadRetry = (Button) itemView.findViewById(R.id.myUploadRetry);

            myImageFrame = (FrameLayout) itemView.findViewById(R.id.myImageFrame);
            myImageSrc = (ImageView) itemView.findViewById(R.id.myImageSrc);

            myFlags = (LinearLayout) itemView.findViewById(R.id.myFlags);
            myDate = (TextView) itemView.findViewById(R.id.myDate);
            mySending = (ProgressBar) itemView.findViewById(R.id.mySending);
            myReceived = (TextView) itemView.findViewById(R.id.myReceived);
            myRead = (TextView) itemView.findViewById(R.id.myRead);

            // Other
            other = (LinearLayout) itemView.findViewById(R.id.other);
            otherAvatar = (FrameLayout) itemView.findViewById(R.id.otherAvatar);
            otherAvatarImage = (ImageView) itemView.findViewById(R.id.otherAvatarImage);
            otherAvatarText = (TextView) itemView.findViewById(R.id.otherAvatarText);
            otherName = (TextView) itemView.findViewById(R.id.otherName);
            otherText = (TextView) itemView.findViewById(R.id.otherText);
            otherImageFrame = (FrameLayout) itemView.findViewById(R.id.otherImageFrame);
            otherImageSrc = (ImageView) itemView.findViewById(R.id.otherImageSrc);
            otherDate = (TextView) itemView.findViewById(R.id.otherDate);

            myUploadCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.onUploadCancelClicked(getAdapterPosition());
                }
            });
            myUploadRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.onUploadRetryClicked(getAdapterPosition());
                }
            });
        }
    }

    private static boolean objectEquals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
