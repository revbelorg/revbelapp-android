package com.news.revbel.network;

import android.content.Context;
import android.support.annotation.NonNull;

import com.news.revbel.coordinator.NetworkCoordinator;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import info.guardianproject.netcipher.proxy.OrbotHelper;

@Module
public class NetworkModule {
    @Provides
    @NonNull
    @Singleton
    public Network provideNetwork(Context context, OrbotHelper helper) {
        return new Network(context, helper);
    }

    @Provides
    @NonNull
    @Singleton
    public NetworkCoordinator provideNetworkCoordinator() {
        return new NetworkCoordinator();
    }

    @Provides
    @NonNull
    @Singleton
    public OrbotHelper provideOrbotHelper(Context context) {
        OrbotHelper helper = OrbotHelper.get(context);
        return helper;
    }
}
