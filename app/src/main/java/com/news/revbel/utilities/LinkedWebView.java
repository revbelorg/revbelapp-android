package com.news.revbel.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Collections;
import java.util.List;

import im.delight.android.webview.AdvancedWebView;

import static android.webkit.WebSettings.LOAD_NO_CACHE;

public class LinkedWebView extends AdvancedWebView {
    public interface LinkedWebViewCallback {
        void onFinishCallback();
        void onLoadedCallback(String url, Bitmap icon);
        void onLoadingListedURL(String host, Uri uri);
        void onLoadingRootURL(String url);
        void onLoadingURL(Uri uri);
    }

    public interface LinkedWebChromeCallback {
        void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback);
        void onHideCustomView();
    }
    private String loadingURL;

    public List<String> links;
    public LinkedWebViewCallback loadingCallback;
    public LinkedWebChromeCallback chromeCallback;
    private boolean staticPage;

    public void setStaticPage(boolean staticPage) {
        this.staticPage = staticPage;
        if (!staticPage) {
            this.getSettings().setSupportZoom(true);
            this.getSettings().setBuiltInZoomControls(true);
        }
    }

    public boolean customCSS = false;

    public LinkedWebView(Context context, boolean isStaticPage, List<String> links, LinkedWebViewCallback callback, LinkedWebChromeCallback chromeCallback) {
        this(context, isStaticPage, links, callback);
        this.chromeCallback = chromeCallback;
    }

    public LinkedWebView(Context context, boolean isStaticPage, List<String> links, LinkedWebViewCallback callback) {
        super(context);
        this.staticPage = isStaticPage;
        this.links = links;
        this.loadingCallback = callback;
        init();
    }

    public LinkedWebView(Context context) {
        super(context);
        init();
    }

    public LinkedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LinkedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (links == null) links = Collections.emptyList();
        setFocusable(false);
        getSettings().setDisplayZoomControls(false);
        getSettings().setAppCacheEnabled(false);
        getSettings().setCacheMode(LOAD_NO_CACHE);

        WebChromeClient webChromeClient = new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {

            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (chromeCallback != null) chromeCallback.onShowCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                if (chromeCallback != null) chromeCallback.onHideCustomView();

            }
        };
        this.setWebChromeClient(webChromeClient);

        WebViewClient webViewClient = new WebViewClient() {
            private int running = 0;

            private boolean callbackWithUrl(Uri uri) {
                String host = uri.getHost();
                String schema = uri.getScheme();
                boolean hasURI = links.contains(host);
                if (loadingCallback != null) {
                    if (schema.equals("file")) {
                        loadingCallback.onLoadingRootURL(uri.getPath());
                    } else if (hasURI) {
                        loadingCallback.onLoadingListedURL(host, uri);
                    } else {
                        loadingCallback.onLoadingURL(uri);
                    }
                }
                return hasURI;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                running++;
                Uri uri = Uri.parse(url);
                boolean result = callbackWithUrl(uri);
                return result || staticPage;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                running = Math.max(running, 1);

                if (loadingCallback != null) loadingCallback.onLoadedCallback(url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if((--running <= 0) && loadingCallback != null) {
                    loadingCallback.onFinishCallback();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if((--running <= 0) && loadingCallback != null) {
                    loadingCallback.onFinishCallback();
                }
            }
        };
        this.setWebViewClient(webViewClient);
    }

    @Override
    public void loadUrl(String url) {
        loadingURL = url;
        super.loadUrl(url);
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        data = customCSS ? updateWithDefaultCSS(data) : data;
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }


    private static String updateWithDefaultCSS(String body) {
        String htmldata = "<html>" + "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />" + "<body>" + body + "</body></html>";

        return htmldata;
    }
}
