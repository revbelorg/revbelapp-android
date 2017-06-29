package com.news.revbel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;

import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.coordinator.SettingsCoordinator;
import com.news.revbel.favorite.FavoriteListFragment;
import com.news.revbel.feedback.PostNewsFragment;
import com.news.revbel.feedback.PostPollActivity;
import com.news.revbel.fulltext.FullPostFragment;
import com.news.revbel.fulltext.WebViewFragment;
import com.news.revbel.filelist.FileListFragment;
import com.news.revbel.network.NetworkModule;
import com.news.revbel.postlist.PostListFragment;
import com.news.revbel.preferences.ClearDataDialog;
import com.news.revbel.viewmodel.BanditsListViewModel;
import com.news.revbel.viewmodel.FileViewModel;
import com.news.revbel.viewmodel.CategoryListViewModel;
import com.news.revbel.viewmodel.ListedFilesViewModel;
import com.news.revbel.viewmodel.PostListViewModel;
import com.news.revbel.viewmodel.PostModel;
import com.news.revbel.viewmodel.PostModule;
import com.news.revbel.viewmodel.ReplyListViewModel;

import javax.inject.Singleton;

import dagger.Component;
import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

import static com.news.revbel.coordinator.SettingsCoordinator.USE_CRASHLYTICS;

public class RevApplication extends Application {
    final private static Handler UIHandler;

    static {
        UIHandler = new Handler(Looper.getMainLooper());
    }
    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }

    private static RevApplication instance;
    public static RevApplication getInstance() {
        return instance;
    }

    private static AppComponent mAppComponents;
    public static AppComponent getComponent() {
        return mAppComponents;
    }

    @Component(modules = {AppModule.class, NetworkModule.class, PostModule.class})
    @Singleton
    public interface AppComponent {
        void inject(WebViewFragment activity);
        void inject(MainActivity activity);

        void inject(PostListFragment postListFragment);
        void inject(FavoriteListFragment fragmentPostLists);
        void inject(FullPostFragment fullPostFragment);
        void inject(FileListFragment fileListFragment);
        void inject(PostNewsFragment postNewsFragment);
        void inject(PostPollActivity postPollActivity);

        void inject(CategoryListViewModel categoryListViewModel);
        void inject(PostListViewModel postListViewModel);
        void inject(FileViewModel fileViewModel);
        void inject(ListedFilesViewModel listedFilesViewModel);
        void inject(ReplyListViewModel replyListViewModel);
        void inject(BanditsListViewModel banditsListViewModel);
        void inject(PostModel postModel);

        void inject(ClearDataDialog dialog);

        void inject(NetworkCoordinator networkCoordinator);
        void inject(SettingsCoordinator settingsCoordinator);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Scada-Regular.ttf")
                .setFontAttrId(com.news.revbel.R.attr.fontPath)
                .build()
        );

        buildDatabase();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hasUseCrashLytics = sharedPref.getBoolean(USE_CRASHLYTICS, false);
        if (hasUseCrashLytics) Fabric.with(this, new Crashlytics());

        mAppComponents = buildComponents();
        instance = this;
    }

    public void buildDatabase(){
        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    protected AppComponent buildComponents() {
        return DaggerRevApplication_AppComponent.builder()
                .postModule(new PostModule())
                .appModule(new AppModule(this))
                .networkModule(new NetworkModule()).build();
    }
}
