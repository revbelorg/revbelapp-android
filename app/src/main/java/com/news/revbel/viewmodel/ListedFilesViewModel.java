package com.news.revbel.viewmodel;

import android.databinding.ObservableBoolean;

import com.news.revbel.BR;
import com.news.revbel.R;
import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.filelist.FileListRecycleAdapter;
import com.news.revbel.filelist.FilesListener;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import me.tatarka.bindingcollectionadapter2.ItemBinding;
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList;

import static com.news.revbel.utilities.Utilities.compareObjects;


public class ListedFilesViewModel {
    @Inject
    NetworkCoordinator networkCoordinator;

    public class FileListInfo {
        private String networkParams;
        public String title;

        FileListInfo(String networkParams, String title) {
            this.networkParams = networkParams;
            this.title = title;
        }
    }

    private FileListInfo currentList;

    public ObservableBoolean isDownloading = new ObservableBoolean(false);

    private final DiffObservableList.Callback<FileViewModel> diffCallback = new DiffObservableList.Callback<FileViewModel>() {
        @Override
        public boolean areItemsTheSame(FileViewModel oldItem, FileViewModel newItem) {
            return oldItem.remoteURL.equals(newItem.remoteURL);
        }

        @Override
        public boolean areContentsTheSame(FileViewModel oldItem, FileViewModel newItem) {
            boolean sameTitle = compareObjects(oldItem.bookTitle, newItem.bookTitle);
            boolean sameLocal = compareObjects(oldItem.localURL, newItem.localURL);
            return sameTitle && sameLocal;
        }
    };

    public FilesListener listener = new FilesListener();

    public final DiffObservableList<FileViewModel> items = new DiffObservableList<>(diffCallback);

    public final ItemBinding singleItemView = ItemBinding.of(BR.item, R.layout.cell_book).bindExtra(BR.listener, listener);
    public final FileListRecycleAdapter adapter = new FileListRecycleAdapter();

    ListedFilesViewModel() {
        RevApplication.getComponent().inject(this);
        adapter.listedFilesViewModel = this;
    }

    private void setItemsToLibrary(List<FileViewModel> books) {
        for (FileViewModel book : books) {
            book.owner = this;
        }
        if (getSorted()) Collections.sort(books, (viewModel, t1) -> viewModel.bookTitle.compareTo(t1.bookTitle));
        items.update(books);
    }

    public void setCurrentLibrary(FileListInfo newList) {
        if (currentList == null || !newList.networkParams.equals(currentList.networkParams)) {
            currentList = newList;
            updateCurrentFileList();
        }
    }

    private void updateCurrentFileList() {
        items.update(Collections.emptyList());
        downloadFileList(currentList);
    }

    private void downloadFileList(FileListInfo downloadList) {
        isDownloading.set(true);
        networkCoordinator.getListedFiles(downloadList.networkParams, this, new NetworkCoordinator.NetworkCoordinatorCallback() {
            @Override
            public void onFailure() {
                isDownloading.set(false);
            }

            @Override
            public void onDatabaseResponse(List items) {
                if (downloadList.networkParams.equals(currentList.networkParams)) {
                    setItemsToLibrary(items);
                }
            }

            @Override
            public void onNetworkResponse(List items) {
                if (downloadList.networkParams.equals(currentList.networkParams)) {
                    isDownloading.set(false);
                    setItemsToLibrary(items);
                }
            }
        });
    }

    boolean getSorted() {
        return false;
    }

    public String getPath() {
        return "Files/";
    }

    public List<FileListInfo> getCategories() {
        return null;
    }
}
