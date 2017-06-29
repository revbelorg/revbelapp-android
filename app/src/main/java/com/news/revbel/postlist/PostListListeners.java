package com.news.revbel.postlist;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import com.news.revbel.MainActivity;
import com.news.revbel.viewmodel.PostListViewModel;

public class PostListListeners {
    private PostListViewModel postListViewModel;

    interface ItemCellClickedCallback {
        void onReadMoreButtonClicked(View view, int position);
    }

    public PostListListeners(PostListViewModel postListViewModel) {
        this.postListViewModel = postListViewModel;
    }

    public final ItemCellClickedCallback itemCallback = new ItemCellClickedCallback() {
        @Override
        public void onReadMoreButtonClicked(View view, int position) {
            Context context = view.getContext();
            MainActivity activity = null;

            while (context instanceof ContextWrapper) {
                if (context instanceof MainActivity) {
                    activity = (MainActivity)context;
                }
                context = ((ContextWrapper)context).getBaseContext();
            }

            if (activity != null) {
                activity.openFullPost(postListViewModel.items.get(position), true);
            }
        }
    };
}
