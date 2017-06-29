package com.news.revbel.viewmodel;


import org.json.JSONException;
import org.json.JSONObject;
import com.news.revbel.RevApplication;
import com.news.revbel.database.TaxonomyDataModel;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

public class TaxonomyModel {
    public String uniqueId;
    private String id;
    private String type;
    public String parentId;
    public String name;
    public ArrayList<String> childTaxonomies = new ArrayList<>();

    TaxonomyModel(JSONObject object, boolean updateDatabase) throws JSONException {
        String taxonomyName = object.getString("name");
        String taxonomyId = String.valueOf(object.getInt("id"));
        String parent = "0";
        if (object.has("parent")) parent = String.valueOf(object.getInt("parent"));

        String type = object.getString("taxonomy");

        updateModel(taxonomyId, type, parent, taxonomyName, updateDatabase);
    }

    public TaxonomyModel(JSONObject object) throws JSONException {
        String taxonomyName = object.getString("name");
        String taxonomyId = String.valueOf(object.getInt("id"));
        String parent = "0";
        if (object.has("parent")) parent = String.valueOf(object.getInt("parent"));

        String type = object.getString("taxonomy");

        updateModel(taxonomyId, type, parent, taxonomyName, true);
    }

    TaxonomyModel(String id, String type, String parentId, String name) {
        updateModel(id, type, parentId, name, true);
    }

    TaxonomyModel(TaxonomyDataModel taxonomyData) {
        this.uniqueId = taxonomyData.uniqueId;
        this.id = taxonomyData.id;
        this.name = taxonomyData.name;
        this.parentId = taxonomyData.parentId;
        this.type = taxonomyData.type;
        for (TaxonomyDataModel childDataModel : taxonomyData.taxonomyCategories) {
            childTaxonomies.add(childDataModel.uniqueId);
        }
    }

    private void updateModel(String id, String type, String parentId, String name, boolean updateDatabase) {
        createUniqueId(id, type);
        this.parentId = parentId;
        this.name = name;
        if (updateDatabase) createData();
    }

    private void createData() {
        TaxonomyDataModel taxonomyDataModel = new TaxonomyDataModel(this);

        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(taxonomyDataModel));
        realm.close();
    }

    void updateChildCategories(final List<TaxonomyModel> newChilds) {
        childTaxonomies.clear();
        for (TaxonomyModel child : newChilds) {
            childTaxonomies.add(child.uniqueId);
        }
        TaxonomyDataModel taxonomyDataModel = new TaxonomyDataModel(this);

        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(realm1 -> realm1.copyToRealmOrUpdate(taxonomyDataModel), realm::close, error -> realm.close());
    }

    void updateChildFromData(List<TaxonomyDataModel> newChildCategories) {
        childTaxonomies.clear();
        for (TaxonomyDataModel childModel : newChildCategories) {
            childTaxonomies.add(childModel.uniqueId);
        }
    }

    private void createUniqueId(String id, String type) {
        this.id = id;
        this.type = type;
        this.uniqueId = uniqueIdWith(id, type);
    }

    public static String uniqueIdWith(String id, String type) {
        return  id + "_" + type;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
