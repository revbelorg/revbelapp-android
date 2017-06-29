package com.news.revbel.database;

import com.news.revbel.viewmodel.TaxonomyModel;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class TaxonomyDataModel extends RealmObject {
    @PrimaryKey public String uniqueId;
    public String id;
    public String parentId;
    public String name;
    public String type;
    public RealmList<TaxonomyDataModel> taxonomyCategories;

    public TaxonomyDataModel() {

    }

    public TaxonomyDataModel(TaxonomyModel taxonomyModel) {
        updateCategory(taxonomyModel);
    }

    private void updateCategory(TaxonomyModel taxonomyModel) {
        uniqueId = taxonomyModel.uniqueId;

        if (taxonomyModel.getId() != null) {
            id = taxonomyModel.getId();
        }

        if (taxonomyModel.getType() != null) {
            type = taxonomyModel.getType();
        }

        if (taxonomyModel.parentId != null) {
            parentId = taxonomyModel.parentId;
        }

        if (taxonomyModel.name != null) {
            name = taxonomyModel.name;
        }

        taxonomyCategories = new RealmList<>();
        Realm realm = Realm.getDefaultInstance();
        for (String childModel : taxonomyModel.childTaxonomies) {
            TaxonomyDataModel taxonomyDataModel = realm.where(TaxonomyDataModel.class).equalTo("uniqueId", childModel).findFirst();
            if (taxonomyDataModel != null) {
                taxonomyCategories.add(realm.copyFromRealm(taxonomyDataModel));
            }
        }
        realm.close();
    }
}
