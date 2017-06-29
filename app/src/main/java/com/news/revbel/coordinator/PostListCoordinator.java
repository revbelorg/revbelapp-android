package com.news.revbel.coordinator;

import android.databinding.Observable;
import android.net.Uri;
import android.os.Environment;

import com.news.revbel.RevApplication;
import com.news.revbel.network.Progress;
import com.news.revbel.viewmodel.ArticlePostModel;
import com.news.revbel.viewmodel.BanditPostModel;
import com.news.revbel.viewmodel.CategoryListViewModel;
import com.news.revbel.viewmodel.CategoryPostListViewModel;
import com.news.revbel.viewmodel.FavoriteListViewModel;
import com.news.revbel.viewmodel.PostListViewModel;
import com.news.revbel.viewmodel.PostModel;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

@Singleton
public class PostListCoordinator {

    public interface ViewModelGetPostCallback {
        void onFailure();
        void onSuccess(PostModel postModel);
    }

    private NetworkCoordinator coordinator;

    private PostListViewModel viewModelList;

    public CategoryListViewModel categoryListViewModel = new CategoryListViewModel(Arrays.asList("287", "8"), "category");

    private Observable.OnPropertyChangedCallback downloadedCallback;

    private boolean isBanditsDownloaded;

    public PostListCoordinator(NetworkCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isUsingTor() {
        return coordinator.isUsingTor();
    }

    public PostListViewModel currentListViewModel() {
        return viewModelList;
    }

    public void setBanditsDownloaded(boolean downloaded) {
        isBanditsDownloaded = downloaded;
    }

    public boolean isBanditsDownloaded() {
        return isBanditsDownloaded;
    }

    public PostListViewModel setListViewModel(PostListViewModel.ListType type) {
        switch (type) {
            case NEWS: {
                CategoryPostListViewModel newPostList = new CategoryPostListViewModel(CategoryPostListViewModel.PostListType.AllPosts, this);

                if (downloadedCallback != null) categoryListViewModel.isDownloaded.removeOnPropertyChangedCallback(downloadedCallback);
                downloadedCallback = new Observable.OnPropertyChangedCallback() {
                    @Override
                    public void onPropertyChanged(Observable observable, int i) {
                        if (i == 1) {
                            newPostList.getNextPagePosts();
                        }
                    }
                };
                categoryListViewModel.isDownloaded.addOnPropertyChangedCallback(downloadedCallback);
                viewModelList = newPostList;
                return newPostList;
            }
            case FAVORITE: {
                FavoriteListViewModel favoriteListViewModel = new FavoriteListViewModel();
                viewModelList = favoriteListViewModel;
                return favoriteListViewModel;
            }
        }
        return null;
    }

    public PostModel getPost(int id, String type) {
        if (type.equals(ArticlePostModel.type())) {
            return getArticleById(id);
        } else if (type.equals(BanditPostModel.type())) {
            return getBanditById(id);
        }
        return null;
    }

    public ArticlePostModel getArticleById(int id) {
        return (ArticlePostModel) coordinator.getPostByIdSync(String.valueOf(id), "post");
    }

    public BanditPostModel getBanditById(int id) {
        return (BanditPostModel) coordinator.getPostByIdSync(String.valueOf(id), "bandit");
    }

    private NetworkCoordinator.NetworkCoordinatorCallback slugCallback(ViewModelGetPostCallback callback) {
        return new NetworkCoordinator.NetworkCoordinatorCallback() {
            @Override
            public void onFailure() {
                callback.onFailure();
            }

            @Override
            public void onDatabaseResponse(List items) {

            }

            @Override
            public void onNetworkResponse(List items) {
                if (items.size() > 0) {
                    PostModel postModel = (PostModel) items.get(0);
                    postModel.updateDataModel();
                    callback.onSuccess(postModel);
                } else {
                    callback.onFailure();
                }
            }
        };
    }

    public void getBanditBySlug(String slug, ViewModelGetPostCallback callback) {
        coordinator.getBanditBySlug(slug, slugCallback(callback));
    }

    public void getArticleBySlug(String slug, ViewModelGetPostCallback callback) {
        coordinator.getArticleBySlug(slug, slugCallback(callback));
    }

    public void clear() {
        RevApplication.runOnUI(() -> {
            viewModelList.items.update(Collections.emptyList());
            viewModelList.updateList();
        });
    }

    public boolean canBeDownloaded(Uri uri) {
        if (uri.getPathSegments().size() > 0) {
            String extension = uri.getLastPathSegment();
            if (extension.endsWith(".pdf") || extension.endsWith(".png") || extension.endsWith(".jpg")) {
                return true;
            }
        }
        return false;
    }

    public void downloadFile(Uri uri, Progress.ProgressListener listener, Runnable onSuccess, Runnable onError) {
        coordinator.downloadFileFromUrl(uri.toString(), getFileFromEnvironment(), uri.getLastPathSegment(), listener, new NetworkCoordinator.NetworkCoordinatorCallback() {
            @Override
            public void onFailure() {
                if (onError != null) onError.run();
            }

            @Override
            public void onDatabaseResponse(List items) {

            }

            @Override
            public void onNetworkResponse(List items) {
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static File getFileFromEnvironment() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
//
//        } else {
//            return RevApplication.getInstance().getFilesDir();
//        }
//        return null;
    }
}
