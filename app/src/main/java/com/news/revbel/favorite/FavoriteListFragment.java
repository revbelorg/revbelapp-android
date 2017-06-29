package com.news.revbel.favorite;

import android.app.Activity;
import android.content.Context;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.news.revbel.MainActivity;
import com.news.revbel.R;
import com.news.revbel.RevApplication;
import com.news.revbel.databinding.FragmentFavoriteListBinding;
import com.news.revbel.postlist.FragmentPostViewAdapter;
import com.news.revbel.postlist.PostListListeners;
import com.news.revbel.utilities.ControlActivityInterface;
import com.news.revbel.viewmodel.FavoriteListViewModel;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class FavoriteListFragment extends Fragment {
    @BindView(R.id.recycle_list) RecyclerView recyclerView;
    @BindView(R.id.swipeContainer)  SwipeRefreshLayout swipeContainer;
    @BindView(R.id.nothing_label) TextView nothingToShowLabel;

    private PostListListeners listeners;
    private Unbinder unbinder;
    private ControlActivityInterface activityInterface;

    private Observable.OnPropertyChangedCallback downloadChange = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable observable, int i) {
            RevApplication.runOnUI(() -> {
                ObservableBoolean observableBoolean = (ObservableBoolean) observable;
                if (!observableBoolean.get()) {
                    swipeContainer.setRefreshing(false);
                    if (postListViewModel.items.size() == 0) {
                        nothingToShowLabel.setVisibility(View.VISIBLE);
                    } else {
                        nothingToShowLabel.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
    };

    @Inject
    FavoriteListViewModel postListViewModel;

    public FavoriteListFragment() {
    }

    public static FavoriteListFragment newInstance() {
        FavoriteListFragment fragment = new FavoriteListFragment();

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (getActivity() instanceof ControlActivityInterface) {
            activityInterface = (ControlActivityInterface) getActivity();

            activityInterface.updateControlButtonTapEvent(this, () -> {
                recyclerView.smoothScrollToPosition(0);
            });
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activityInterface != null) {
            activityInterface.onFragmentHide(this);
        }
        postListViewModel.isDownloading.removeOnPropertyChangedCallback(downloadChange);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RevApplication.getComponent().inject(this);

        listeners = new PostListListeners(postListViewModel);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentFavoriteListBinding binding = FragmentFavoriteListBinding.inflate(inflater, container, false);
        binding.setPostListViewModel(postListViewModel);
        binding.setListeners(listeners);
        binding.executePendingBindings();
        View view = binding.getRoot();

        unbinder = ButterKnife.bind(this, view);

        Activity activity = getActivity();

        swipeContainer.setOnRefreshListener(() -> {
            postListViewModel.updateList();
        });

        postListViewModel.isDownloading.addOnPropertyChangedCallback(downloadChange);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (postListViewModel.items.size() == 0) {
                    if (activity instanceof MainActivity) {
                        ((MainActivity) activity).hideControlButton(FavoriteListFragment.this);
                    }
                    return;
                }

                if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                    LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
                    int first = layoutManager.findFirstVisibleItemPosition();
                    if (first == 0) {
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).hideControlButton(FavoriteListFragment.this);
                        }
                    } else {
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).showControlButton(FavoriteListFragment.this);
                        }
                    }
                } else if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                    StaggeredGridLayoutManager layoutManager = ((StaggeredGridLayoutManager) recyclerView.getLayoutManager());
                    int first = layoutManager.findFirstVisibleItemPositions(null)[0];
                    if (first == 0) {
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).hideControlButton(FavoriteListFragment.this);
                        }
                    } else {
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).showControlButton(FavoriteListFragment.this);
                        }
                    }
                }
            }
        });

        FragmentPostViewAdapter adapter = (FragmentPostViewAdapter) recyclerView.getAdapter();
        adapter.listenersCallback = listeners.itemCallback;
        adapter.postListViewModel = postListViewModel;

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
        unbinder.unbind();
    }
}
