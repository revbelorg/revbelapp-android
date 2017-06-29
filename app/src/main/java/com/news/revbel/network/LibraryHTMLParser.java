package com.news.revbel.network;

import com.news.revbel.viewmodel.FileViewModel;
import com.news.revbel.viewmodel.ListedFilesViewModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class LibraryHTMLParser {
    public static ArrayList<FileViewModel> parseHTMLLibrary(String htmlText, String owner, ListedFilesViewModel model) {
        ArrayList<FileViewModel> libraryList = new ArrayList<>();

        Document document = Jsoup.parse(htmlText, Network.revbelUrl);
        Element body = document.body();
        Elements elements = body.getElementsByTag("li");
        int identifier = 0;
        for (Element element : elements) {
            Element hrefTagElement = element.getElementsByTag("a").first();
            String url = hrefTagElement.attributes().get("href");
            String title = hrefTagElement.ownText();
            Element image = element.getElementsByTag("img").first();
            String imageURL = null;
            float imageHeightAspect = 0;
            if (image != null) {
                int width = Integer.parseInt(image.attributes().get("width"));
                int height = Integer.parseInt(image.attributes().get("height"));
                imageHeightAspect = (float) height / width;
                imageURL = image.attributes().get("src");
            }
            FileViewModel book = new FileViewModel(identifier, title, url, imageURL, imageHeightAspect, owner, model);
            identifier++;
            libraryList.add(book);
        }
        return libraryList;
    }
}
