/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ru.iqchannels.sdk.Log;
import ru.iqchannels.sdk.R;
import ru.iqchannels.sdk.app.Callback;
import ru.iqchannels.sdk.app.Cancellable;
import ru.iqchannels.sdk.app.IQChannels;
import ru.iqchannels.sdk.app.IQChannelsListener;
import ru.iqchannels.sdk.app.MessagesListener;
import ru.iqchannels.sdk.lib.InternalIO;
import ru.iqchannels.sdk.schema.ChatEvent;
import ru.iqchannels.sdk.schema.ChatMessage;
import ru.iqchannels.sdk.schema.ClientAuth;
import ru.iqchannels.sdk.ui.images.ImagePreviewFragment;
import ru.iqchannels.sdk.ui.rv.SwipeController;
import ru.iqchannels.sdk.ui.widgets.ReplyMessageView;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 24/01/2017.
 */
public class ChatFragment extends Fragment {
    private static final String TAG = "iqchannels";
    private static final int SEND_FOCUS_SCROLL_THRESHOLD_PX = 300;

    private static final int REQUEST_CAMERA_OR_GALLERY = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;

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
    @Nullable private Cancellable iqchannelsListenerCancellable;

    // Messages
    private boolean messagesLoaded;
    @Nullable private Cancellable messagesRequest;
    @Nullable private Cancellable moreMessagesRequest;

    // Auth layout
    private RelativeLayout authLayout;

    // Signup layout
    private LinearLayout signupLayout;
    private EditText signupText;
    private Button signupButton;
    private TextView signupError;

    // Chat layout
    private RelativeLayout chatLayout;

    // Message views
    private ProgressBar progress;
    private SwipeRefreshLayout refresh;
    private ChatMessagesAdapter adapter;
    private RecyclerView recycler;

    // Send views
    private EditText sendText;
    private ImageButton attachButton;
    private ImageButton sendButton;

    private ReplyMessageView clReply;

    // Camera and gallery
    @Nullable private File cameraTempFile;

    private BroadcastReceiver onDownloadComplete = null;

    private ChatMessage replyingMessage = null;

