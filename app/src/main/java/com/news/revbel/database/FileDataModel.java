package com.news.revbel.database;

import com.news.revbel.viewmodel.FileViewModel;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class FileDataModel extends RealmObject {
    public int id;
    public String localURL;
    public String imageURL;
    public float imageHeightAspect;

    @PrimaryKey
    public String remoteURL;
    public String bookTitle;
    public String pageOwner;

    public FileDataModel() {

    }

    public FileDataModel(FileViewModel viewModel) {
        this.id = viewModel.id;
        this.localURL = viewModel.localURL;
        this.remoteURL = viewModel.remoteURL;
        this.bookTitle = viewModel.bookTitle;
        this.pageOwner = viewModel.pageOwner;
        this.imageURL = viewModel.imageURL;
        this.imageHeightAspect = viewModel.imageHeightAspect;
    }
}
