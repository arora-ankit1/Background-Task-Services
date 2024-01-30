package com.example.downloaderdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText editText;
    private Button downloadButton;
    private ProgressBar progressBar;
    private ImageView imageView;

    private DownloadTask downloadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.et_img_url);
        downloadButton = findViewById(R.id.goButton);
        progressBar = findViewById(R.id.progress_horizontal);
        imageView = findViewById(R.id.iv_downloaded_file);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String dimension = editText.getText().toString();
             downloadTask =  new DownloadTask(progressBar,imageView);
             downloadTask.execute(dimension);
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