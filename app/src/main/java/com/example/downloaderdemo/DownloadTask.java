package com.example.downloaderdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class DownloadTask extends AsyncTask<String, Integer, byte[]> {
    private ProgressBar progressBar;
    ImageView imageView;

    public DownloadTask(ProgressBar progressBar, ImageView imageView) {
        this.progressBar = progressBar;
        this.imageView = imageView;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
    }

    @Override
    protected byte[] doInBackground(/*Jaggered Array notation*/ String... params) {

        String dimension = params[0];

        /*if (!dimension.endsWith("/")) {
            dimension = dimension + "/";
        }*/

        Retrofit retrofit = RetrofitClientInstance.getRetrofitInstance();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.downloadImage(dimension);

        try {
            // call happens once execute is called
            Response<ResponseBody> response = call.execute();
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                byte[] data = responseBody.bytes();
                responseBody.close();
                return data;
            } else {
                Log.e("DownloadTask", "Download failed: " + response.code());
            }
        } catch (IOException e) {
            Log.e("DownloadTask", "IOException during download: " + e.getMessage());
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        // values[0] contains the current progress, values[1] contains the total file size
        int progress = values[0];
        int totalSize = values[1];

        Log.d("PROGRESS_UPDATE", "onProgressUpdate: progress= " + progress + " totalSize= " + totalSize);

        // Calculate the percentage and update the progress bar
        int percentage = (int) (((float) progress / totalSize) * 100);
        progressBar.setProgress(percentage);
    }

    @Override
    protected void onPostExecute(byte[] result) {
        if (result != null) {
            // Convert the byte array to a Bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(result, 0, result.length);

            // Save the image to shared storage
            saveImageToStorage(result);

            // Update the ImageView with the downloaded image
            imageView.setImageBitmap(bitmap);

            // Hide progressBar visibility
            progressBar.setVisibility(View.GONE);

        }
    }

    private void saveImageToStorage(byte[] imageData) {
        try {
            // getting image context
            Context context = imageView.getContext();

            // using shared preferences to have unique name
            SharedPreferences sharedPreferences = context.getSharedPreferences( "",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();


            // counter for naming
            Integer last_count = sharedPreferences.getInt(context.getString(R.string.last_saved_image_count), 1);

            // Get the public external storage directory
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            // Create a file to save the image
            File file = new File(directory, "downloaded_image_"+last_count+"_.jpg");

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
                    null
            );

            // storing last count for further usage
            editor.putInt(context.getString(R.string.last_saved_image_count),last_count+1);
            editor.apply();

            // Display a toast message indicating that the image has been saved
            Toast.makeText(context, "Image saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception
            Toast.makeText(imageView.getContext(), "Error saving image", Toast.LENGTH_LONG).show();
        }
    }
}