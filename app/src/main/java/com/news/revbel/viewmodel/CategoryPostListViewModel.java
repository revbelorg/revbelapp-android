package com.news.revbel.viewmodel;

import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.coordinator.PostListCoordinator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryPostListViewModel extends PostListViewModel {
    public PostListCoordinator coordinator;

    public interface ReloadListCallback {
        void onDataLoaded();
        void onNetworkLoaded();
    }

    public enum PostListType {
        AllPosts {
            public String networkParams() {
                return "author=1,2";
            }
            public boolean hasCategories() { return true; }
        },
        FreeNews {
            public String networkParams() {
                return "author=3";
            }
        };

        public String networkParams() { return null; }
        public boolean hasCategories() { return false; }
    }

    public final PostListType postListType;

    private String categoryId;

    private int currentPage;

    public CategoryPostListViewModel(PostListType type, PostListCoordinator coordinator) {
        super();

        currentPage = 1;
        this.postListType = type;
        this.coordinator = coordinator;

    }

    public void changeCategoryId(String categoryId, ReloadListCallback callback) {
        if (!categoryId.equals(this.categoryId)) {
            this.categoryId = categoryId;
            reloadPagePosts(callback);
        }
    }

    public void updateFirstPosts() {
        getPostsForPage(1, false, null);
    }

    public void getNextPagePosts() {
        final int oldCurrentPage = currentPage;
        getPostsForPage(currentPage, true, () -> currentPage = oldCurrentPage + 1);
    }

    private void reloadPagePosts(ReloadListCallback callback) {
        currentPage = 1;
        isDownloading.set(true);
        networkCoordinator.getPostsForPage(1, "reload", categoryId, postListType, new NetworkCoordinator.NetworkCoordinatorCallback() {
            @Override
            public void onFailure() {
                isDownloading.set(false);
            }

            @Override
            public void onDatabaseResponse(List items) {
                mergePosts(items, 1, true, true);
                if (callback != null) {
                    callback.onDataLoaded();
                }
                currentPage = 2;
            }

            @Override
            public void onNetworkResponse(List items) {
                mergePosts(items, 1, false, true);
                currentPage = 2;
                isDownloading.set(false);
                if (callback != null) {
                    callback.onNetworkLoaded();
                }
            }
        });
    }

    private void getPostsForPage(int page, boolean clipItems, Runnable callback)
    {
        isDownloading.set(true);
        networkCoordinator.getPostsForPage(page, "reload", categoryId, postListType, new NetworkCoordinator.NetworkCoordinatorCallback() {
            @Override
            public void onFailure() {
                isDownloading.set(false);
            }

            @Override
            public void onDatabaseResponse(List items) {
                mergePosts(items, page, false, clipItems);
                if (callback != null) {
                    callback.run();
                }
            }

            @Override
            public void onNetworkResponse(List items) {
                mergePosts(items, page, false, clipItems);
                if (callback != null) {
                    callback.run();
                }
                isDownloading.set(false);
            }
        });
    }

    private void mergePosts(List<PostModel> posts, int page, boolean clearItems, boolean clipItems) {
        ArrayList<PostModel> overallItems = new ArrayList<>();

        if (!clearItems) {
            overallItems.addAll(items);

            ArrayList<PostModel> addItems = new ArrayList<>(posts);
            for (PostModel addItem : posts) {
                for (int i = 0; i < items.size(); i++) {
                    PostModel item = items.get(i);
                    if (item.getId() == addItem.getId()) {
                        overallItems.set(i, addItem);
                        addItem.mergeItem(item);

                        addItems.remove(addItem);
                        break;
                    }
                }
            }
            for (PostModel item : addItems) {
                item.updateDataModel();
            }
            overallItems.addAll(addItems);
        } else {
            for (PostModel item : posts) {
                item.updateDataModel();
            }
            overallItems.addAll(posts);
        }

        Collections.sort(overallItems, (postModel, t1) -> t1.createdAt.compareTo(postModel.createdAt));

        if (clipItems) {
            if (page * 10 < overallItems.size()) {
                overallItems.subList(Math.min(page * 10, overallItems.size()), overallItems.size()).clear();
            }
        }
        items.update(overallItems);
    }

    @Override
    public void updateList() {
        super.updateList();
        reloadPagePosts(null);
    }
}
