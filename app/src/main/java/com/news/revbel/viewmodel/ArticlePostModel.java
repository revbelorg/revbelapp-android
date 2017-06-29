package com.news.revbel.viewmodel;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.database.PostDataModel;

import java.util.ArrayList;
import java.util.List;

public class ArticlePostModel extends PostModel {
    public ArticlePostModel(JSONObject object) throws JSONException {
        super(object);

        if (object.has("_embedded")) {
            JSONObject embed = object.getJSONObject("_embedded");
            JSONArray wpterm = embed.getJSONArray("wp:term");
            for (int i = 0; i < wpterm.length(); i++) {
                JSONArray taxonomyArray = wpterm.getJSONArray(i);

                if (taxonomyArray.length() > 0) {
                    JSONObject taxonomy = taxonomyArray.getJSONObject(0);
                    if (taxonomy.getString("taxonomy").equals("category")) {
                        ArrayList<String> categoryModels = new ArrayList<>();
                        for (int j = 0; j < taxonomyArray.length(); j++) {
                            taxonomy = taxonomyArray.getJSONObject(j);
                            TaxonomyModel taxonomyModel = new TaxonomyModel(taxonomy, false);

                            categoryModels.add(taxonomyModel.uniqueId);
                        }
                        this.categories = categoryModels;
                    } else if (taxonomy.getString("taxonomy").equals("post_tag")) {
                        ArrayList<String> tags = new ArrayList<>();
                        for (int j = 0; j < taxonomyArray.length(); j++) {
                            taxonomy = taxonomyArray.getJSONObject(j);
                            TaxonomyModel taxonomyModel = new TaxonomyModel(taxonomy);
                            tags.add(taxonomyModel.uniqueId);
                        }
                        this.tags = tags;
                    }
                }
            }
        }
    }

    ArticlePostModel(PostDataModel dataModel) {
        super(dataModel);
    }

    @Override
    public void postReplyOnPost(int parentId, String userName, String userEmail, String text, ViewModelPostReplyCallback callback) {
        networkCoordinator.postReplyOnArticle(getId(), parentId, userName, userEmail, text, new NetworkCoordinator.NetworkCoordinatorErrorCallback() {
            @Override
            public void onFailure(Exception e) {
                if (callback != null) callback.onFailure(e);
            }

            @Override
            public void onNetworkResponse(List items) {
                replies.addRepliesAndSort(items);
                if (callback != null) callback.onSuccess((ReplyModel) items.get(0));
            }
        });
    }

    @Override
    public String getCategoryType() {
        return "category";
    }

    @Override
    public String getTagType() {
        return "post_tag";
    }

    @Override
    public String getType() {
        return type();
    }

    public static String type() {
        return "post";
    }
}
