package com.news.revbel.viewmodel;

import com.news.revbel.RevApplication;
import com.news.revbel.database.ReplyDataModel;

import java.util.Date;

import io.realm.Realm;

public class ReplyModel {
    public int replyId;
    public int postId;
    public int parentId;
    public String authorName;
    public Date date;
    public String textContent;
    public boolean isAdmin;

    public ReplyModel(int replyId, int postId, int parentId, String authorName, Date date, String textContent, boolean isAdmin) {
        this.replyId = replyId;
        this.postId = postId;
        this.parentId = parentId;
        this.authorName = authorName;
        this.date = date;
        this.textContent = textContent;
        this.isAdmin = isAdmin;

        RevApplication.runOnUI(this::createData);
    }

    public ReplyModel(ReplyDataModel dataModel) {
        replyId = dataModel.replyId;
        postId = dataModel.postId;
        parentId = dataModel.parentId;
        authorName = dataModel.authorName;
        date = dataModel.date;
        textContent = dataModel.textContent;
        isAdmin = dataModel.isAdmin;
    }

    private void createData() {
        ReplyDataModel replyDataModel = new ReplyDataModel(this);

        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(realm1 -> realm1.copyToRealmOrUpdate(replyDataModel), realm::close, error -> realm.close());
    }
}
