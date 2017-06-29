package com.news.revbel.viewmodel;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableList;

import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.utilities.SimpleRecyclerViewAdapter;
import com.news.revbel.utilities.Utilities;
import com.news.revbel.BR;
import com.news.revbel.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import me.tatarka.bindingcollectionadapter2.ItemBinding;
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList;

import static com.news.revbel.utilities.Utilities.compareObjects;


public class ReplyListViewModel {
    @Inject
    NetworkCoordinator networkCoordinator;

    private final DiffObservableList.Callback<ReplyModel> diffCallback = new DiffObservableList.Callback<ReplyModel>() {
        @Override
        public boolean areItemsTheSame(ReplyModel oldItem, ReplyModel newItem) {
            return oldItem.replyId == newItem.replyId;
        }

        @Override
        public boolean areContentsTheSame(ReplyModel oldItem, ReplyModel newItem) {
            boolean sameTitle = compareObjects(oldItem.authorName, newItem.authorName);
            boolean sameContent = compareObjects(oldItem.textContent, newItem.textContent);
            return sameTitle && sameContent && (oldItem.replyId == newItem.replyId) && (oldItem.postId == newItem.parentId) && (oldItem.parentId == newItem.parentId);
        }
    };

    public final SimpleRecyclerViewAdapter adapter = new SimpleRecyclerViewAdapter();
    private int postId = -1;
    public final ObservableBoolean isDownloading = new ObservableBoolean(false);
    public final DiffObservableList<ReplyModel> items = new DiffObservableList<>(diffCallback);

    public final ItemBinding singleItemView = ItemBinding.of(BR.item, R.layout.cell_reply);
    private Comparator<ReplyModel> sorting = (r1, r2) -> r1.date.compareTo(r2.date);

    ReplyListViewModel(List<ReplyModel> list, int postId) {
        this.postId = postId;

        addRepliesAndSort(list);
        RevApplication.getComponent().inject(this);
    }

    private List<ReplyModel> findChildReplies(List<ReplyModel> replies, ReplyModel currentReply) {
        ArrayList<ReplyModel> newSubList = new ArrayList<>();
        for (ReplyModel subModel : replies) {
            if (currentReply.replyId == subModel.parentId) {
                newSubList.add(subModel);
                newSubList.addAll(findChildReplies(replies, subModel));
            }
        }
        return newSubList;
    }

    private List<ReplyModel> sortReplies(ArrayList<ReplyModel> allReplies) {
        List<ReplyModel> dynamicReplies = new ArrayList<>(allReplies);
        ArrayList<ReplyModel> sortedList = new ArrayList<>();

        while (dynamicReplies.size() > 0) {
            ReplyModel model = dynamicReplies.get(0);
            if (model.replyId != 0) {
                sortedList.add(model);
                List<ReplyModel> newSubList = findChildReplies(allReplies, model);
                sortedList.addAll(newSubList);
                allReplies.removeAll(newSubList);

                dynamicReplies.remove(0);
                dynamicReplies.removeAll(newSubList);
            } else {
                dynamicReplies.remove(0);
            }
        }

        return sortedList;
    }

    void addRepliesAndSort(List<ReplyModel> list) {
        if (list.size() > 0) {
            ArrayList<ReplyModel> newList = new ArrayList<>(list);
            newList.addAll(items);

            Collections.sort(newList, sorting);

            items.update(sortReplies(newList));
        }
    }

    void reloadReplies(List<ReplyModel> list) {
        ArrayList<ReplyModel> newList = new ArrayList<>(list);
        Collections.sort(newList, sorting);
        items.update(sortReplies(newList));
    }

    public void getOlderPosts(Runnable onFinish) {
        if (postId != -1 && items.size() > 0) {
            isDownloading.set(true);
            networkCoordinator.getRepliesForPost(postId, Utilities.dateISOToString(Utilities.dateBefore(items.get(0).date)), new NetworkCoordinator.NetworkCoordinatorCallback() {
                @Override
                public void onFailure() {
                    onFinish.run();
                }

                @Override
                public void onDatabaseResponse(List items) {

                }

                @Override
                public void onNetworkResponse(List items) {
                    onFinish.run();
                    isDownloading.set(false);
                    addRepliesAndSort(items);
                }
            });
        } else {
            onFinish.run();
        }
    }
}
