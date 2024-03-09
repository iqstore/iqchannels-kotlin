package ru.iqchannels.sdk.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import ru.iqchannels.sdk.R;
import ru.iqchannels.sdk.download.FileDownloader;

public class FileActionsChooseFragment extends BottomSheetDialogFragment {

    private static final String ARG_URL = "FileActionsChooseFragment#argUrl";
    private static final String ARG_FILE_NAME = "FileActionsChooseFragment#argFileName";
    private static final int REQUEST_STORAGE_PERMISSION = 1;
    static final String REQUEST_KEY = "FileActionsChooseFragment#requestKey";
    static final String KEY_DOWNLOAD_ID = "FileActionsChooseFragment#downloadId";
    static final String KEY_FILE_NAME = "FileActionsChooseFragment#resultFileName";

    static FileActionsChooseFragment newInstance(String url, String fileName) {
        FileActionsChooseFragment fragment = new FileActionsChooseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putString(ARG_FILE_NAME, fileName);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_BottomNavBar;
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_file_actions_choose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvOpenFile = view.findViewById(R.id.tv_open_file);
        TextView tvSaveFile = view.findViewById(R.id.tv_save_file);
        String url = getArguments().getString(ARG_URL);
        String fileName = getArguments().getString(ARG_FILE_NAME);

        tvOpenFile.setTextColor(Colors.textColor());
        tvSaveFile.setTextColor(Colors.textColor());

        tvOpenFile.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            getContext().startActivity(i);
            dismiss();
        });

        tvSaveFile.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    downloadAndFinish(url, fileName);
                } else {
                    ActivityCompat.requestPermissions(getActivity(), new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQUEST_STORAGE_PERMISSION);
                }
            } else {
                downloadAndFinish(url, fileName);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String url = getArguments().getString(ARG_URL);
                String fileName = getArguments().getString(ARG_FILE_NAME);
                downloadAndFinish(url, fileName);
            }
        }
    }

    private void downloadAndFinish(String url, String fileName) {
        Long id = FileDownloader.downloadFile(getContext(), url, fileName);
        Bundle bundle = new Bundle();
        bundle.putLong(KEY_DOWNLOAD_ID, id);
        bundle.putString(KEY_FILE_NAME, fileName);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, bundle);
        dismiss();
    }
}
