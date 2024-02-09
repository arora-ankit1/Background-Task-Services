package com.example.downloaderdemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
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

    public static final int VIEW_IMAGE_NOTIFICATION_ID = 2;
    private static final String VIEW_CHANNEL = "view_channel";

    private static final String BROADCAST_CHANNEL = "broadcast_channel";

    public static final int BROADCAST_IMAGE_NOTIFICATION_ID = 3;
    private String dimension = "";

    public static final String DOWNLOAD_SUCCESS_BROADCAST = "com.example.downloaderdemo.IMAGE_DOWNLOADED";
    IntentFilter intentFilter;
    ImageDownloadReceiver mReceiver;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.et_img_url);
        downloadButton = findViewById(R.id.goButton);
        progressBar = findViewById(R.id.progress_horizontal);
        imageView = findViewById(R.id.iv_downloaded_file);
        greetings = findViewById(R.id.tv_greetings);
        mContext = this;
        BackgroundUtils.mContext = this;
        workManager = WorkManager.getInstance(getApplicationContext());
        // creating request channel
        createViewNotificationChannel();
        createBroadcastNotificationChannel();
        mReceiver = new ImageDownloadReceiver();
        intentFilter = new IntentFilter(DOWNLOAD_SUCCESS_BROADCAST);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dimension = editText.getText().toString();
                greetings.setText("Enter Dimensions");
//                downloadTask = new DownloadTask(progressBar, imageView);
//                downloadTask.execute(dimension);
                // Checking Notification Permission before notifying
                checkPermissions();
//                scheduleDownloadWorker(dimension);
            }
        });
    }


    private void checkPermissions() {
        if (areNotificationPermissionsGranted()) {
            // Proceed with showing the notification
            scheduleDownloadWorker(dimension);
        } else {
            // Request notification permissions
            requestNotificationPermissions();
        }
    }

    private boolean areNotificationPermissionsGranted() {
        // Check if the app has the necessary notification permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return NotificationManagerCompat.from(this).areNotificationsEnabled();
        }
        return true; // Notifications are always enabled pre-API 23
    }

    private void requestNotificationPermissions() {
        // Show a dialog asking the user to enable notifications
        new AlertDialog.Builder(this)
                .setTitle("Enable Notifications")
                .setMessage("To download files, please enable notifications for this app.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Open app settings to allow the user to enable notifications
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, 123);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getBaseContext(), "No Notifications will be delivered", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123) {
            // Check again if the user granted notification permissions after returning from settings
            if (areNotificationPermissionsGranted()) {
                scheduleDownloadWorker(dimension);
            }
        } else if (requestCode == 124) {
            // open camera success
        } else if (requestCode == 125) {
            // open mail success
        }
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

//                    createPendingIntent(BackgroundUtils.fileUri);
                    sendImageBroadcast(getApplicationContext(), BackgroundUtils.fileUri);
//                    showNotificationWithButton(getApplicationContext());

                    Toast.makeText(imageView.getContext(), "DownloadSuccessful URI", Toast.LENGTH_LONG).show();
                } else if (workInfo.getOutputData().hasKeyWithValueOfType(BackgroundUtils.FAILURE_ERROR_MESSAGE, String.class)) {
                    String errorMessage = workInfo.getOutputData().getString(BackgroundUtils.FAILURE_ERROR_MESSAGE);
                    greetings.setText(errorMessage);
                    Toast.makeText(imageView.getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }

            }
        });
    }


    private void createViewNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    VIEW_CHANNEL,
                    "View Image Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for image view notifications");
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createBroadcastNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    BROADCAST_CHANNEL,
                    "Broadcast Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for Broadcast notifications");
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createPendingIntent(Uri uri) {

        // Create a PendingIntent to open the gallery when the user clicks the notification

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Build a bitmap image of the downloaded image to show in notification icon
        Bitmap bitmap = BitmapFactory.decodeByteArray(BackgroundUtils.imageData, 0, BackgroundUtils.imageData.length);

        // Build the notification
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), VIEW_CHANNEL)
                .setContentTitle("View Image")
                .setContentText("View the downloaded image")
                .setContentInfo("Lets view the image")
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setLargeIcon(bitmap)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .build();

        // Create the Pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                8,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        notification.contentIntent = pendingIntent;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        // Verify if the user has granted notification permission or not
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissions();
        }
        notificationManager.notify(VIEW_IMAGE_NOTIFICATION_ID, notification);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(mReceiver, intentFilter);
    }

    private void sendImageBroadcast(Context context, Uri imageUri) {
        Intent broadcastIntent = new Intent(DOWNLOAD_SUCCESS_BROADCAST);
//        broadcastIntent.setAction();
        broadcastIntent.putExtra("imageUri", imageUri.toString());
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(broadcastIntent);
//        sendBroadcast(broadcastIntent);
    }


    public void showNotificationWithButton(Context context) {
        // Create PendingIntent for the broadcast button
        Intent broadcastIntent = new Intent(DOWNLOAD_SUCCESS_BROADCAST);
        broadcastIntent.putExtra("imageUri", BackgroundUtils.fileUri.toString());
        PendingIntent broadcastPendingIntent = PendingIntent.getBroadcast(context, 0, broadcastIntent, PendingIntent.FLAG_IMMUTABLE);
        Bitmap bitmap = BitmapFactory.decodeByteArray(BackgroundUtils.imageData, 0, BackgroundUtils.imageData.length);

        // Create notification with button
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, BROADCAST_CHANNEL)
                .setContentTitle("Image Broadcast")
                .setContentText("Your Image is downloaded successfully")
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setLargeIcon(bitmap)
                .addAction(R.drawable.baseline_notifications_active_24, "Open Image", broadcastPendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissions();
        }
        notificationManager.notify(BROADCAST_IMAGE_NOTIFICATION_ID, builder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadTask != null || workManager != null) {
            downloadTask.cancel(true);
            workManager.cancelAllWork();
        }
        LocalBroadcastManager.getInstance(this.getApplicationContext()).unregisterReceiver(mReceiver);
    }
}