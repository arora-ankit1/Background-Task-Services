package com.example.downloaderdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.downloaderdemo.BackgroundWorker.BackgroundUtils;
import com.example.downloaderdemo.BackgroundWorker.DownloadWorker;
import com.google.android.material.textfield.TextInputEditText;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText editText;
    private Button downloadButton;
    private ProgressBar progressBar;
    private ImageView imageView;
    private TextView greetings;
    private DownloadTask downloadTask;

    private WorkRequest downloadRequest;
    private Worker downloadWorker;
    private WorkManager workManager;
    private Constraints constraints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.et_img_url);
        downloadButton = findViewById(R.id.goButton);
        progressBar = findViewById(R.id.progress_horizontal);
        imageView = findViewById(R.id.iv_downloaded_file);
        greetings = findViewById(R.id.tv_greetings);
        workManager = WorkManager.getInstance(getApplicationContext());

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String dimension = editText.getText().toString();
//                downloadTask = new DownloadTask(progressBar, imageView);
//                downloadTask.execute(dimension);
                scheduleDownloadWorker(dimension);
            }
        });
    }

    private void scheduleDownloadWorker(String dimension) {
        Data inputData = new Data.Builder()
                .putString(BackgroundUtils.INPUT_DATA, dimension)
                .build();

        constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
//                        .setRequiresCharging(true)
                .setRequiresStorageNotLow(true)
//                        .setRequiresDeviceIdle(true)
                .build();
        downloadRequest = new OneTimeWorkRequest.Builder(DownloadWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build();


        workManager.enqueue(downloadRequest);
        setDownloadImageObserver();
    }

    private void setDownloadImageObserver() {
        workManager.getWorkInfoByIdLiveData(downloadRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo.getOutputData().hasKeyWithValueOfType(BackgroundUtils.SUCCESS_IMAGE_DOWNLOADED_URI, String.class)) {
                    Uri fileUri = Uri.parse(workInfo.getOutputData().getString(BackgroundUtils.SUCCESS_IMAGE_DOWNLOADED_URI));
                    Log.d("FILE_URI", "onChanged: fileUri= " + fileUri);
                    greetings.setText(getResources().getString(R.string.yay_success));
                    Glide.with(imageView).load(BackgroundUtils.fileUri).into(imageView);
                    Toast.makeText(imageView.getContext(), "DownloadSuccessful URI", Toast.LENGTH_LONG).show();
                } else if (workInfo.getOutputData().hasKeyWithValueOfType(BackgroundUtils.FAILURE_ERROR_MESSAGE, String.class)) {
                    String errorMessage = workInfo.getOutputData().getString(BackgroundUtils.FAILURE_ERROR_MESSAGE);
                    greetings.setText(errorMessage);
                    Toast.makeText(imageView.getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }

    }
}