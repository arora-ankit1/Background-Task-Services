package com.example.downloaderdemo.BackgroundWorker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.downloaderdemo.ApiService;
import com.example.downloaderdemo.R;
import com.example.downloaderdemo.RetrofitClientInstance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class DownloadWorker extends Worker {

    private String dimension;
    private Uri fileUri = Uri.parse("");
    private String filePath = "";
    private Context context;
    public static final String TAG = DownloadWorker.class.getName();
    private static final int NOTIFICATION_ID = 1;

    private static final String CHANNEL_ID = "download_channel";


    public DownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        createNotificationChannel();

        Result result;

        Retrofit retrofit = RetrofitClientInstance.getRetrofitInstance();


        ApiService apiService = retrofit.create(ApiService.class);
        dimension = getInputData().getString(BackgroundUtils.INPUT_DATA);
        Call<ResponseBody> call = apiService.downloadImage(dimension);
        try {
            // Set up a foreground notification
            ForegroundInfo foregroundInfo = createForegroundInfo("Connecting...", 0);
            setForegroundAsync(foregroundInfo);

            // call happens once execute is called
            Response<ResponseBody> response = call.execute();
            if (response.isSuccessful()) {
                foregroundInfo = createForegroundInfo("Downloading...", 50);
                setForegroundAsync(foregroundInfo);
                ResponseBody responseBody = response.body();
                byte[] data = responseBody.bytes();
                responseBody.close();
                saveImageToStorage(data, context);
                BackgroundUtils.imageData = data;
                // Update notification to indicate download complete
                foregroundInfo = createForegroundInfo("Download Complete", 100);
                setForegroundAsync(foregroundInfo);
                Data data1 = new Data.Builder()
                        .putString(BackgroundUtils.SUCCESS_IMAGE_DOWNLOADED_URI, fileUri.toString())
                        .putString(BackgroundUtils.SUCCESS_IMAGE_DOWNLOADED_PATH, filePath)
                        .build();
                result = Result.success(data1);
            } else {
                foregroundInfo = createForegroundInfo("Download Failed", 0);
                setForegroundAsync(foregroundInfo);
                result = Result.failure(
                        new Data.Builder()
                                .putInt(BackgroundUtils.FAILURE_ERROR_CODE, response.code())
                                .build()
                );
                Log.e("DownloadWorker", "Download failed: " + response.code());
            }
        } catch (IOException e) {
            result = Result.failure(
                    new Data.Builder()
                            .putString(BackgroundUtils.FAILURE_ERROR_MESSAGE, e.getMessage())
                            .build()
            );
            Log.e("DownloadWorker", "IOException during download: " + e.getMessage());
        }

        return result;
    }

    private ForegroundInfo createForegroundInfo(String status, int progress) {
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Image Download")
                .setContentText(status)
                .setSmallIcon(R.drawable.baseline_cloud_download_24)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setProgress(100, progress, false)
                .build();


        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }



    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for image download notifications");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }



    private void saveImageToStorage(byte[] imageData, Context context) {
        try {


            // using shared preferences to have unique name
            SharedPreferences sharedPreferences = context.getSharedPreferences("", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();


            // counter for naming
            Integer last_count = sharedPreferences.getInt(context.getString(R.string.last_saved_image_count), 1);

            // Get the public external storage directory
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            // Create a file to save the image
            File file = new File(directory, "downloaded_image_" + last_count + "_.jpg");

            // Create a FileOutputStream to write the bytes to the file
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(imageData);

            // Close the FileOutputStream
            outputStream.close();

            // MediaScanner to notify the system about the new file
            MediaScannerConnection.scanFile(
                    context,
                    new String[]{file.getAbsolutePath()},
                    new String[]{"image/jpeg"},
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            fileUri = uri;
                            BackgroundUtils.fileUri = uri;
                            filePath = path;
                        }
                    }
            );

            // storing last count for further usage
            editor.putInt(context.getString(R.string.last_saved_image_count), last_count + 1);
            editor.apply();

            // Display a toast message indicating that the image has been saved
//            Toast.makeText(context, "Image saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception
//            Toast.makeText(context, "Error saving image", Toast.LENGTH_LONG).show();
        }
    }


}
