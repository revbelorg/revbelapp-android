package com.news.revbel.viewmodel;

import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.database.TaxonomyDataModel;
import com.news.revbel.database.PostDataModel;
import com.news.revbel.database.ReplyDataModel;
import com.news.revbel.utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public abstract class PostModel {
    public interface ViewModelPostReplyCallback {
        void onFailure(Exception e);
        void onSuccess(ReplyModel replyModel);
    }

    @Inject NetworkCoordinator networkCoordinator;

    private int id;

    public ArrayList<String> categories;

    public ArrayList<String> tags;

    public String fullPostLink;
    public String excerpt;
    public Date createdAt;
    public String title;
    public String author;
    public String featuredMediaUrl;
    public String link;

    public float featuredImageHeight = 0;
    public boolean isFavorite;

    public ReplyListViewModel replies;

    public final ObservableField<String> text = new ObservableField<>();

    final public ObservableBoolean isDownloading = new ObservableBoolean();

    protected PostModel(JSONObject object) throws JSONException {
        RevApplication.getComponent().inject(this);

        this.id = object.getInt("id");

        title = object.getJSONObject("title").getString("rendered");
        excerpt = object.getJSONObject("excerpt").getString("rendered").trim();
        link = object.getString("link");

        author = object.getString("author");
        if (object.has("content") && object.getJSONObject("content").has("rendered")) {
            text.set(object.getJSONObject("content").getString("rendered"));
        }

        updateReplies(Collections.emptyList());
        if (object.has("_embedded")) {
            JSONObject embed = object.getJSONObject("_embedded");
            if (embed.has("replies")) {
                JSONArray replies = embed.getJSONArray("replies").getJSONArray(0);
                ArrayList<ReplyModel> repliesArray = new ArrayList<>();

                for (int i = 0; i < replies.length(); i++) {
                    JSONObject reply = replies.getJSONObject(i);

                    String authorName = reply.getString("author_name");
                    int postId = this.getId();
                    int replyId = reply.getInt("id");
                    int parentId = reply.getInt("parent");

                    String textContent = reply.getJSONObject("content").getString("rendered");
                    Date date = Utilities.stringToDate(reply.getString("date"));
                    ReplyModel replyModel = new ReplyModel(replyId, postId, parentId, authorName, date, textContent, false);
                    repliesArray.add(replyModel);
                }

                updateReplies(repliesArray);
            }

            if (embed.has("wp:featuredmedia")) {
                JSONArray mediaArray = embed.getJSONArray("wp:featuredmedia");
                if (mediaArray.length() > 0) {
                    JSONObject media = mediaArray.getJSONObject(0);

                    if (media.has("media_details")) {
                        JSONObject sizes = media.getJSONObject("media_details").getJSONObject("sizes");
                        String imageURL;

                        int width, height;
                        if (sizes.has("large")) {
                            width = sizes.getJSONObject("large").getInt("width");
                            height = sizes.getJSONObject("large").getInt("height");
                            imageURL = sizes.getJSONObject("large").getString("source_url");
                        } else {
                            width = sizes.getJSONObject("full").getInt("width");
                            height = sizes.getJSONObject("full").getInt("height");
                            imageURL = sizes.getJSONObject("full").getString("source_url");
                        }

                        this.featuredImageHeight = (float) height / width;
                        this.featuredMediaUrl = imageURL;
                    }
                }
            }
        }

        if (object.has("_links")) {
            JSONObject links = object.getJSONObject("_links");
            this.fullPostLink = links.getJSONArray("self").getJSONObject(0).getString("href");
        }

        this.createdAt = Utilities.stringToDate(object.getString("date"));
    }

    public static PostModel createFromDatabase(PostDataModel postDataModel) {
        PostModel postModel = null;
        if (postDataModel.type.equals(BanditPostModel.type())) {
            postModel = new BanditPostModel(postDataModel);
        } else if (postDataModel.type.equals(ArticlePostModel.type())) {
            postModel = new ArticlePostModel(postDataModel);
        }
        return postModel;
    }

    protected PostModel(PostDataModel postDataModel) {
        RevApplication.getComponent().inject(this);
        this.id = postDataModel.postId;
        this.excerpt = postDataModel.excerpt;
        this.createdAt = postDataModel.createdAt;
        this.fullPostLink = postDataModel.fullPostLink;
        this.title = postDataModel.postTitle;
        this.featuredMediaUrl = postDataModel.featuredMediaUrl;
        this.featuredImageHeight = postDataModel.featuredImageHeight;
        this.isFavorite = postDataModel.isFavorite;
        this.link = postDataModel.link;
        this.text.set(postDataModel.postText);

        this.author = postDataModel.author;

        categories = new ArrayList<>();
        if (postDataModel.categories != null) {
            for (TaxonomyDataModel taxonomyDataModel : postDataModel.categories) {
                categories.add(taxonomyDataModel.uniqueId);
            }
        }

        tags = new ArrayList<>();
        if (postDataModel.tags != null) {
            for (TaxonomyDataModel taxonomyDataModel : postDataModel.tags) {
                tags.add(taxonomyDataModel.uniqueId);
            }
        }

        Realm realm = Realm.getDefaultInstance();
        RealmResults<ReplyDataModel> results = realm.where(ReplyDataModel.class).equalTo("uniqueId",getUniqueId()).findAllSorted("date", Sort.ASCENDING);

        ArrayList<ReplyModel> replyModels = new ArrayList<>();

        for (ReplyDataModel reply : results) {
            ReplyModel replyModel = new ReplyModel(reply);
            replyModels.add(replyModel);
        }
        updateReplies(replyModels);
        realm.close();
    }

    public void updateDataModel() {
        PostDataModel dataModel = new PostDataModel(this);

        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(dataModel));
        realm.close();
    }

    private void updateReplies(List<ReplyModel> list) {
        if (replies == null) {
            replies = new ReplyListViewModel(list, id);
        } else {
            replies.reloadReplies(list);
        }
    }

    protected void mergeItem(PostModel postModel) {
        String oldText = this.text.get();
        String newText = postModel.text.get();

        boolean isUpdating = false;
        if (newText != null && !newText.isEmpty() && !newText.equals(oldText)) {
            this.text.set(newText);
            isUpdating = true;
        }

        if (isFavorite != postModel.isFavorite) {
            isFavorite = postModel.isFavorite = true;
            isUpdating = true;
        }

        if (categories == null || (postModel.categories != null && categories.size() != postModel.categories.size())) {
            categories = postModel.categories;
            isUpdating = true;
        }

        if (tags == null || (postModel.tags != null && tags.size() != postModel.tags.size())) {
            tags = postModel.tags;
            isUpdating = true;
        }

        if (postModel.replies != null && postModel.replies.items != null) {
            updateReplies(postModel.replies.items);
            isUpdating = true;
        }

        if (isUpdating) {
            updateDataModel();
        }
    }

    public void setIsFavorite(boolean favorite, PostListCoordinator.ViewModelGetPostCallback callback) {
        Runnable setter = () -> {
            this.isFavorite = favorite;
            updateDataModel();
            callback.onSuccess(this);
        };

        if (favorite) {
            if (this.text.get() != null && this.text.get().length() > 0) {
                setter.run();
            } else {
                getFullPost(new PostListCoordinator.ViewModelGetPostCallback() {
                    @Override
                    public void onFailure() {
                        callback.onFailure();
                    }

                    @Override
                    public void onSuccess(PostModel postModel) {
                        setter.run();
                    }
                });
            }
        } else {
            setter.run();
        }
    }

    public Spanned getTitleSpanned() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(title, android.text.Html.FROM_HTML_MODE_COMPACT);
        } else {
            return Html.fromHtml(title);
        }
    }

    public Spanned getExcerptSpanned() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(excerpt, android.text.Html.FROM_HTML_MODE_COMPACT);
        } else {
            return Html.fromHtml(excerpt);
        }
    }

    public abstract void postReplyOnPost(int parentId, String userName, String userEmail, String text, ViewModelPostReplyCallback callback);

    public void getFullPost(PostListCoordinator.ViewModelGetPostCallback callback) {
        if (text.get() == null || text.get().isEmpty()) {
            isDownloading.set(true);
        }

        networkCoordinator.getPostFullText(fullPostLink, new NetworkCoordinator.NetworkCoordinatorCallback() {
            @Override
            public void onFailure() {
                isDownloading.set(false);
                if (callback != null) callback.onFailure();
            }

            @Override
            public void onDatabaseResponse(List items) {

            }

            @Override
            public void onNetworkResponse(List items) {
                PostModel item = (PostModel) items.get(0);
                mergeItem(item);
                isDownloading.set(false);

                if (callback != null) callback.onSuccess(PostModel.this);
            }
        });
    }

    public abstract String getCategoryType();

    public abstract String getTagType();

    public abstract String getType();

    public int getId() {
        return id;
    }

    public static String getUninqueId(int id, String type) {
        return id + "_" + type;
    }

    public String getUniqueId() {
        return getUninqueId(getId(), getType());
    }
}
