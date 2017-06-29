package com.news.revbel;

import android.content.Context;
import android.support.annotation.NonNull;

import com.news.revbel.coordinator.SettingsCoordinator;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {
    private Context appContext;

    public AppModule(@NonNull Context context) {
        appContext = context;
    }

    @Provides
    @Singleton
    Context provideContext() {
        return appContext;
    }

    @Provides
    @Singleton
    SettingsCoordinator provideSettingCoordinator() {
        return new SettingsCoordinator();
    }
}
