package com.news.revbel.database;

import com.news.revbel.viewmodel.ReplyModel;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ReplyDataModel extends RealmObject {
    @PrimaryKey
    public String uniqueId;
    public int replyId;
    public int postId;
    public int parentId;
    public String authorName;
    public Date date;
    public String textContent;
    public boolean isAdmin;

    public ReplyDataModel() {

    }

    public ReplyDataModel(ReplyModel replyModel) {
        uniqueId = replyModel.replyId + "_" + replyModel.postId;
        replyId = replyModel.replyId;
        postId = replyModel.postId;
        parentId = replyModel.parentId;
        authorName = replyModel.authorName;
        date = replyModel.date;
        textContent = replyModel.textContent;
        isAdmin = replyModel.isAdmin;
    }


}
