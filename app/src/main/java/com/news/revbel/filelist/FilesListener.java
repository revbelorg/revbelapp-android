package com.news.revbel.filelist;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.news.revbel.MainActivity;
import com.news.revbel.viewmodel.FileViewModel;

import java.util.ArrayList;
import java.util.List;

public class FilesListener {
    private static int WRITE_STORAGE_HANDLER_CONST = 1;

    private Runnable writePermissionRunnable;
    private Runnable readPermissionRunnable;

    public void checkReadWriteDownloadFolderPermissions(Activity activity, Runnable writePermissionCallback, Runnable readPermissionCallback) {
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            writePermissionRunnable = writePermissionCallback;
        } else {
            writePermissionRunnable = null;
            if (writePermissionCallback != null)
                writePermissionCallback.run();
        }

        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                readPermissionRunnable = readPermissionCallback;
            } else {
                readPermissionRunnable = null;
                if (readPermissionCallback != null)
                    readPermissionCallback.run();
            }

        } else {
            readPermissionRunnable = null;
            if (readPermissionCallback != null)
                readPermissionCallback.run();
        }

        if (!listPermissionsNeeded.isEmpty()) {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).filesListener = this;
            }
            ActivityCompat.requestPermissions(activity, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), WRITE_STORAGE_HANDLER_CONST);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == WRITE_STORAGE_HANDLER_CONST) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        if (writePermissionRunnable != null) writePermissionRunnable.run();
                    } else {
                        writePermissionRunnable = null;
                    }
                }

                if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        if (readPermissionRunnable != null) readPermissionRunnable.run();
                    } else {
                        readPermissionRunnable = null;
                    }
                }
            }
        }
    }

    public void onBookClick(View view, FileViewModel viewModel) {
        Context context = view.getContext();
        MainActivity activity = null;
        while (context instanceof ContextWrapper) {
            if (context instanceof MainActivity) {
                activity = (MainActivity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        final MainActivity finalActivity = activity;

        if (finalActivity != null) {
            if (viewModel.isDownloaded.get()) {
                checkReadWriteDownloadFolderPermissions(finalActivity, null, () -> {
                    finalActivity.openFile(viewModel.localURL);
                });
            } else {
                checkReadWriteDownloadFolderPermissions(finalActivity, () -> {
                    finalActivity.downloadFileDialog(viewModel.bookTitle, viewModel::downloadFile);
                }, null);
            }
        }
    }
}
