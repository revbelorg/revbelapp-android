package com.news.revbel.viewmodel;

import android.databinding.ObservableBoolean;

import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.database.TaxonomyDataModel;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmResults;

public class CategoryListViewModel {
    @Inject NetworkCoordinator networkCoordinator;

    public TaxonomyModel rootTaxonomy;
    private int deepCounter;
    private String type;
    public ObservableBoolean isDownloaded = new ObservableBoolean();
    public ObservableBoolean isFirstDownloaded = new ObservableBoolean();
    final public HashMap<String, TaxonomyModel> listAll = new HashMap<>();

    public CategoryListViewModel(List<String> excludeCategories, String type) {
        RevApplication.getComponent().inject(this);

        this.type = type;
        isDownloaded.set(false);
        isFirstDownloaded.set(false);

        deepCounter = 0;

        Realm realm = Realm.getDefaultInstance();
        RealmResults<TaxonomyDataModel> resultList = realm.where(TaxonomyDataModel.class).equalTo("type", type).findAll();
        if (resultList.size() > 0) {
            for (TaxonomyDataModel taxonomyDataModel : resultList) {
                TaxonomyModel category = new TaxonomyModel(taxonomyDataModel);
                listAll.put(category.uniqueId, category);
            }
            String rootUniqueId = TaxonomyModel.uniqueIdWith("0", type);
            if (listAll.containsKey(rootUniqueId)) {
                rootTaxonomy = listAll.get(rootUniqueId);
                if (rootTaxonomy.childTaxonomies.size() > 0) {
                    firstDownloaded();
                }
            } else {
                rootTaxonomy = new TaxonomyModel("0", type, null, "Root");
            }
        } else {
            rootTaxonomy = new TaxonomyModel("0", type, null, "Root");
        }
        realm.close();

        updateCategory(rootTaxonomy, excludeCategories);
    }

    private void firstDownloaded() {
        if (!isFirstDownloaded.get()) {
            isFirstDownloaded.set(true);
        }
    }

    private void updateCategory(TaxonomyModel taxonomyModel, List<String> excludeCategories) {
        deepCounter++;
        listAll.put(taxonomyModel.uniqueId, taxonomyModel);
        networkCoordinator.getCategoriesForParent(taxonomyModel.getId(), type, excludeCategories, new NetworkCoordinator.NetworkCoordinatorCallback() {
            @Override
            public void onFailure() {

            }

            @Override
            public void onDatabaseResponse(List items) {
                taxonomyModel.updateChildFromData(items);
            }

            @Override
            public void onNetworkResponse(List items) {
                deepCounter--;
                List<TaxonomyModel> categories = (List<TaxonomyModel>) items;
                taxonomyModel.updateChildCategories(categories);
                for (final TaxonomyModel childModel : categories) {

                    updateCategory(childModel, excludeCategories);
                }
                if (taxonomyModel == rootTaxonomy) {
                    firstDownloaded();
                }
                if (deepCounter == 0) {
                    isDownloaded.set(true);
                }
            }
        });
    }
}
