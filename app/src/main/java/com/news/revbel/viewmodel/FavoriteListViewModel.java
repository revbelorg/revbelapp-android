package com.news.revbel.viewmodel;


import com.news.revbel.coordinator.NetworkCoordinator;

import java.util.List;

public class FavoriteListViewModel extends PostListViewModel {
    public FavoriteListViewModel() {
        super();

        updateList();
    }

    @Override
    public void updateList() {
        super.updateList();
        isDownloading.set(true);
        networkCoordinator.listFavoritePosts(new NetworkCoordinator.NetworkCoordinatorCallback() {
            @Override
            public void onFailure() {
                isDownloading.set(false);
            }

            @Override
            public void onDatabaseResponse(List items) {
                isDownloading.set(false);
                FavoriteListViewModel.this.items.update(items);
            }

            @Override
            public void onNetworkResponse(List items) {

            }
        });
    }
}
