package com.news.revbel.viewmodel;

import android.databinding.ObservableBoolean;

import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.coordinator.PostListCoordinator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class BanditsListViewModel {
    @Inject NetworkCoordinator networkCoordinator;
    @Inject PostListCoordinator coordinator;
    public interface BanditsViewModelCallback {
        void onUpdated();
        void onFinishUpdating();
    }

    private BanditsViewModelCallback onUpdateCallback;
    private String lastFilter;

    private ArrayList<PostModel> items = new ArrayList<>();
    private ArrayList<PostModel> searchItems = new ArrayList<>();

    private int currentPage = 1;
    private ObservableBoolean isLoading = new ObservableBoolean(false);

    public ObservableBoolean isLoading() {
        return isLoading;
    }

    public BanditsListViewModel(BanditsViewModelCallback onUpdateCallback) {
        RevApplication.getComponent().inject(this);
        this.onUpdateCallback = onUpdateCallback;
    }

    public boolean isSearching() {
        return lastFilter != null;
    }

    public void filterSearchResult(String filter) {
        if (lastFilter == null || !lastFilter.equals(filter)) {
            lastFilter = filter;
            searchItems.clear();

            if (filter != null && !filter.isEmpty()) {
                for (PostModel item : items) {
                    BanditPostModel postModel = (BanditPostModel) item;
                    String[] words = filter.split("\\s+");
                    boolean has = false;
                    for (String word : words) {
                        has = has || Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE).matcher(postModel.title).find();
                        has = has || Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE).matcher(postModel.getMetaContacted()).find();
                    }

                    if (has) {
                        searchItems.add(postModel);
                    }
                }
            }
            onUpdateCallback.onUpdated();
        }
    }

    public List<PostModel> getItems() {
        if (lastFilter != null && !lastFilter.isEmpty()) {
            return searchItems;
        } else {
            return items;
        }
    }

    public void loadBandits(boolean forceReload) {
        if (!isLoading.get()) {
            getBanditsFromDatabse();
            isLoading.set(true);
            if (!coordinator.isBanditsDownloaded() || forceReload) {
                onUpdateCallback.onUpdated();
                getBanditsForPage(1);
            } else {
                isLoading.set(false);
                onUpdateCallback.onFinishUpdating();
            }
        }
    }

    private void getNextPage() {
        currentPage += 1;
        getBanditsForPage(currentPage);
    }

    private void getBanditsFromDatabse() {
        List<PostModel> list = networkCoordinator.getBanditsFromDatabase();
        mergePosts(list);
    }

    private void getBanditsForPage(int page) {
        networkCoordinator.getBanditsForPage(page, "bandits_reload", new NetworkCoordinator.NetworkCoordinatorCallback() {
            @Override
            public void onFailure() {
            }

            @Override
            public void onDatabaseResponse(List items) {
            }

            @Override
            public void onNetworkResponse(List items) {
                mergePosts(items);
                onUpdateCallback.onUpdated();

                if (items.size() == 10) {
                    getNextPage();
                } else {
                    isLoading.set(false);
                    coordinator.setBanditsDownloaded(true);
                    onUpdateCallback.onFinishUpdating();
                }
            }
        });
    }

    private void mergePosts(List<PostModel> posts) {
        ArrayList<PostModel> overallItems = new ArrayList<>();

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
        overallItems.addAll(addItems);

        Collections.sort(overallItems, (postModel, t1) -> t1.createdAt.compareTo(postModel.createdAt));

        items.clear();
        items.addAll(overallItems);
        if (lastFilter != null) filterSearchResult(lastFilter);
    }
}
