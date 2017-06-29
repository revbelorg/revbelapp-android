package com.news.revbel.network;

import android.content.Context;
import android.databinding.Observable;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;
import com.news.revbel.utilities.ProxyUtilities;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;

import okhttp3.OkHttpClient;

public class TorifiedGlideModule implements GlideModule {
    @Override public void applyOptions(Context context, GlideBuilder builder) {
        Log.d("Log", "Apply Options!");

    }

    @Override public void registerComponents(Context context, Glide glide) {
        registerClient(context, glide);
        Network.usingTor.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                registerClient(context, glide);
            }
        });
    }

    private void registerClient(Context context, Glide glide) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (Network.usingTor.get() == Network.TorSettings.TOR_SOCKS) {
            builder = builder.socketFactory(ProxyUtilities.torSocksFactory());
        } else if (Network.usingTor.get() == Network.TorSettings.TOR_SOCKS) {
            builder = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8118)));
        }
        OkHttpClient client = builder.build();
        glide.register(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
    }
}