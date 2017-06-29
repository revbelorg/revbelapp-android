package com.news.revbel.composers;

import android.app.Activity;
import android.databinding.Observable;
import android.databinding.ObservableInt;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.news.revbel.MainActivity;
import com.news.revbel.fulltext.FullPostFragment;
import com.news.revbel.fulltext.GalleryFragment;
import com.news.revbel.network.Network;
import com.news.revbel.utilities.LinkedWebView;
import com.news.revbel.utilities.MediaFullscreenProtocol;
import com.news.revbel.utilities.ViewUtilities;
import com.news.revbel.viewmodel.ImageModel;
import com.news.revbel.R;
import com.news.revbel.viewmodel.PostModel;
import com.wang.avi.AVLoadingIndicatorView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FullTextViewComposer {
    private ArrayList<Runnable> loadingCompleted = new ArrayList<>();
    private ObservableInt loadingComponents = new ObservableInt(0);
    private ArrayList<Fragment> fragments = new ArrayList<>();

    private LinearLayout containerView;
    private Activity activity;
    private FullPostFragment fragment;
    private Element text;
    private int position;

    private AsyncTask<String, Void, Boolean> asyncTask;

    public FullTextViewComposer(Activity activity, FullPostFragment fragment, LinearLayout containerView) {
        this.activity = activity;
        this.fragment = fragment;
        this.containerView = containerView;
    }

    public void cleanUpView() {
        if (fragments.size() > 0) {
            FragmentTransaction transaction = fragment.getChildFragmentManager().beginTransaction();
            for (Fragment childFragment : fragments) {

                transaction = transaction.remove(childFragment);

            }
            transaction.commitAllowingStateLoss();
        }
        containerView.removeAllViewsInLayout();
    }

    private boolean parseImageElementToView(Element element) {
        String url = element.attributes().get("src");
        final String newUrl = url.replace(Network.OLD_SERVER_HOST, Network.revbelUrl);

        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;
            for (PostModel post : mainActivity.currentPosts.values()) {
                String featureUrl =  post.featuredMediaUrl;

                if (featureUrl != null && !mainActivity.mTwoPane) {
                    if (featureUrl.equals(newUrl)) return false;
                    featureUrl = featureUrl.substring(0, featureUrl.lastIndexOf("-"));
                    if (featureUrl.equals(newUrl.substring(0, newUrl.lastIndexOf("."))))
                        return false;
                }
            }
        }

        loadingCompleted.add(() -> {
            RelativeLayout relativeLayout = new RelativeLayout(activity);
            relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            AVLoadingIndicatorView loadingIndicatorView = ViewUtilities.indicatorViewForMedia(activity, relativeLayout);
            loadingIndicatorView.smoothToShow();

            ImageView imageView = new ImageView(activity);
            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            Glide
                    .with(activity)
                    .load(newUrl)
                    .fitCenter()
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            loadingIndicatorView.smoothToHide();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            loadingIndicatorView.smoothToHide();
                            return false;
                        }
                    })
                    .into(imageView);

            relativeLayout.addView(imageView, params);
            containerView.addView(relativeLayout);
        });
        return true;
    }

    private void parseVideoElementToView(Element frame) {
        Element div = new Element(Tag.valueOf("div"), "revbel.org");
        div.attr("style","position:relative;padding-bottom:56.25%;padding-top:35px;height:0;overflow: hidden");

        frame.attr("style", "position:absolute;top:0;left:0;width:100%;height:100%");
        div.appendChild(frame);

        String html = div.outerHtml();

        loadingCompleted.add(() -> {
            RelativeLayout layout = new RelativeLayout(activity);
            layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinkedWebView youtubeView = new LinkedWebView(activity);

            WebChromeClient webChromeClient = new WebChromeClient() {

                @Override
                public void onProgressChanged(WebView view, int progress) {

                }

                @Override
                public void onShowCustomView(View view, CustomViewCallback callback) {
                    if (activity instanceof MediaFullscreenProtocol) {
                        ((MediaFullscreenProtocol) activity).showMediaFullscreen(view, callback, true, false);
                    }
                }

                @Override
                public void onHideCustomView() {
                    String js = "javascript:";
                    js += "var _ytrp_html5_video = document.getElementsByTagName('video')[0];";
                    js += "_ytrp_html5_video.webkitExitFullscreen();";
                    youtubeView.loadUrl(js);
                    if (activity instanceof MediaFullscreenProtocol) {
                        ((MediaFullscreenProtocol) activity).hideMediaFullscreen();
                    }
                }
            };
            youtubeView.setStaticPage(false);
            youtubeView.setWebChromeClient(webChromeClient);

            youtubeView.setBackgroundColor(ResourcesCompat.getColor(activity.getResources(), android.R.color.transparent, null));
            youtubeView.loadDataWithBaseURL("file:///android_asset/", html , "text/html", "UTF-8", null);

            layout.addView(youtubeView);

            AVLoadingIndicatorView loader = ViewUtilities.indicatorViewForMedia(activity, layout);
            WebViewClient webViewClient = new WebViewClient() {
                private int running = 0;
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    running = Math.max(running, 1);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    if (--running <= 0) {
                        loader.smoothToHide();
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    if (--running <= 0) {
                        loader.smoothToHide();
                    }
                }
            };
            youtubeView.setWebViewClient(webViewClient);
            loader.smoothToShow();

            containerView.addView(layout);
        });
    }

    private void parseTextElementToView(Element element) {
        String html = element.outerHtml();

        loadingCompleted.add(() -> {
            LinkedWebView textView = new LinkedWebView(activity, true, Arrays.asList(Network.OLD_SERVER_HOST, Network.NEW_SERVER_HOST), new LinkedWebView.LinkedWebViewCallback() {
                @Override
                public void onFinishCallback() {
                    loadingComponents.set(loadingComponents.get() - 1);
                }

                @Override
                public void onLoadedCallback(String url, Bitmap icon) {

                }

                @Override
                public void onLoadingRootURL(String url) {
                    Uri uri = Uri.parse("https://" + Network.revbelUrl + url);
                    fragment.showPostByLink(uri);
                }

                @Override
                public void onLoadingListedURL(String host, Uri uri) {
                    fragment.showPostByLink(uri);
                }

                @Override
                public void onLoadingURL(Uri uri) {
                    fragment.showWebURL(uri);
                }
            });

            textView.customCSS = true;
            textView.setBackgroundColor(ContextCompat.getColor(activity, R.color.recycleBackground));

            textView.loadDataWithBaseURL("file:///android_asset/", html , "text/html", "UTF-8", null);
            loadingComponents.set(loadingComponents.get() + 1);
            containerView.addView(textView);
        });
    }

    private void createImageGallery(List<ImageModel> imageModels, String id) {
        GalleryFragment fragmentGallery = GalleryFragment.newInstance(imageModels);
        if (!isFragmentAttached()) return;

        FragmentTransaction transaction = fragment.getChildFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_full_post, fragmentGallery, id).commitNow();
        fragments.add(fragmentGallery);
    }

    private void parseFusionGalleryElementToView(Element element, int position) {
        Elements galleryThumbs = element.getElementsByClass("fusion-image-wrapper");
        ArrayList<ImageModel> imageModels = new ArrayList<>();
        for (Element galleryThumb : galleryThumbs) {
            Elements links = galleryThumb.getElementsByTag("a");
            String link = links.first().attributes().get("href");
            Elements images = galleryThumb.getElementsByTag("img");
            String thumb = images.first().attributes().get("src");
            ImageModel imageModel = new ImageModel();
            imageModel.imageUrl = link;
            imageModel.thumbUrl = Uri.parse(thumb);
            imageModels.add(imageModel);
        }

        loadingCompleted.add(() -> {
            createImageGallery(imageModels, "fusion" + position);
        });
    }

    private void parseNGGalleryElementToView(Element element) {
        Elements galleryThumbs = element.getElementsByClass("ngg-gallery-thumbnail");
        final String id = element.attributes().get("id");
        ArrayList<ImageModel> imageModels = new ArrayList<>();
        for (Element galleryThumb : galleryThumbs) {
            Elements links = galleryThumb.getElementsByTag("a");
            String link = links.first().attributes().get("href");
            Elements images = galleryThumb.getElementsByTag("img");
            String thumb = images.first().attributes().get("src");
            ImageModel imageModel = new ImageModel();
            imageModel.imageUrl = link;
            imageModel.thumbUrl = Uri.parse(thumb);
            imageModels.add(imageModel);
        }

        loadingCompleted.add(() -> {
            createImageGallery(imageModels, id);
        });
    }

    private void parseTextElement() {
        if (text.children().size() > 0) {
            parseTextElementToView(text);
            text = new Element(Tag.valueOf("div"), "revbel.org");
            position++;
        }
    }

    private boolean hasMediaElements(Element element) {
        Elements wistiaElements = element.getElementsByClass("wistia_responsive_padding");
        Elements youTubeElements = element.getElementsByTag("iframe");
        Elements galleryElements = element.getElementsByClass("ngg-galleryoverview");
        Elements imageElements = element.getElementsByTag("img");
        Elements fusionGalleryElements = element.getElementsByTag("fusion-image-carousel");
        Elements videoElements = element.getElementsByTag("video");
        return wistiaElements.size() > 0 || youTubeElements.size() > 0 || galleryElements.size() > 0 || imageElements.size() > 0 || fusionGalleryElements.size() > 0 || videoElements.size() > 0;
    }

    private void parseMediaElement(Element element) {
        if (hasMediaElements(element)) {
            parseTextElement();
        }
        Elements wistiaElements = element.getElementsByClass("wistia_responsive_padding");
        for (Element wistiaElement : wistiaElements) {
            parseTextElementToView(element);
            wistiaElement.remove();
            position++;
        }

        Elements youTubeElements = element.getElementsByTag("iframe");
        for (Element youTubeElement : youTubeElements) {
            parseVideoElementToView(youTubeElement);
            youTubeElement.remove();
            position++;
        }

        Elements videoElements = element.getElementsByTag("video");
        for (Element videoElement : videoElements) {
            parseVideoElementToView(videoElement);
            videoElement.remove();
            position++;
        }

        Elements galleryElements = element.getElementsByClass("ngg-galleryoverview");

        if (galleryElements.size() > 0) {
            for (Element galleryElement : galleryElements) {
                parseNGGalleryElementToView(galleryElement);

                position++;
            }
            element.getElementsByClass("ngg-galleryoverview").remove();
        } else {
            Elements fusionElements = element.getElementsByClass("fusion-image-carousel");

            if (fusionElements.size() > 0) {
                for (Element galleryElement : fusionElements) {
                    parseFusionGalleryElementToView(galleryElement, position);

                    position++;
                }
                element.getElementsByClass("fusion-image-carousel").remove();
            } else {
                Elements imageElements = element.getElementsByTag("img");

                for (Element imageElement : imageElements) {
                    if (parseImageElementToView(imageElement)) {
                        position++;
                    }
                    imageElement.remove();
                }
            }
        }
    }

    public void loadHTMLString(String htmlString, Runnable callback) {
        if (htmlString != null) {
            asyncTask = new AsyncTask<String, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    position = 0;
                }

                @Override
                protected Boolean doInBackground(String... strings) {
                    Document document = Jsoup.parse(strings[0], Network.revbelUrl);
                    text = new Element(Tag.valueOf("div"), "revbel.org");

                    Element body = document.body();
                    List<Node> elements = body.childNodes();
                    while (elements.size() > 0){
                        Node element = elements.get(0);

                        if (this.isCancelled()) return false;

                        if (element instanceof Element) {
                            parseMediaElement((Element) element);
                            if (element.parentNode() == null) {
                                continue;
                            }
                        }
                        Node clone = element.clone();
                        text.appendChild(clone);
                        element.remove();
                    }

                    text.appendText(body.text());
                    if (this.isCancelled()) return false;
                    parseTextElement();
//                    if (activity.isImmersive())
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);

                    if (!result || !isFragmentAttached()) return;

                    for (Runnable run : FullTextViewComposer.this.loadingCompleted) {
                        run.run();
                    }

                    FullTextViewComposer.this.loadingCompleted.clear();
                    FullTextViewComposer.this.loadingComponents.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                        @Override
                        public void onPropertyChanged(Observable observable, int i) {
                            if (!isFragmentAttached()) return;
                            if (loadingComponents.get() == 0) {
                                callback.run();
                            }
                        }
                    });
                }
            };
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, htmlString);
        }
    }

    private boolean isFragmentAttached() {
        return !fragment.isRemoving() && !fragment.isDetached() && fragment.isAdded();
    }
}
