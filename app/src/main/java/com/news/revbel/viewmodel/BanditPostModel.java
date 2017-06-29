package com.news.revbel.viewmodel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.news.revbel.R;
import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.database.PostDataModel;
import com.news.revbel.database.TaxonomyDataModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;

public class BanditPostModel extends PostModel {
    private final static List<String> professionCategories = Arrays.asList("13", "8", "27", "12");
    private ArrayList<TaxonomyModel> categoryModels;
    private ArrayList<TaxonomyModel> tagModels;
    private ArrayList<TaxonomyModel> skillModels;

    public enum TAG_TYPE {
        AUTO, ADDRESS, VIDEO, PROFESSION, RELATIVES, SOCIAL, TELEPHONE, PHOTO, UNDEFINED;

        static TAG_TYPE fromId(String id) {
            switch (Integer.parseInt(id)) {
                case 35: return AUTO;
                case 30: return ADDRESS;
                case 33: return VIDEO;
                case 29: return PROFESSION;
                case 31: return RELATIVES;
                case 34: return SOCIAL;
                case 32: return TELEPHONE;
                case 28: return PHOTO;
            }
            return UNDEFINED;
        }
    }

    public BanditPostModel(JSONObject object) throws JSONException {
        super(object);

        if (object.has("_embedded")) {
            JSONObject embed = object.getJSONObject("_embedded");
            JSONArray wpterm = embed.getJSONArray("wp:term");

            this.categoryModels = new ArrayList<>();
            this.tagModels = new ArrayList<>();
            this.skillModels = new ArrayList<>();

            this.categories = new ArrayList<>();
            this.tags = new ArrayList<>();

            for (int i = 0; i < wpterm.length(); i++) {
                JSONArray taxonomyArray = wpterm.getJSONArray(i);

                if (taxonomyArray.length() > 0) {
                    JSONObject taxonomy = taxonomyArray.getJSONObject(0);
                    if (taxonomy.getString("taxonomy").equals(getCategoryType())) {
                        ArrayList<TaxonomyModel> categoryModelList = new ArrayList<>();
                        for (int j = 0; j < taxonomyArray.length(); j++) {
                            taxonomy = taxonomyArray.getJSONObject(j);
                            TaxonomyModel taxonomyModel = new TaxonomyModel(taxonomy);

                            categoryModelList.add(taxonomyModel);
                        }
                        this.categoryModels.addAll(categoryModelList);

                        for (TaxonomyModel category : categoryModels) {
                            this.categories.add(category.uniqueId);
                        }
                    } else if (taxonomy.getString("taxonomy").equals(getTagType())) {
                        ArrayList<TaxonomyModel> categoryModelList = new ArrayList<>();
                        for (int j = 0; j < taxonomyArray.length(); j++) {
                            taxonomy = taxonomyArray.getJSONObject(j);
                            TaxonomyModel taxonomyModel = new TaxonomyModel(taxonomy);

                            categoryModelList.add(taxonomyModel);
                        }
                        this.tagModels.addAll(categoryModelList);

                        for (TaxonomyModel tag : tagModels) {
                            this.tags.add(tag.uniqueId);
                        }
                    } else if (taxonomy.getString("taxonomy").equals(getSkillsType())) {
                        ArrayList<TaxonomyModel> categoryModelList = new ArrayList<>();
                        for (int j = 0; j < taxonomyArray.length(); j++) {
                            taxonomy = taxonomyArray.getJSONObject(j);
                            TaxonomyModel taxonomyModel = new TaxonomyModel(taxonomy);

                            categoryModelList.add(taxonomyModel);
                        }
                        this.skillModels.addAll(categoryModelList);

                        for (TaxonomyModel tag : skillModels) {
                            this.tags.add(tag.uniqueId);
                        }
                    }
                }
            }
        }
        updateDataModel();
    }

    BanditPostModel(PostDataModel dataModel) {
        super(dataModel);

        this.categoryModels = new ArrayList<>();
        Realm realm = Realm.getDefaultInstance();
        for (String categoryId : this.categories) {
            RealmQuery<TaxonomyDataModel> resultQuery = realm.where(TaxonomyDataModel.class).equalTo("uniqueId", categoryId);
            TaxonomyDataModel taxonomyDataModel = resultQuery.findFirst();

            TaxonomyModel taxonomyModel = new TaxonomyModel(taxonomyDataModel);
            this.categoryModels.add(taxonomyModel);
        }

        this.tagModels = new ArrayList<>();
        this.skillModels = new ArrayList<>();
        for (String tagId : this.tags) {
            RealmQuery<TaxonomyDataModel> resultQuery = realm.where(TaxonomyDataModel.class).equalTo("uniqueId", tagId);
            TaxonomyDataModel taxonomyDataModel = resultQuery.findFirst();

            TaxonomyModel taxonomyModel = new TaxonomyModel(taxonomyDataModel);
            if (taxonomyModel.getType().equals(getTagType())) {
                this.tagModels.add(taxonomyModel);
            } else if (taxonomyModel.getType().equals(getSkillsType())) {
                this.skillModels.add(taxonomyModel);
            }
        }

        realm.close();
    }

    @Override
    protected void mergeItem(PostModel postModel) {
        super.mergeItem(postModel);

        BanditPostModel banditPostModel = (BanditPostModel) postModel;

        if (categoryModels.size() == 0 || (categoryModels.size() != banditPostModel.categoryModels.size())) {
            categoryModels = banditPostModel.categoryModels;
        }

        if (tagModels.size() == 0 || (tagModels.size() != banditPostModel.tagModels.size())) {
            tagModels = banditPostModel.tagModels;
        }

        if (skillModels.size() == 0 || (skillModels.size() != banditPostModel.skillModels.size())) {
            skillModels = banditPostModel.skillModels;
        }
    }

    public String getCityName() {
        if (categoryModels.size() > 0) {
            for (TaxonomyModel categoryModel : categoryModels) {
                if (!professionCategories.contains(categoryModel.getId())) return categoryModel.name;
            }
        }
        return RevApplication.getInstance().getString(R.string.bandaluki_city_unknown);
    }

    public HashSet<TAG_TYPE> getTags() {
        HashSet<TAG_TYPE> result = new HashSet<>();
        for (TaxonomyModel tag : tagModels) {
            if (tag.getType().contains(getTagType())) result.add(TAG_TYPE.fromId(tag.getId()));
        }
        return result;
    }

    @Override
    public void postReplyOnPost(int parentId, String userName, String userEmail, String text, ViewModelPostReplyCallback callback) {
        networkCoordinator.postReplyOnBandit(getId(), parentId, userName, userEmail, text, new NetworkCoordinator.NetworkCoordinatorErrorCallback() {
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

    String getMetaContacted() {
        String result = "";
        for (TaxonomyModel skill : skillModels) {
            result += skill.name + ",";
        }
        for (TaxonomyModel category : categoryModels) {
            result += category.name + ",";
        }
        for (TaxonomyModel tag : tagModels) {
            result += tag.name + ",";
        }
        return result;
    }

    private String getSkillsType() {
        return "portfolio_skills";
    }

    @Override
    public String getCategoryType() {
        return "portfolio_category";
    }

    @Override
    public String getTagType() {
        return "portfolio_tags";
    }

    @Override
    public String getType() {
        return type();
    }

    public static String type() {
        return "bandit";
    }
}
