package ru.iqchannels.sdk.ui.images;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import ru.iqchannels.sdk.Log;
import ru.iqchannels.sdk.R;

public class ImagePreviewFragment extends Fragment {

    private static final String ARG_SENDER_NAME = "ImagePreviewFragment#senderName";
    private static final String ARG_DATE = "ImagePreviewFragment#date";
    private static final String ARG_IMAGE_URL = "ImagePreviewFragment#imageUrl";
    private static final String ARG_MESSAGE = "ImagePreviewFragment#message";

    public static ImagePreviewFragment newInstance(
        String senderName,
        Date date,
        String imageUrl,
        String message
    ) {
        ImagePreviewFragment fragment = new ImagePreviewFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_SENDER_NAME, senderName);
        bundle.putSerializable(ARG_DATE, date);
        bundle.putString(ARG_IMAGE_URL, imageUrl);
        bundle.putString(ARG_MESSAGE, message);
        fragment.setArguments(bundle);

        return fragment;
    }

    private boolean downloadSuccess = false;

    private final ActivityResultLauncher<String> requestStoragePermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                String imageUrl = getArguments().getString(ARG_IMAGE_URL);
                String fileName = Uri.parse(imageUrl).getLastPathSegment();
                ImageView image = getView().findViewById(R.id.iv_image);
                startDownload(image, fileName);
            }
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvDate = view.findViewById(R.id.tv_date);
        ImageButton ibBack = view.findViewById(R.id.ib_back);
        ImageButton ibSave = view.findViewById(R.id.ib_save);
        TextView tvMessage = view.findViewById(R.id.tv_message);
        ImageView image = view.findViewById(R.id.iv_image);
        ProgressBar progressBar = view.findViewById(R.id.progress_bar);

        String senderName = getArguments().getString(ARG_SENDER_NAME);
        Date msgDate = (Date) getArguments().getSerializable(ARG_DATE);
        String message = getArguments().getString(ARG_MESSAGE);
        String imageUrl = getArguments().getString(ARG_IMAGE_URL);
        String fileName = Uri.parse(imageUrl).getLastPathSegment();

        setDateText(tvDate, msgDate);
        tvName.setText(senderName);
        tvMessage.setText(message);

        Glide.with(getContext())
                .load(imageUrl)
                .error(R.drawable.placeholder_load_image)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        downloadSuccess = true;
                        return false;
                    }
                })
                .into(image);

        ibBack.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        ibSave.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                startDownload(image, fileName);
            } else {
                requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        });
    }

    private void startDownload(ImageView image, String fileName) {
        if (downloadSuccess) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) image.getDrawable();

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    return saveImage(bitmapDrawable.getBitmap(), fileName);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);
                    if (result) {
                        showSuccessMessage();
                    }
                }
            }.execute();
        }
    }

    private void setDateText(TextView tvDate, Date msgDate) {
        java.text.DateFormat dateFormat = DateFormat.getDateFormat(getContext());
        java.text.DateFormat timeFormat = DateFormat.getTimeFormat(getContext());

        String time = timeFormat.format(msgDate);
        String dateStr = dateFormat.format(msgDate);

        Date today = new Date();
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(today);
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTime(msgDate);

        boolean areSameDay = todayCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.MONTH) == msgCal.get(Calendar.MONTH) &&
                todayCal.get(Calendar.DAY_OF_MONTH) == msgCal.get(Calendar.DAY_OF_MONTH);

        if (areSameDay) {
            dateStr = getString(R.string.today);
        }

        tvDate.setText(getString(R.string.msg_date_time, dateStr, time));
    }

    private boolean saveImage(Bitmap image, String imageFileName) {
        String savedImagePath = null;
        File storageDir = new File(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString() + "/" + getContext().getPackageName()
        );
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }

        if (success) {
            File imageFile = new File(storageDir, imageFileName);

            if (imageFile.exists()) {
                return true;
            }

            savedImagePath = imageFile.getAbsolutePath();
            try {
                OutputStream fOut =  new FileOutputStream(imageFile);
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
            } catch (Exception e) {
                Log.e(this.getClass().getName(), e.getMessage());
                return false;
            }

            // Add the image to the system gallery
            galleryAddPic(savedImagePath);
            return true;
        }

        return false;
    }

    private void showSuccessMessage() {
        Toast.makeText(getContext(), getString(R.string.image_saved), Toast.LENGTH_LONG).show(); // to make this working, need to manage coroutine, as this execution is something off the main thread
    }

    private void galleryAddPic(String imagePath) {
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(imagePath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            getContext().sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
            Log.e(this.getClass().getName(), e.getMessage());
        }
    }
}
