package com.news.revbel.utilities;

import android.view.View;
import android.webkit.WebChromeClient;

public interface MediaFullscreenProtocol {
    void showMediaFullscreen(View view, WebChromeClient.CustomViewCallback callback, boolean landscape, boolean cancelOnTap);
    void hideMediaFullscreen();
}
