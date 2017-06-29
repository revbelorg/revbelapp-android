package com.news.revbel.utilities;

import android.net.Uri;

import com.news.revbel.viewmodel.PostModel;

public interface PostOpenInterface {
    void openFullPost(PostModel postId, boolean clear);
    void openWebURL(String webURL);
    void openLink(Uri link, Runnable onFinish);
}
