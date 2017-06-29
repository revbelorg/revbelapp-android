package com.news.revbel.postlist;

import android.app.Activity;
import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.news.revbel.RevApplication;
import com.news.revbel.utilities.ControlActivityInterface;
import com.news.revbel.viewmodel.TaxonomyModel;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.viewmodel.CategoryPostListViewModel;
import com.news.revbel.R;
import com.news.revbel.databinding.FragmentPostListBinding;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class PostListFragment extends Fragment {
    @Inject
    PostListCoordinator listCoordinator;
    @Inject
    CategoryPostListViewModel postListViewModel;

    private View view;
    @BindView(R.id.recycle_list) RecyclerView recyclerView;
    @BindView(R.id.swipeContainer) SwipyRefreshLayout swipeContainer;

    private Unbinder unbinder;

    private PostListListeners postListListeners;
    private ArrayList<View> spinners = new ArrayList<>();

    Observable.OnPropertyChangedCallback callback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable observable, int i) {
            if (postListViewModel.coordinator.categoryListViewModel.isFirstDownloaded.get()) {
                addSpinnerWithCategory(view, listCoordinator.categoryListViewModel.rootTaxonomy);
            }
        }
    };

    private Observable.OnPropertyChangedCallback downloadingCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable observable, int i) {
            RevApplication.runOnUI(() -> {
                ObservableBoolean observableBoolean = (ObservableBoolean) observable;
                Activity activity = getActivity();
                if (observableBoolean.get()) {
                    if (activity instanceof ControlActivityInterface && swipeContainer != null && !swipeContainer.isRefreshing()) {
                        ((ControlActivityInterface) activity).startLoading();
                    }
                } else {
                    if (activity instanceof ControlActivityInterface && swipeContainer != null && !swipeContainer.isRefreshing()) {
                        ((ControlActivityInterface) activity).stopLoading();
                    }
                }
            });
        }
    };

    public PostListFragment() {
    }

    public static PostListFragment newInstance() {
        PostListFragment fragment = new PostListFragment();

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ControlActivityInterface activity = (ControlActivityInterface) getActivity();
        activity.updateControlButtonTapEvent(this, () -> {
            recyclerView.smoothScrollToPosition(0);
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ControlActivityInterface activity = (ControlActivityInterface) getActivity();
        activity.onFragmentHide(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RevApplication.getComponent().inject(this);

        postListListeners = new PostListListeners(postListViewModel);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentPostListBinding binding = FragmentPostListBinding.inflate(inflater, container, false);
        binding.setPostListViewModel(postListViewModel);
        binding.setListeners(postListListeners);
        binding.executePendingBindings();
        view = binding.getRoot();

        unbinder = ButterKnife.bind(this, view);

        swipeContainer.setOnRefreshListener(direction -> {
            if (direction == SwipyRefreshLayoutDirection.TOP ) {
                postListViewModel.updateFirstPosts();
            }
            else {
                postListViewModel.getNextPagePosts();
            }
        });

        postListViewModel.isDownloading.addOnPropertyChangedCallback(downloadingCallback);

        Activity activity = getActivity();

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(50);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (isFirstItemVisible()) {
                    if (activity instanceof ControlActivityInterface) {
                        ((ControlActivityInterface) activity).hideControlButton(PostListFragment.this);
                    }
                } else {
                    if (activity instanceof ControlActivityInterface) {
                        ((ControlActivityInterface) activity).showControlButton(PostListFragment.this);
                    }
                }
            }
        });

        FragmentPostViewAdapter adapter = (FragmentPostViewAdapter) recyclerView.getAdapter();
        adapter.listenersCallback = postListListeners.itemCallback;
        adapter.postListViewModel = postListViewModel;

        if (postListViewModel.coordinator.categoryListViewModel.isFirstDownloaded.get()) {
            addSpinnerWithCategory(view, listCoordinator.categoryListViewModel.rootTaxonomy);
        } else {
            postListViewModel.coordinator.categoryListViewModel.isFirstDownloaded.addOnPropertyChangedCallback(callback);
        }

        return view;
    }

    private void addSpinnerWithCategory(View view, TaxonomyModel taxonomyModel) {
        final List<String> categoryList = taxonomyModel.childTaxonomies;
        final Activity activity = getActivity();

        if (categoryList.size() > 0) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View layout = inflater.inflate(R.layout.breadcrumb_spinner, (ViewGroup)view.findViewById(R.id.spinnerLayout), false);
            BreadcrumbSpinner spinner = (BreadcrumbSpinner) layout.findViewById(R.id.breadcrumb);
            spinner.setCategory(taxonomyModel, postListViewModel);

            LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.spinnerLayout);
            linearLayout.addView(layout);

            CategoryPostListViewModel.ReloadListCallback callback = new CategoryPostListViewModel.ReloadListCallback() {
                @Override
                public void onDataLoaded() {
                    if (activity instanceof ControlActivityInterface && recyclerView != null) {
                        if (!isFirstItemVisible() && postListViewModel.items.size() > 0) recyclerView.scrollToPosition(0);
                    }
                }

                @Override
                public void onNetworkLoaded() {
                    if (activity instanceof ControlActivityInterface && recyclerView != null) {
                        if (!isFirstItemVisible()) recyclerView.smoothScrollToPosition(0);
                    }
                }
            };

            spinner.callback = (parent, view1, position) -> {
                if (position == 0) {
                    postListViewModel.changeCategoryId(taxonomyModel.uniqueId, callback);
                    removeSpinnersAfter(view, layout);
                } else {
                    String categoryId = categoryList.get(position - 1);
                    postListViewModel.changeCategoryId(categoryId, callback);
                    TaxonomyModel nextCategory = postListViewModel.coordinator.categoryListViewModel.listAll.get(categoryList.get(position - 1));
                    removeSpinnersAfter(view, layout);
                    addSpinnerWithCategory(view, nextCategory);
                }
            };

            spinners.add(layout);
        }
    }

    private void removeSpinnersAfter(View view, View spinner) {
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.spinnerLayout);
        ArrayList<View> spinnersToDelete = new ArrayList<>();
        for (int i = spinners.indexOf(spinner) + 1; i < spinners.size(); i++) {
            View nextSpinner = spinners.get(i);
            linearLayout.removeView(nextSpinner);
            spinnersToDelete.add(nextSpinner);
        }

        spinners.removeAll(spinnersToDelete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        postListViewModel.coordinator.categoryListViewModel.isFirstDownloaded.removeOnPropertyChangedCallback(callback);
        postListViewModel.isDownloading.removeOnPropertyChangedCallback(downloadingCallback);
        recyclerView.setAdapter(null);
        unbinder.unbind();
    }

    private boolean isFirstItemVisible() {
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
            int first = layoutManager.findFirstVisibleItemPosition();
            return first == 0;
        } else if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = ((StaggeredGridLayoutManager) recyclerView.getLayoutManager());
            int first = layoutManager.findFirstVisibleItemPositions(null)[0];
            return first == 0;
        }
        return false;
    }

    @BindingAdapter({"stopSwipeRefresh"})
    public static void stopRefresh(SwipyRefreshLayout swipeRefreshLayout, boolean stopSwipeRefresh) {
        if (!stopSwipeRefresh) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
