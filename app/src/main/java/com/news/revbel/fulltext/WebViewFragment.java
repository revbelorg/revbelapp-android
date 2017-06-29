package com.news.revbel.fulltext;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.news.revbel.R;
import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.network.Network;
import com.news.revbel.utilities.ControlActivityInterface;
import com.news.revbel.utilities.LinkedWebView;
import com.news.revbel.utilities.PostOpenInterface;
import com.news.revbel.viewmodel.PostModel;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class WebViewFragment extends Fragment {
    @Inject
    PostListCoordinator coordinator;

    @BindView(R.id.web_view) LinkedWebView webView;

    @BindColor(android.R.color.black) int blackColor;
    private boolean loadingURL = false;
    public static String WEB_VIEW_URL = "WEB_VIEW_URL";

    private ControlActivityInterface activityInterface;
    private String webURL;
    private Unbinder unbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RevApplication.getComponent().inject(this);

        if (getArguments().containsKey(WEB_VIEW_URL)) {
            webURL = getArguments().getString(WEB_VIEW_URL);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ControlActivityInterface) {
            activityInterface = ((ControlActivityInterface)context);
            activityInterface.updateControlButtonTapEvent(this, () -> {
                View view = (View) getView();
                if (view instanceof NestedScrollView) {
                    ((NestedScrollView) view).smoothScrollTo(0,0);
                }
            });
            activityInterface.showControlButton(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activityInterface != null) {
            activityInterface.onFragmentHide(this);
            activityInterface = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);

        unbinder = ButterKnife.bind(this, view);
        if (activityInterface != null) activityInterface.startLoading();

        webView.links = Arrays.asList(Network.OLD_SERVER_HOST, Network.NEW_SERVER_HOST);
        webView.setStaticPage(false);
        webView.loadingCallback = new LinkedWebView.LinkedWebViewCallback() {
            @Override
            public void onFinishCallback() {
                if (activityInterface != null) activityInterface.stopLoading();
            }

            @Override
            public void onLoadingRootURL(String url) {
                Uri uri = Uri.parse(Network.revbelUrl + url);
                showPostByLink(uri);
            }

            @Override
            public void onLoadedCallback(String url, Bitmap icon) {

            }

            @Override
            public void onLoadingListedURL(String host, Uri uri) {
                showPostByLink(uri);
            }

            @Override
            public void onLoadingURL(Uri uri) {

            }
        };

        try {
            if (savedInstanceState == null) {
                webView.loadUrl(webURL);
            }
        } catch (Exception e) {
            Log.w("Debug", "Failed to set webview proxy", e);
        }

        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_share);

        if (item != null) {
            ShareActionProvider shareProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
            String shareText = webURL;
            Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity())
                    .setType("text/plain").setText(shareText).getIntent();
            shareProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webView.loadData("", "text/html", "UTF-8");
        unbinder.unbind();
    }

    void showPostByLink(Uri link) {
        if (!loadingURL) {
            loadingURL = true;
            if (activityInterface != null) activityInterface.startLoading();
            List<String> pathList = link.getPathSegments();
            String slug = pathList.get(pathList.size() - 1);
            coordinator.getArticleBySlug(slug, new PostListCoordinator.ViewModelGetPostCallback() {
                @Override
                public void onFailure() {
                    loadingURL = false;
                    webView.loadUrl(link.toString());
                }

                @Override
                public void onSuccess(PostModel postModel) {
                    loadingURL = false;
                    if (activityInterface != null) activityInterface.stopLoading();
                    if (getActivity() instanceof PostOpenInterface) {
                        PostOpenInterface activity = (PostOpenInterface) getActivity();
                        activity.openFullPost(postModel, false);
                    }
                }
            });
        }
    }
}
