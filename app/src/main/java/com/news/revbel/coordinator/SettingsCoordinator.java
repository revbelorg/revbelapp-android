package com.news.revbel.coordinator;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.news.revbel.RevApplication;
import com.news.revbel.viewmodel.AgitationListViewModel;
import com.news.revbel.viewmodel.LibraryViewModel;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;

@Singleton
public class SettingsCoordinator {
    @Inject Context appContext;
    @Inject PostListCoordinator postListCoordinator;

    public final static String NETWORK_USE_TOR_KEY = "NETWORK_USE_TOR_KEY";
    public final static String NETWORK_USE_VPN_KEY = "NETWORK_USE_VPN_KEY";
    public final static String NETWORK_USE_MIRROR_KEY = "NETWORK_USE_MIRROR_KEY";
    public final static String OPEN_INTERNAL_PDF = "OPEN_INTERNAL_PDF";
    public final static String USE_CRASHLYTICS = "USE_CRASHLYTICS";

    public SettingsCoordinator() {
        RevApplication.getComponent().inject(this);
    }

    public void init() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        sharedPref.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if (key.equals(USE_CRASHLYTICS) && sharedPreferences.getBoolean(key, false)) {
                if (!Fabric.isInitialized()) Fabric.with(appContext, new Crashlytics());
            }
        });
    }

    private void clearFolder(File folder, boolean clearRoot) {
        if (folder.exists()) {
            String[] fileNames = folder.list();
            if (fileNames != null) {
                for (String fileName : fileNames) {
                    deleteFile(new File(folder, fileName));
                }
            }
            if (clearRoot) folder.delete();
        }
    }

    private static boolean deleteFile(File file) {
        boolean deletedAll = true;
        if (file != null) {
            if (file.isDirectory()) {
                String[] children = file.list();
                for (String aChildren : children) {
                    deletedAll = deleteFile(new File(file, aChildren)) && deletedAll;
                }
            } else {
                deletedAll = file.delete();
            }
        }

        return deletedAll;
    }

    public void clearAllData() {
        Realm.getDefaultInstance().executeTransaction(realm -> realm.deleteAll());

        Glide.get(appContext).clearDiskCache();
        RevApplication.runOnUI(Glide.get(appContext)::clearMemory);
        clearFolder(appContext.getCacheDir(), false);
        clearFolder(appContext.getExternalCacheDir(), false);

        File downloadFolder = PostListCoordinator.getFileFromEnvironment();

        File directory = new File(downloadFolder, LibraryViewModel.pathToFolder());
        clearFolder(directory, true);

        directory = new File(downloadFolder, AgitationListViewModel.pathToFolder());
        clearFolder(directory, true);

        postListCoordinator.clear();
    }
}
