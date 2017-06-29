package com.news.revbel.viewmodel;

import android.databinding.ObservableBoolean;
import android.databinding.ObservableList;

import com.news.revbel.BR;
import com.news.revbel.R;
import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.postlist.FragmentPostViewAdapter;

import javax.inject.Inject;

import me.tatarka.bindingcollectionadapter2.ItemBinding;
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList;

import static com.news.revbel.utilities.Utilities.compareObjects;


public class PostListViewModel {
    public enum ListType {
        NEWS, FAVORITE
    }

    @Inject
    NetworkCoordinator networkCoordinator;

    public final ObservableBoolean hasOpenedDetails = new ObservableBoolean(false);

    public final FragmentPostViewAdapter adapter = new FragmentPostViewAdapter();

    public final ItemBinding singleItemView = ItemBinding.of(BR.item, R.layout.cell_post);
    private final DiffObservableList.Callback<PostModel> diffCallback = new DiffObservableList.Callback<PostModel>() {
        @Override
        public boolean areItemsTheSame(PostModel oldItem, PostModel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(PostModel oldItem, PostModel newItem) {
            boolean sameTitle = compareObjects(oldItem.title, newItem.title);
            boolean sameExcerpt = compareObjects(oldItem.excerpt, newItem.excerpt);
            boolean samePicture = compareObjects(oldItem.featuredMediaUrl, newItem.featuredMediaUrl);
            boolean sameCreatedAt = compareObjects(oldItem.createdAt, newItem.createdAt);
            boolean sameHeight = oldItem.featuredImageHeight == newItem.featuredImageHeight;
            return sameTitle && sameExcerpt && samePicture && sameCreatedAt && sameHeight;
        }
    };
    public final DiffObservableList<PostModel> items = new DiffObservableList<>(diffCallback);

    final public ObservableBoolean isDownloading = new ObservableBoolean();

    PostListViewModel() {
        RevApplication.getComponent().inject(this);

        items.addOnListChangedCallback(new ObservableList.OnListChangedCallback<ObservableList<PostModel>>() {
            @Override
            public void onChanged(ObservableList<PostModel> postModels) {

            }

            @Override
            public void onItemRangeChanged(ObservableList<PostModel> postModels, int i, int i1) {
                for (int a = i; a < i + i1; a++) {
                    postModels.get(a).updateDataModel();
                }
            }

            @Override
            public void onItemRangeInserted(ObservableList<PostModel> postModels, int i, int i1) {

            }

            @Override
            public void onItemRangeMoved(ObservableList<PostModel> postModels, int i, int i1, int i2) {

            }

            @Override
            public void onItemRangeRemoved(ObservableList<PostModel> postModels, int i, int i1) {

            }
        });
    }

    public void updateList() {

    }
}
