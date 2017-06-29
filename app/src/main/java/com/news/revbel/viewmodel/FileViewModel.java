package com.news.revbel.viewmodel;

import android.databinding.BaseObservable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableInt;

import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.database.FileDataModel;
import com.news.revbel.network.Network;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;


public class FileViewModel extends BaseObservable {
    @Inject
    NetworkCoordinator networkCoordinator;

    ListedFilesViewModel owner;

    public int id;
    public String localURL;
    public String remoteURL;
    public String bookTitle;
    public String pageOwner;

    public String imageURL;
    public float imageHeightAspect;

    private FileDataModel dataModel;
    final public ObservableBoolean isDownloading = new ObservableBoolean();
    final public ObservableBoolean isDownloaded = new ObservableBoolean();
    final public ObservableInt progress = new ObservableInt();

    public FileViewModel(int id, String bookTitle, String remoteURL, String imageURL, float imageHeightAspect, String pageOwner, ListedFilesViewModel model) {
        RevApplication.getComponent().inject(this);
        this.id = id;
        this.bookTitle = bookTitle;
        this.pageOwner = pageOwner;
        this.owner = model;
        this.imageURL = imageURL;
        this.imageHeightAspect = imageHeightAspect;

        URL url;

        try {
            url = new URL(remoteURL);
        } catch (MalformedURLException e) {
            try {
                url = new URL("https://"+ Network.revbelUrl + remoteURL);
            }
            catch (MalformedURLException e2) {
                return;
            }
        }
        this.remoteURL = url.toString();

        RevApplication.runOnUI(() -> {
            updateFileDownloaded();
            createData();
        });

    }

    public FileViewModel(FileDataModel fileDataModel, ListedFilesViewModel model) {
        RevApplication.getComponent().inject(this);
        this.id = fileDataModel.id;
        this.bookTitle = fileDataModel.bookTitle;
        this.remoteURL = fileDataModel.remoteURL;
        this.pageOwner = fileDataModel.pageOwner;
        this.localURL = fileDataModel.localURL;
        this.imageURL = fileDataModel.imageURL;
        this.imageHeightAspect = fileDataModel.imageHeightAspect;
        this.owner = model;

        dataModel = fileDataModel;
        updateFileDownloaded();
    }

    private void createData() {
        dataModel = new FileDataModel(this);
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(realm1 -> realm1.copyToRealmOrUpdate(dataModel), realm::close, error -> realm.close());
    }

    private void updateFileDownloaded() {
        String baseName = remoteURL.substring(remoteURL.lastIndexOf('/')+1, remoteURL.length());
        File downloadFolder = PostListCoordinator.getFileFromEnvironment();

        localURL = owner.getPath() + baseName;

        File book = new File(downloadFolder,  localURL);

        if (book.exists()) {
            isDownloaded.set(true);
            progress.set(100);
        }
    }

    public void downloadFile() {
        if (!isDownloading.get()) {
            isDownloading.set(true);
            networkCoordinator.downloadFileFromUrl(remoteURL, PostListCoordinator.getFileFromEnvironment(), localURL, (bytesRead, contentLength, done) -> {
                final int processPercent = Math.round((float) bytesRead / contentLength * 100);

                progress.set(processPercent);
            }, new NetworkCoordinator.NetworkCoordinatorCallback() {
                @Override
                public void onFailure() {
                    isDownloading.set(false);
                    isDownloaded.set(false);
                }

                @Override
                public void onDatabaseResponse(List items) {

                }

                @Override
                public void onNetworkResponse(List items) {
                    isDownloaded.set(true);
                    isDownloading.set(false);
                }
            });
        }
    }

    public static String insertBeforeExt(String insertString, String path) {
        return path.replace(".", insertString + ".");
    }

    public static String fileExt(String localURL) {
        String url = localURL;

        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") + 1);
            if (ext.contains("%")) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.contains("/")) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }
}
