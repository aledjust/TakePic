package com.aledgroup.takepic.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.aledgroup.takepic.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageSaver {

    private String directoryName = "images";
    private Context context;
    private Uri getUriImage;

    public ImageSaver(Context context) {
        this.context = context;
    }

    public ImageSaver setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
        return this;
    }

    public Uri getGetUriImage() {
        return getUriImage;
    }

    public void setGetUriImage(Uri getUriImage) {
        this.getUriImage = getUriImage;
    }

    public void save(Bitmap bitmapImage) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(createFile());
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File createFile() {

        //File directory = context.getDir(directoryName, Context.MODE_PRIVATE);
        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                directoryName);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(GlobalConfig.TAG, "Oops! Failed create "
                        + directoryName + " directory");
                return null;
            }
        }

        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + RandomStringUUID.GUID() + ".jpg");

        setGetUriImage(Uri.fromFile(mediaFile));
        MainActivity.fileUri = Uri.fromFile(mediaFile);
        return mediaFile;
    }

    public Bitmap load() {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(createFile());
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}