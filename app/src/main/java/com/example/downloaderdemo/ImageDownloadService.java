package com.example.downloaderdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ImageDownloadService extends Service {

    private static final long INTERVAL = 5000; // 5 seconds
    private Handler handler = new Handler();
    public static final String TAG = ImageDownloadService.class.getName();
    private ImageDownloadListener listener;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "image_download_service_channel";
    Notification notification;

    private Runnable downloadTask = new Runnable() {
        @Override
        public void run() {
            // Call download image method in MainActivity
            Log.d(TAG, "run: listener is null = " + (listener != null));
            if (listener != null) {
                listener.onDownloadRequested("200");
            }

            // Schedule the next download task after INTERVAL
            handler.postDelayed(this, INTERVAL);
        }
    };



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        // Start the download task when the service is started
        createNotification();
//        handler.post(downloadTask);
        if (listener != null) {
            listener.onDownloadRequested("200");
        }

        // Return START_STICKY to ensure the service keeps running
        return START_STICKY;
    }

    private void createNotification() {
        // Create a notification channel (for devices running Android 8.0 or higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Image Download Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setContentTitle("Downloading Images")
                .setContentText("Your images are downloading...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notification = builder.build();
        // Display the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the download task when the service is destroyed
        handler.removeCallbacks(downloadTask);
    }

    public interface ImageDownloadListener {


        void onDownloadRequested(String dimension);
    }

    public void setImageDownloadListener(ImageDownloadListener listener) {
        this.listener = listener;
    }
}
