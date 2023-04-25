package com.example.opusapplication.util;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

public class PermissionsUtil {

    private Activity activity;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public PermissionsUtil(Activity activity) {
        this.activity = activity;
    }

    public boolean openPermissions() {
        int permission1 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(activity)
                        .setCancelable(false)
                        .setTitle("需要讀取權限")
                        .setMessage("需要讀取權限才能連線")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
                            }
                        })
                        .show();

            return false;
        }
        return true;
    }

    public boolean checkPermissions() {
        int permission1 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

}
