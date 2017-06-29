package com.news.revbel.about;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.news.revbel.R;
import com.news.revbel.feedback.PostPollActivity;
import com.news.revbel.utilities.ControlActivityInterface;
import com.news.revbel.utilities.LinkedWebView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class AboutUsFragment extends Fragment {
    @BindView(R.id.webview_about_us) LinkedWebView webView;

    private Unbinder unbinder;

    private ControlActivityInterface activityInterface;

    public static AboutUsFragment newInstance() {
        AboutUsFragment fragment = new AboutUsFragment();

        return fragment;
    }

    public AboutUsFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof ControlActivityInterface) {
            activityInterface = (ControlActivityInterface) getActivity();
        }
        if (activityInterface != null) {
            activityInterface.updateControlButtonTapEvent(this, () -> {
            });
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activityInterface != null) {
            activityInterface.onFragmentHide(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aboutus, container, false);
        unbinder = ButterKnife.bind(this, view);
        if (activityInterface != null) activityInterface.hideControlButton(this);
        webView.loadUrl("file:///android_asset/about.html");
        webView.setStaticPage(true);
        webView.loadingCallback = new LinkedWebView.LinkedWebViewCallback() {
            @Override
            public void onFinishCallback() {

            }

            @Override
            public void onLoadedCallback(String url, Bitmap icon) {

            }

            @Override
            public void onLoadingListedURL(String host, Uri uri) {

            }

            @Override
            public void onLoadingRootURL(String url) {

            }

            @Override
            public void onLoadingURL(Uri uri) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                getActivity().startActivity(intent);
            }
        };

        return view;
    }

    @OnClick(R.id.send_poll_button)
    void onPollButtonClick() {
        Intent intent = new Intent(getActivity(), PostPollActivity.class);
        getActivity().startActivity(intent);
    }
}
