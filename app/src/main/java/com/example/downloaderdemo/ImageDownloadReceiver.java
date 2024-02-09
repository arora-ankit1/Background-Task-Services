package com.example.downloaderdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.downloaderdemo.BackgroundWorker.BackgroundUtils;


public class ImageDownloadReceiver extends BroadcastReceiver {
    public static final String TAG = ImageDownloadReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle the broadcast to open the downloaded image
        String imageUriString = intent.getStringExtra("imageUri");
        Log.d(TAG, "onReceive: uri= "+imageUriString);
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);

            // Show a dialog with success message
            showDialog(BackgroundUtils.mContext, "Image Downloaded Successfully", imageUri);
        } else {
            // Handle null URI
            Toast.makeText(context, "Error: Image URI is null", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDialog(Context context, String message, Uri imageUri) {
        Log.d(TAG, "showDialog: uri= " + imageUri + " message= " + message);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setPositiveButton("Open Image", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Open the image
                        openImage(context, imageUri);
                    }
                })
                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Dismiss the dialog
                        dialog.dismiss();
                    }
                }).show();
//        AlertDialog dialog = builder.create();
//        dialog.show();
    }

    private void openImage(Context context, Uri imageUri) {
        // Open the image using an Intent
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setDataAndType(imageUri, "image/*");
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(viewIntent);
    }
}
