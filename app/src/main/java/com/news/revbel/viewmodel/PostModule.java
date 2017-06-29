package com.news.revbel.viewmodel;

import android.support.annotation.NonNull;

import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.coordinator.PostListCoordinator;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PostModule {
    @Provides
    @NonNull
    CategoryPostListViewModel providePostListViewModel(PostListCoordinator coordinator) {
        return (CategoryPostListViewModel)coordinator.setListViewModel(PostListViewModel.ListType.NEWS);
    }

    @Provides
    @NonNull
    FavoriteListViewModel provideFavoritePostListViewModel(PostListCoordinator coordinator) {
        return (FavoriteListViewModel)coordinator.setListViewModel(PostListViewModel.ListType.FAVORITE);
    }

    @Provides
    @NonNull
    @Singleton
    PostListCoordinator providePostListCoordinator(NetworkCoordinator coordinator) {
        return new PostListCoordinator(coordinator);
    }
}
