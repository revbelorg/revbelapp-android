package com.news.revbel.filelist;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.news.revbel.RevApplication;
import com.news.revbel.databinding.FragmentFileListBinding;
import com.news.revbel.utilities.ControlActivityInterface;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.R;
import com.news.revbel.viewmodel.ListedFilesViewModel;

import org.jsoup.select.Evaluator;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
import butterknife.Unbinder;

public class FileListFragment extends Fragment {
    @Inject PostListCoordinator coordinator;

    @BindView(R.id.list) RecyclerView recyclerView;
    @BindView(R.id.spinner) Spinner categorySpinner;

    private int itemWasVisible = 0;

    private Unbinder unbinder;
    private FragmentFileListBinding binding;

    private ControlActivityInterface activityInterface;
    private ListedFilesViewModel fileListViewModel;
    private final static String CLASS_NAME_KEY = "CLASS_NAME";

    Observable.OnPropertyChangedCallback callback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable observable, int i) {
            if (observable instanceof ObservableBoolean) {
                ObservableBoolean observableBoolean = (ObservableBoolean) observable;
                if (observableBoolean.get()) {
                    activityInterface.startLoading();
                } else {
                    activityInterface.stopLoading();
                }
            }
        }
    };

    public FileListFragment() {
    }

    public static FileListFragment newInstance(ListedFilesViewModel model) {
        FileListFragment fragment = new FileListFragment();
        fragment.fileListViewModel = model;
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof ControlActivityInterface) {
            activityInterface = (ControlActivityInterface) getActivity();
        }
        if (activityInterface != null) {
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
        fileListViewModel.isDownloading.removeOnPropertyChangedCallback(callback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CLASS_NAME_KEY, fileListViewModel.getClass().getName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RevApplication.getComponent().inject(this);

        binding = FragmentFileListBinding.inflate(inflater, container, false);

        if (savedInstanceState != null) {
            String className = savedInstanceState.getString(CLASS_NAME_KEY);
            try {
                Class modelClass = Class.forName(className);
                Constructor modelConstructor = modelClass.getConstructor();
                fileListViewModel = (ListedFilesViewModel) modelConstructor.newInstance();
            } catch (Exception e) {

            }
        }
        binding.setLibraryViewModel(fileListViewModel);
        binding.executePendingBindings();

        fileListViewModel.isDownloading.addOnPropertyChangedCallback(callback);

        View view = binding.getRoot();
        unbinder = ButterKnife.bind(this, view);

        ArrayList<String> spinnerArray = new ArrayList<>();
        for (ListedFilesViewModel.FileListInfo item : fileListViewModel.getCategories()) {
            spinnerArray.add(item.title);
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.library_category_item,
                spinnerArray);

        spinnerArrayAdapter.setDropDownViewResource(R.layout.breadcrumb_category_dropdown_item);
        categorySpinner.setAdapter(spinnerArrayAdapter);

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
                if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                    LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
                    int first = layoutManager.findFirstVisibleItemPosition();
                    if (first == 0) {
                        if (activityInterface != null) {
                            activityInterface.hideControlButton(FileListFragment.this);
                        }
                    } else {
                        if (activityInterface != null) {
                            activityInterface.showControlButton(FileListFragment.this);
                        }
                    }

                    int firstCompletely = layoutManager.findFirstCompletelyVisibleItemPosition();

                    if (firstCompletely == 0 && itemWasVisible > 0) {
                        categorySpinner.animate().alpha(1).setDuration(300);
                    } else if (itemWasVisible == 0 && firstCompletely > 0) {
                        categorySpinner.animate().alpha(0).setDuration(300);
                    }

                    itemWasVisible = firstCompletely;
                }
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.setLibraryViewModel(null);
        unbinder.unbind();
    }

    @OnItemSelected(R.id.spinner)
    void onItemSelected(int position) {
        ListedFilesViewModel.FileListInfo item = fileListViewModel.getCategories().get(position);
        fileListViewModel.setCurrentLibrary(item);
    }
}