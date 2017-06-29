package com.news.revbel.database;

import com.news.revbel.viewmodel.PostModel;
import com.news.revbel.viewmodel.TaxonomyModel;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class PostDataModel extends RealmObject {
    @PrimaryKey public String uniqueId;
    public int postId;
    public String type;

    public RealmList<TaxonomyDataModel> categories;

    public RealmList<TaxonomyDataModel> tags;

    public String postTitle;
    public String postText;
    public String fullPostLink;
    public String excerpt;
    public Date createdAt;
    public String link;
    public String featuredMediaUrl;
    public float featuredImageHeight;
    public String author;
    public boolean isFavorite;

    public PostDataModel() {

    }

    public PostDataModel(PostModel postViewModel)
    {
        postId = postViewModel.getId();
        uniqueId = postViewModel.getUniqueId();
        updateModel(postViewModel);
    }

    private void updateModel(PostModel postViewModel)
    {
        type = postViewModel.getType();
        postTitle = postViewModel.title;
        fullPostLink = postViewModel.fullPostLink;
        excerpt = postViewModel.excerpt;
        createdAt = postViewModel.createdAt;
        author = postViewModel.author;
        featuredMediaUrl = postViewModel.featuredMediaUrl;
        featuredImageHeight = postViewModel.featuredImageHeight;
        link = postViewModel.link;
        postText = postViewModel.text.get();
        isFavorite = postViewModel.isFavorite;

        categories = new RealmList<>();
        Realm realm = Realm.getDefaultInstance();
        if (postViewModel.categories != null) {
            for (String categoryId : postViewModel.categories) {
                TaxonomyDataModel taxonomyDataModel = realm.where(TaxonomyDataModel.class).equalTo("uniqueId", categoryId).findFirst();
                if (taxonomyDataModel != null && !hasIdInCategories(categoryId)) {
                    categories.add(realm.copyFromRealm(taxonomyDataModel));
                    addAllParentCategory(realm, taxonomyDataModel.parentId, postViewModel.getCategoryType());
                }
            }
        }

        tags = new RealmList<>();
        if (postViewModel.tags != null) {
            for (String tagId : postViewModel.tags) {
                TaxonomyDataModel taxonomyDataModel = realm.where(TaxonomyDataModel.class).equalTo("uniqueId", tagId).findFirst();
                if (taxonomyDataModel != null) {
                    tags.add(realm.copyFromRealm(taxonomyDataModel));
                }
            }
        }
        realm.close();
    }

    private void addAllParentCategory(Realm realm, String categoryId, String categoryType) {
        if (categoryId != null) {
            TaxonomyDataModel taxonomyDataModel = realm.where(TaxonomyDataModel.class).equalTo("uniqueId", TaxonomyModel.uniqueIdWith(categoryId, categoryType)).findFirst();
            if (taxonomyDataModel != null && !hasIdInCategories(categoryId)) {
                categories.add(realm.copyFromRealm(taxonomyDataModel));
                if (taxonomyDataModel.parentId != null && !taxonomyDataModel.parentId.isEmpty()) {
                    addAllParentCategory(realm, taxonomyDataModel.parentId, categoryType);
                }
            }
        }
    }

    private boolean hasIdInCategories(String categoryId) {
        if (categoryId != null) {
            for (TaxonomyDataModel taxonomyDataModel : categories) {
                if (taxonomyDataModel.uniqueId.equals(categoryId))
                    return true;
            }
        }
        return false;
    }
}
