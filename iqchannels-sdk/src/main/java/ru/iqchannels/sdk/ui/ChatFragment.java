/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ru.iqchannels.sdk.R;
import ru.iqchannels.sdk.app.Callback;
import ru.iqchannels.sdk.app.Cancellable;
import ru.iqchannels.sdk.app.IQChannels;
import ru.iqchannels.sdk.app.MessagesListener;
import ru.iqchannels.sdk.lib.InternalIO;
import ru.iqchannels.sdk.schema.ChatMessage;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 24/01/2017.
 */
public class ChatFragment extends Fragment {
    private static final String TAG = "iqchannels";
    private static final int SEND_FOCUS_SCROLL_THRESHOLD_PX = 300;

    private static final int REQUEST_CAMERA_OR_GALLERY = 1;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ChatFragment.
     */
    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private final IQChannels iqchannels;

    // Messages
    private boolean messagesLoaded;
    @Nullable private Cancellable messagesRequest;
    @Nullable private Cancellable moreMessagesRequest;

    // Message views
    private ProgressBar progress;
    private SwipeRefreshLayout refresh;
    private ChatMessagesAdapter adapter;
    private RecyclerView recycler;

    // Send views
    private EditText sendText;
    private ImageButton attachButton;
    private ImageButton sendButton;

    // Camera and gallery
    @Nullable private File cameraTempFile;

    public ChatFragment() {
        iqchannels = IQChannels.instance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        setupMessageViews(view);
        setupSendViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        loadMessages();
    }

