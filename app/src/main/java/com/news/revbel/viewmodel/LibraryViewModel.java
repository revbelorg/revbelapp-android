package com.news.revbel.viewmodel;

import java.util.Arrays;
import java.util.List;


public class LibraryViewModel extends ListedFilesViewModel {
    public static String pathToFolder() {
        return "RevBooks/";
    }

    @Override
    boolean getSorted() {
        return true;
    }

    @Override
    public List<FileListInfo> getCategories() {
        FileListInfo classic = new FileListInfo("40659", "Классика анархизма");
        FileListInfo history = new FileListInfo("40655", "История анархизма");
        FileListInfo celebrities = new FileListInfo("40664", "Знаменитые анархисты");
        FileListInfo anarchism = new FileListInfo("40667", "Современный анархизм");
        FileListInfo insurrectionism = new FileListInfo("40672", "Повстанчество");
        return Arrays.asList(classic, history, celebrities, anarchism, insurrectionism);
    }

    @Override
    public String getPath() {
        return "RevBooks/";
    }
}
