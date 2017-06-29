package com.news.revbel.viewmodel;

import java.util.Arrays;
import java.util.List;


public class AgitationListViewModel extends ListedFilesViewModel {
    public static String pathToFolder() {
        return "RevAgitation/";
    }

    @Override
    boolean getSorted() {
        return false;
    }

    @Override
    public List<FileListInfo> getCategories() {
        FileListInfo leaflets = new FileListInfo("43541", "Листовки");
        FileListInfo booklets = new FileListInfo("43545", "Брошюры");
        return Arrays.asList(leaflets, booklets);
    }

    @Override
    public String getPath() {
        return "RevAgitation/";
    }
}