    public ChatFragment() {
        iqchannels = IQChannels.instance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Auth views.
        authLayout = (RelativeLayout) view.findViewById(R.id.authLayout);

        // Login views.
        signupLayout = (LinearLayout) view.findViewById(R.id.signupLayout);
        signupText = (EditText) view.findViewById(R.id.signupName);
        signupButton = (Button) view.findViewById(R.id.signupButton);
        clReply = view.findViewById(R.id.reply);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });
        signupError = (TextView) view.findViewById(R.id.signupError);

        // Chat.
        chatLayout = (RelativeLayout) view.findViewById(R.id.chatLayout);

        // Messages.
        progress = (ProgressBar) view.findViewById(R.id.messagesProgress);

        refresh = (SwipeRefreshLayout) view.findViewById(R.id.messagesRefresh);
        refresh.setEnabled(false);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshMessages();
            }
        });

        adapter = new ChatMessagesAdapter(iqchannels, view, new ItemClickListener());

        recycler = view.findViewById(R.id.messages);
        recycler.setAdapter(adapter);
        recycler.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                maybeScrollToBottomOnKeyboardShown(bottom, oldBottom);
            }
        });

        SwipeController swipeController = new SwipeController(position -> {
            ChatMessage chatMessage = adapter.getItem(position);
            replyingMessage = chatMessage;
            clReply.showReplyingMessage(chatMessage);
            clReply.post(this::maybeScrollToBottomOnNewMessage);
        });
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeController);
        itemTouchHelper.attachToRecyclerView(recycler);

        clReply.setCloseBtnClickListener(v -> {
            hideReplying();
        });

        // Send.
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
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getChildFragmentManager().setFragmentResultListener(
            FileActionsChooseFragment.REQUEST_KEY,
            this,
            (requestKey, bundle) -> {

                long downloadID = bundle.getLong(FileActionsChooseFragment.KEY_DOWNLOAD_ID);
                String fileName = bundle.getString(FileActionsChooseFragment.KEY_FILE_NAME);

                handleDownload(downloadID, fileName);
            }
        );
    }

    @Override
    public void onDestroy() {
        if (onDownloadComplete != null) {
            getContext().unregisterReceiver(onDownloadComplete);
        }

        super.onDestroy();
    }

    private void updateViews() {
        authLayout.setVisibility(
                iqchannels.getAuth() == null && iqchannels.getAuthRequest() != null
                        ? View.VISIBLE : View.GONE);

        signupLayout.setVisibility(
                iqchannels.getAuth() == null && iqchannels.getAuthRequest() == null
                        ? View.VISIBLE : View.GONE);
        signupButton.setEnabled(iqchannels.getAuthRequest() == null);
        signupText.setEnabled(iqchannels.getAuthRequest() == null);

        chatLayout.setVisibility(iqchannels.getAuth() != null ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        iqchannelsListenerCancellable = iqchannels.addListener(new IQChannelsListener() {
            @Override
            public void authenticating() {
                signupError.setText("");
                updateViews();
            }

            @Override
            public void authComplete(ClientAuth auth) {
                signupError.setText("");
                loadMessages();
                updateViews();
            }

            @Override
            public void authFailed(Exception e) {
                signupError.setText(String.format("Ошибка: %s", e.getLocalizedMessage()));
                updateViews();
            }
        });

        if (iqchannels.getAuth() != null) {
            loadMessages();
        }
        updateViews();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.iqchannelsListenerCancellable != null) {
            this.iqchannelsListenerCancellable.cancel();
            this.iqchannelsListenerCancellable = null;
        }

        clearMessages();
        clearMoreMessages();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQUEST_CAMERA_OR_GALLERY:
                boolean isCamera = intent == null;

                if (!isCamera) {
                    String action = intent.getAction();
                    isCamera = MediaStore.ACTION_IMAGE_CAPTURE.equals(action);
                }

                if (!isCamera) {
                    Uri uri = intent.getData();
                    isCamera = uri == null;
                }

                if (isCamera) {
                    onCameraResult(resultCode);
                } else {
                    onGalleryResult(resultCode, intent);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showAttachChooser(true);
            } else {
                showAttachChooser(false);
            }
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

    // Signup

    private void signup() {
        String name = signupText.getText().toString();
        if (name.length() < 3) {
            signupError.setText("Ошибка: длина имени должна быть не менее 3-х символов.");
            return;
        }

        signupError.setText("");
        iqchannels.signup(name);
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

            @Override
            public void messageDeleted(ChatMessage message) {
                ChatFragment.this.messageDeleted(message);
            }

            @Override
            public void eventTyping(ChatEvent event) {
                ChatFragment.this.eventTyping(event);
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

    private void messageDeleted(ChatMessage message) {
        if (messagesRequest == null) {
            return;
        }
        adapter.deleted(message);
    }

    private void eventTyping(ChatEvent event) {
        adapter.typing(event);
        maybeScrollToBottomOnNewMessage();
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
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(getActivity(), new String[] {
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
            }  else {
                ActivityCompat.requestPermissions(getActivity(), new String[] {
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
            }
            return;
        }

        showAttachChooser(true);
    }

    private void showAttachChooser(boolean withCamera) {
        // Try to create a camera intent.
        Intent cameraIntent = null;
        if (withCamera) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    File tmpDir = getActivity().getExternalCacheDir();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        tmpDir = Environment.getExternalStorageDirectory();
                    }

                    File tmp = File.createTempFile("image", ".jpg", tmpDir);
                    tmp.deleteOnExit();

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmp));
                    intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(FLAG_GRANT_WRITE_URI_PERMISSION);

                    cameraTempFile = tmp;
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

    private void onGalleryResult(int resultCode, final Intent intent) {
        if (resultCode != RESULT_OK || intent == null) {
            Log.i(TAG, String.format(
                    "onGalleryResult: Did not pick an image, activity result=%d", resultCode));
            return;
        }

        Log.i(TAG, "onGalleryResult: Started processing a file from the gallery");
        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... params) {
                try {
                    final Uri uri = intent.getData();

                    ContentResolver resolver = getActivity().getContentResolver();
                    MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                    String mtype = resolver.getType(uri);
                    String ext = mimeTypeMap.getExtensionFromMimeType(mtype);

                    final File file = createGalleryTempFile(uri, ext);
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

                showConfirmDialog(file);
            }
        }.execute();
    }

    private void showConfirmDialog(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.chat_send_file_confirmation)
                .setMessage(file.getName())
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    Long replyToMessageId = null;
                    if (replyingMessage != null) {
                        replyToMessageId = replyingMessage.Id;
                    }
                    iqchannels.sendFile(file, replyToMessageId);
                    hideReplying();
                })
                .setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    private void hideReplying() {
        clReply.setVisibility(View.GONE);
        replyingMessage = null;
    }

    private File createGalleryTempFile(Uri uri, String ext) throws IOException {
        String filename = getGalleryFilename(uri);

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

        if (filename.length() < 3) {
            filename = "file-" + filename;
        }

        File file = File.createTempFile(filename, "." + ext, getActivity().getCacheDir());
        file.deleteOnExit();
        return file;
    }

    private String getGalleryFilename(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String path = uri.getPath();

            int i = path.lastIndexOf("/");
            if (i > -1) {
                path = path.substring(i+1);
            }
            return path;
        }

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
        showConfirmDialog(file);
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
        Long replyToMessageId = null;
        if (replyingMessage != null) {
            replyToMessageId = replyingMessage.Id;
        }
        iqchannels.send(text, replyToMessageId);
        hideReplying();
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

    private void handleDownload(long downloadID, String fileName) {
        if (downloadID > 0) {
            onDownloadComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                        Log.d(TAG, "received: " + downloadId);
                        if (downloadID != downloadId) return;

                        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);

                        Cursor cursor = downloadManager.query(query);
                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            int status = cursor.getInt(columnIndex);

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                // Загрузка завершена успешно
                                Log.d(TAG, "SUCCESS");
                                Toast.makeText(context, getString(R.string.file_saved_success_msg, fileName), Toast.LENGTH_LONG).show();
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                                int reason = cursor.getInt(columnReason);
                                // Обработка ошибки загрузки
                                Log.d(TAG, "FAILED");
                                Toast.makeText(context, getString(R.string.file_saved_fail_msg, fileName), Toast.LENGTH_LONG).show();
                            }
                        }
                        cursor.close();
                        getContext().unregisterReceiver(this);
                    }
                }
            };

            getContext().registerReceiver(
                    onDownloadComplete,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            );
        }
    }

    private class ItemClickListener implements ChatMessagesAdapter.ItemClickListener {
        @Override
        public void onFileClick(String url, String fileName) {
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.add(FileActionsChooseFragment.newInstance(url, fileName), null);
            fragmentTransaction.commit();
        }

        @Override
        public void onImageClick(ChatMessage message) {
            String senderName = message.User.DisplayName;
            Date date = message.Date;
            String msg = message.Text;
            String imageUrl = message.File.imageUrl;

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            ImagePreviewFragment fragment = ImagePreviewFragment.newInstance(
                senderName, date, imageUrl, msg
            );
            transaction.replace(((ViewGroup)getView().getParent()).getId(), fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}