    @Override
    public void onStop() {
        super.onStop();

        clearMessages();
        clearMoreMessages();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CAMERA_OR_GALLERY:
                boolean isCamera = data == null;
                if (!isCamera) {
                    isCamera = MediaStore.ACTION_IMAGE_CAPTURE.equals(data.getAction());
                }

                if (isCamera) {
                    onCameraResult(resultCode);
                } else {
                    onGalleryResult(resultCode, data);
                }
                break;
        }
    }

    // Views

    private void setupMessageViews(View view) {
        progress = (ProgressBar) view.findViewById(R.id.messagesProgress);

        refresh = (SwipeRefreshLayout) view.findViewById(R.id.messagesRefresh);
        refresh.setEnabled(false);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshMessages();
            }
        });

        adapter = new ChatMessagesAdapter(iqchannels, view);
        recycler = (RecyclerView) view.findViewById(R.id.messages);
        recycler.setAdapter(adapter);
        recycler.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                maybeScrollToBottomOnKeyboardShown(bottom, oldBottom);
            }
        });
    }

    private void setupSendViews(View view) {
        sendText = (EditText) view.findViewById(R.id.sendText);
        sendText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    handled = true;
                }
                return handled;
            }
        });

        attachButton = (ImageButton) view.findViewById(R.id.attachButton);
        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAttachChooser();
            }
        });

        sendButton = (ImageButton) view.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            attachButton.setVisibility(View.GONE);
        }
    }

    // Messages scroll

    private void maybeScrollToBottomOnKeyboardShown(int bottom, int oldBottom) {
        if (!sendText.hasFocus()) {
            return;
        }
        if (bottom >= oldBottom) {
            return;
        }

        int extent = recycler.computeVerticalScrollExtent();
        int offset = recycler.computeVerticalScrollOffset();
        int range = recycler.computeVerticalScrollRange();
        if (range - (oldBottom - bottom) - (extent + offset) > SEND_FOCUS_SCROLL_THRESHOLD_PX) {
            return;
        }

        int count = adapter.getItemCount();
        recycler.smoothScrollToPosition(count == 0 ? 0 : count - 1);
    }

    private void maybeScrollToBottomOnNewMessage() {
        int extent = recycler.computeVerticalScrollExtent();
        int offset = recycler.computeVerticalScrollOffset();
        int range = recycler.computeVerticalScrollRange();
        if (range - (extent + offset) > SEND_FOCUS_SCROLL_THRESHOLD_PX) {
            return;
        }

        int count = adapter.getItemCount();
        recycler.smoothScrollToPosition(count == 0 ? 0 : count - 1);
    }

    // Messages

    private void clearMessages() {
        if (messagesRequest != null) {
            messagesRequest.cancel();
        }

        messagesLoaded = false;
        messagesRequest = null;
        adapter.clear();

        progress.setVisibility(View.GONE);
        refresh.setRefreshing(false);
        refresh.setEnabled(false);
    }

    private void refreshMessages() {
        if (!messagesLoaded) {
            if (messagesRequest != null) {
                refresh.setRefreshing(false);
                return;
            }

            // Load messages.
            loadMessages();
            return;
        }

        loadMoreMessages();
    }

    private void loadMessages() {
        if (messagesLoaded) {
            return;
        }
        if (messagesRequest != null) {
            return;
        }

        // Show the progress bar only when the refresh control is not active already.
        disableSend();
        progress.setVisibility(refresh.isRefreshing() ? View.GONE : View.VISIBLE);
        messagesRequest = iqchannels.loadMessages(new MessagesListener() {
            @Override
            public void messagesLoaded(List<ChatMessage> messages) {
                ChatFragment.this.messagesLoaded(messages);
            }

            @Override
            public void messagesException(Exception e) {
                ChatFragment.this.messagesException(e);
            }

            @Override
            public void messagesCleared() {
                ChatFragment.this.clearMessages();
            }

            @Override
            public void messageReceived(ChatMessage message) {
                ChatFragment.this.messageReceived(message);
            }

            @Override
            public void messageSent(ChatMessage message) {
                ChatFragment.this.messageSent(message);
            }

            @Override
            public void messageUploaded(ChatMessage message) {
                ChatFragment.this.messageUploaded(message);
            }

            @Override
            public void messageUpdated(ChatMessage message) {
                ChatFragment.this.messageUpdated(message);
            }

            @Override
            public void messageCancelled(ChatMessage message) {
                ChatFragment.this.messageCancelled(message);
            }
        });
    }

    private void messagesLoaded(List<ChatMessage> messages) {
        if (messagesRequest == null) {
            return;
        }
        messagesLoaded = true;

        enableSend();
        adapter.loaded(messages);
        recycler.scrollToPosition(messages.isEmpty() ? 0 : messages.size() - 1);

        progress.setVisibility(View.GONE);
        refresh.setRefreshing(false);
        refresh.setEnabled(true);
    }

    private void messagesException(Exception e) {
        if (messagesRequest == null) {
            return;
        }
        messagesRequest = null;

        progress.setVisibility(View.GONE);
        refresh.setRefreshing(false);
        refresh.setEnabled(true);

        showMessagesErrorAlert(e);
    }

    private void messageReceived(ChatMessage message) {
        if (messagesRequest == null) {
            return;
        }
        adapter.received(message);
        maybeScrollToBottomOnNewMessage();
    }

    private void messageSent(ChatMessage message) {
        if (messagesRequest == null) {
            return;
        }
        adapter.sent(message);
        maybeScrollToBottomOnNewMessage();
    }

    private void messageUploaded(ChatMessage message) {
        if (messagesRequest == null) {
            return;
        }
        adapter.updated(message);
        maybeScrollToBottomOnNewMessage();
    }

    private void messageCancelled(ChatMessage message) {
        if (messagesRequest == null) {
            return;
        }
        adapter.cancelled(message);
    }

    private void messageUpdated(ChatMessage message) {
        if (messagesRequest == null) {
            return;
        }
        adapter.updated(message);
    }

    // More messages

    private void clearMoreMessages() {
        if (moreMessagesRequest != null) {
            moreMessagesRequest.cancel();
        }

        moreMessagesRequest = null;
    }

    private void loadMoreMessages() {
        if (!messagesLoaded) {
            refresh.setRefreshing(false);
            return;
        }
        if (moreMessagesRequest != null) {
            return;
        }

        moreMessagesRequest = iqchannels.loadMoreMessages(new Callback<List<ChatMessage>>() {
            @Override
            public void onResult(List<ChatMessage> result) {
                moreMessagesLoaded(result);
            }

            @Override
            public void onException(Exception e) {
                moreMessagesException(e);
            }
        });
    }

    private void moreMessagesException(Exception e) {
        if (moreMessagesRequest == null) {
            return;
        }
        moreMessagesRequest = null;
        refresh.setRefreshing(false);

        showMessagesErrorAlert(e);
    }

    private void moreMessagesLoaded(List<ChatMessage> moreMessages) {
        if (moreMessagesRequest == null) {
            return;
        }
        moreMessagesRequest = null;
        refresh.setRefreshing(false);
        adapter.loadedMore(moreMessages);
    }

    // Attach

    private void showAttachChooser() {
        // Try to create a camera intent.
        Intent cameraIntent = null;
        {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    File temp = File.createTempFile("image", null, getActivity().getExternalCacheDir());
                    temp.deleteOnExit();

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(temp));
                    cameraTempFile = temp;
                }
                cameraIntent = intent;
            } catch (IOException e) {
                Log.e(TAG, String.format(
                        "showAttachChooser: Failed to create a temp file for the camera, e=%s", e));
            }
        }

        // Create a gallery intent.
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            galleryIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "audio/*", "video/*", "text/*", "application/*", "file/*"});
        }

        // Create and start an intent chooser.
        CharSequence title = getResources().getText(R.string.chat_camera_or_file);
        Intent chooser = Intent.createChooser(galleryIntent, title);
        if (cameraIntent != null) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{cameraIntent});
        }

        startActivityForResult(chooser, REQUEST_CAMERA_OR_GALLERY);
    }

    // Gallery

    private void onGalleryResult(int resultCode, final Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            Log.i(TAG, String.format(
                    "onGalleryResult: Did not pick an image, activity result=%d", resultCode));
            return;
        }

        Log.i(TAG, "onGalleryResult: Started processing a file from the gallery");
        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... params) {
                try {
                    Uri uri = data.getData();
                    File file = createGalleryTempFile(uri);

                    ContentResolver resolver = getActivity().getContentResolver();
                    InputStream in = resolver.openInputStream(uri);
                    if (in == null) {
                        Log.e(TAG, "onGalleryResult: Failed to pick a file, no input stream");
                        return null;
                    }

                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        try {
                            InternalIO.copy(in, out);
                        } finally {
                            out.close();
                        }
                    } finally {
                        in.close();
                    }
                    return file;

                } catch (IOException e) {
                    Log.e(TAG, String.format("onGalleryResult: Failed to pick a file, e=%s", e));
                    return null;
                }
            }

            @Override
            protected void onPostExecute(File file) {
                if (file == null) {
                    return;
                }

                iqchannels.sendFile(file);
            }
        }.execute();
    }

    private File createGalleryTempFile(Uri uri) throws IOException {
        String filename = getGalleryFilename(uri);
        String ext = null;
        if (filename != null) {
            int i = filename.lastIndexOf(".");
            if (i > -1) {
                ext = filename.substring(i + 1);
                filename = filename.substring(0, i - 1);
            }
        } else {
            filename = "file";
            String mimeType = getActivity().getContentResolver().getType(uri);
            ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }

        File file = File.createTempFile(filename, "." + ext, getActivity().getCacheDir());
        file.deleteOnExit();
        return file;
    }

    private String getGalleryFilename(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            return uri.getLastPathSegment();
        }
        if (!scheme.equals("content")) {
            return null;
        }

        String[] projection = {MediaStore.Video.Media.TITLE};
        ContentResolver resolver = getActivity().getContentResolver();

        Cursor cursor = resolver.query(uri, projection, null, null, null);
        if (cursor == null) {
            return null;
        }

        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } finally {
            cursor.close();
        }
    }

    // Camera

    private void onCameraResult(int resultCode) {
        if (resultCode != RESULT_OK || cameraTempFile == null) {
            Log.i(TAG, String.format(
                    "onCameraResult: Did not capture a photo, activity result=%d", resultCode));
            if (cameraTempFile != null) {
                cameraTempFile.delete();
            }
            return;
        }

        File file;
        try {
            // Create a dst file.
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String app = getResources().getString(R.string.app_name);
            if (!app.isEmpty()) {
                dir = new File(dir, app);
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date());
            file = File.createTempFile(timestamp, ".jpg", dir);
            InternalIO.copy(cameraTempFile, file);

            //noinspection ResultOfMethodCallIgnored
            cameraTempFile.delete();
            cameraTempFile = null;
        } catch (IOException e) {
            Log.e(TAG, String.format("showCamera: Failed to save a captured file, error=%s", e));
            return;
        }

        addCameraPhotoToGallery(file);
        iqchannels.sendFile(file);
    }

    private void addCameraPhotoToGallery(File file) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        scanIntent.setData(contentUri);
        getActivity().sendBroadcast(scanIntent);
    }

    // Send

    private void enableSend() {
        sendText.setEnabled(true);
        attachButton.setEnabled(true);
        sendButton.setEnabled(true);
    }

    private void disableSend() {
        sendText.setEnabled(false);
        attachButton.setEnabled(false);
        sendButton.setEnabled(false);
    }

    private void sendMessage() {
        String text = sendText.getText().toString();
        sendText.setText("");
        iqchannels.send(text);
    }

    // Error alerts

    private void showMessagesErrorAlert(Exception e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.chat_failed_to_load_messages)
                .setNeutralButton(R.string.ok, null);

        if (e != null) {
            builder.setMessage(e.toString());
        } else {
            builder.setMessage(R.string.unknown_exception);
        }
        builder.show();
    }
}
