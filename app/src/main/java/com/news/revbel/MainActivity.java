package com.news.revbel;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.news.revbel.about.AboutUsFragment;
import com.news.revbel.bandaluki.BandaLukiListFragment;
import com.news.revbel.coordinator.SettingsCoordinator;
import com.news.revbel.dialogs.OpenBookDialogFragment;
import com.news.revbel.dialogs.TorDialogFragment;
import com.news.revbel.donate.DonateFragment;
import com.news.revbel.favorite.FavoriteListFragment;
import com.news.revbel.feedback.PostNewsFragment;
import com.news.revbel.filelist.FilesListener;
import com.news.revbel.fulltext.FullPostFragment;
import com.news.revbel.fulltext.WebViewFragment;
import com.news.revbel.filelist.FileListFragment;
import com.news.revbel.network.Network;
import com.news.revbel.pdfview.DocumentActivity;
import com.news.revbel.postlist.PostListFragment;
import com.news.revbel.preferences.SettingsActivity;
import com.news.revbel.utilities.ControlActivityInterface;
import com.news.revbel.utilities.ControllableAppBarLayout;
import com.news.revbel.utilities.PostOpenInterface;
import com.news.revbel.utilities.Utilities;
import com.news.revbel.utilities.MediaFullscreenProtocol;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.utilities.ViewUtilities;
import com.news.revbel.viewmodel.AgitationListViewModel;
import com.news.revbel.viewmodel.FileViewModel;
import com.news.revbel.viewmodel.LibraryViewModel;
import com.news.revbel.viewmodel.PostModel;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javax.inject.Inject;

import butterknife.BindBool;
import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.news.revbel.coordinator.SettingsCoordinator.NETWORK_USE_MIRROR_KEY;
import static com.news.revbel.coordinator.SettingsCoordinator.NETWORK_USE_TOR_KEY;
import static com.news.revbel.coordinator.SettingsCoordinator.NETWORK_USE_VPN_KEY;
import static com.news.revbel.coordinator.SettingsCoordinator.OPEN_INTERNAL_PDF;
import static com.news.revbel.coordinator.SettingsCoordinator.USE_CRASHLYTICS;
import static com.news.revbel.fulltext.WebViewFragment.WEB_VIEW_URL;
import static com.news.revbel.network.Network.REVBEL_URL;

public class MainActivity extends AppCompatActivity implements MediaFullscreenProtocol, PostOpenInterface, ControlActivityInterface {
    @Inject PostListCoordinator coordinator;
    @Inject OrbotHelper orbotHelper;
    @Inject SettingsCoordinator settingsCoordinator;

    @BindView(com.news.revbel.R.id.nav_view) NavigationView navigationView;
    @BindView(com.news.revbel.R.id.drawer_owner) DrawerLayout drawer;
    @BindView(com.news.revbel.R.id.video_layout) LinearLayout videoLayout;

    @BindView(com.news.revbel.R.id.container) RelativeLayout container;
    @BindView(com.news.revbel.R.id.post_detail_container) RelativeLayout detailContainer;

    @BindView(com.news.revbel.R.id.app_bar) ControllableAppBarLayout appBarLayout;
    @BindView(com.news.revbel.R.id.toolbar_layout) CollapsingToolbarLayout toolbarLayout;
    @BindView(com.news.revbel.R.id.toolbar) Toolbar toolbar;
    @BindView(com.news.revbel.R.id.toolbar_details_title) TextView toolbarTitle;
    @BindView(com.news.revbel.R.id.toolbar_image) ImageView featuredImage;

    @BindView(com.news.revbel.R.id.coordinator) CoordinatorLayout coordinatorLayout;
    @BindView(com.news.revbel.R.id.progress_bar) AVLoadingIndicatorView loadingIndicator;
    @BindView(com.news.revbel.R.id.scroll_to_top) FloatingActionButton controlButton;
    @BindView(com.news.revbel.R.id.close_button) FloatingActionButton closeButton;

    @BindString(com.news.revbel.R.string.download_progress_dialog_title) String downloadProgressTitle;
    @BindString(com.news.revbel.R.string.download_progress_dialog_message) String downloadProgressMessage;
    @BindString(com.news.revbel.R.string.download_progress_dialog_success) String downloadProgressSuccess;
    @BindString(com.news.revbel.R.string.download_progress_dialog_cancel) String downloadProgressCancel;
    @BindString(com.news.revbel.R.string.download_progress_dialog_error) String downloadProgressError;
    @BindString(com.news.revbel.R.string.download_dialog_title) String downloadTitle;
    @BindString(com.news.revbel.R.string.download_dialog_cancel) String downloadCancelMessage;
    @BindString(com.news.revbel.R.string.download_dialog_message) String downloadMessage;
    @BindString(com.news.revbel.R.string.crashlytics_dialog_title) String crashlyticsTitle;
    @BindString(com.news.revbel.R.string.crashlytics_dialog_message) String crashlyticsMessage;
    @BindString(com.news.revbel.R.string.book_no_external_app) String noExternalPDFMessage;
    @BindString(com.news.revbel.R.string.file_downloaded) String fileDownloadedMessage;
    @BindString(com.news.revbel.R.string.choose_app) String chooseMessage;
    @BindBool(com.news.revbel.R.bool.isTablet) public boolean mTwoPane;
    @BindColor(android.R.color.black) int blackColor;
    @BindColor(android.R.color.white) int whiteColor;

    private ActionBarDrawerToggle actionBarDrawerToggle;
    private DrawerArrowDrawable drawerArrow;
    private ProgressDialog downloadFileDialog;

    public FilesListener filesListener;

    private enum CurrentScreen {
        MAIN, FAVORITE, BANDALUKI, LIBRARY, AGITATION, DONATE, ABOUT_US, POST_NEWS;

        String getTag() {
            switch (this) {
                case MAIN:
                    return "MAIN";
                case FAVORITE:
                    return "FAVORITE";
                case BANDALUKI:
                    return "BANDALUKI";
                case LIBRARY:
                    return "LIBRARY";
                case AGITATION:
                    return "AGITATION";
                case POST_NEWS:
                    return "POST_NEWS";
                case DONATE:
                    return "DONATE";
                case ABOUT_US:
                    return "ABOUT_US";
            }
            return null;
        }

        int getItem() {
            switch (this) {
                case MAIN:
                    return 0;
                case FAVORITE:
                    return 1;
                case BANDALUKI:
                    return 2;
                case LIBRARY:
                    return 3;
                case AGITATION:
                    return 4;
                case POST_NEWS:
                    return 5;
                case DONATE:
                    return 6;
                case ABOUT_US:
                    return 8;
            }
            return 0;
        }

        static CurrentScreen getScreen(int i) {
            switch (i) {
                case 0:
                    return MAIN;
                case 1:
                    return FAVORITE;
                case 2:
                    return BANDALUKI;
                case 3:
                    return LIBRARY;
                case 4:
                    return AGITATION;
                case 5:
                    return POST_NEWS;
                case 6:
                    return DONATE;
                case 8:
                    return ABOUT_US;
            }
            return MAIN;
        }
    }

    private CurrentScreen currentScreen = CurrentScreen.MAIN;
    private Fragment currentFragment;
    public HashMap<String, PostModel> currentPosts = new HashMap<>();
    private Stack<String> keyStack = new Stack<>();

    private final static String STACK_NAME = "STACK_NAME";
    private final static String CURRENT_SCREEN = "CURRENT_SCREEN";

    private float lastTranslate = 0.0f;
    private ControllableAppBarLayout.State lastState = ControllableAppBarLayout.State.COLLAPSED;

    private Runnable closeDrawerCallback;

    private class FragmentEventWrapper {
        Fragment fragment;
        Runnable event;

        FragmentEventWrapper(Fragment fragment, Runnable event) {
            this.fragment = fragment;
            this.event = event;
        }
    }

    private TreeMap<Integer,FragmentEventWrapper> controlledFragments = new TreeMap<>();

    private WebChromeClient.CustomViewCallback endVideoCallback;
    private Dialog videoFullscreenDialog;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(CURRENT_SCREEN, currentScreen.getItem());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RevApplication.getComponent().inject(this);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        toolbarLayout.setTitleEnabled(false);

        drawerArrow = new DrawerArrowDrawable(this);
        drawerArrow.setColor(whiteColor);

        toolbar.setNavigationIcon(drawerArrow);

        setSupportActionBar(toolbar);

        setUpNavigationView();

        FloatingActionButton closeButton = (FloatingActionButton) findViewById(R.id.close_button);
        closeButton.setOnClickListener(view -> closeFullPost());

        controlButton.setOnClickListener(view -> {
            if (controlledFragments.size() > 0) {
                controlledFragments.firstEntry().getValue().event.run();
            }
        });

        appBarLayout.setOnStateChangeListener(this::updateToolbarTitle);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(blackColor);
        }

        if (savedInstanceState == null) {
            showCurrentScreen();
            settingsCoordinator.init();

            checkAndOpenCrashlyticsDialog(() -> checkAndOpenNetworkDialog(() -> {
                Intent intent = getIntent();
//            String action = intent.getAction();
                Uri data = intent.getData();
                if (data != null) {
                    if (data.toString().contains(Network.BANDALUKI_URL) && data.getPathSegments().size() > 0) {
                        forceCurrentScreen(CurrentScreen.BANDALUKI);
                        openLink(data, null);
                    } else if (data.toString().contains(Network.BANDALUKI_URL)) {
                        forceCurrentScreen(CurrentScreen.BANDALUKI);
                    } else if (data.toString().contains(Network.REVBEL_URL) && data.getPathSegments().size() > 0) {
                        forceCurrentScreen(CurrentScreen.MAIN);
                        openLink(data, null);
                    } else if (data.toString().contains(Network.REVBEL_URL)) {
                        forceCurrentScreen(CurrentScreen.MAIN);
                    }
                }
            }));
        } else {
            stopLoading();
            int current = savedInstanceState.getInt(CURRENT_SCREEN);
            currentScreen = CurrentScreen.getScreen(current);
            updateScreenInterface();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (filesListener != null) filesListener.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //region Activity results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
    //endregion

    //region Navigation drawer logic
    private void setUpNavigationView() {
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.nav_home:
                    currentScreen = CurrentScreen.MAIN;
                    break;
                case R.id.nav_favorite:
                    currentScreen = CurrentScreen.FAVORITE;
                    break;
                case R.id.nav_bandaluki:
                    currentScreen = CurrentScreen.BANDALUKI;
                    break;
                case R.id.nav_agitation:
                    currentScreen = CurrentScreen.AGITATION;
                    break;
                case R.id.nav_library:
                    currentScreen = CurrentScreen.LIBRARY;
                    break;
                case R.id.nav_donate:
                    currentScreen = CurrentScreen.DONATE;
                    break;
                case R.id.nav_postnews:
                    currentScreen = CurrentScreen.POST_NEWS;
                    break;
                case R.id.nav_settings: {
                    closeDrawerCallback = () -> {
                        Intent intent = new Intent(this, SettingsActivity.class);
                        startActivity(intent);
                    };
                    drawer.closeDrawers();
                    return true;
                }
                case R.id.nav_about_us:
                    currentScreen = CurrentScreen.ABOUT_US;
                    break;
                default:
                    currentScreen = CurrentScreen.MAIN;
            }

            hideKeyboard();
            closeFullPost();
            setCurrentOrientation();

            drawer.closeDrawers();
            if (menuItem.isChecked()) {
                menuItem.setChecked(false);
            } else {
                menuItem.setChecked(true);
            }
            menuItem.setChecked(true);

            closeDrawerCallback = this::showCurrentScreen;

            return true;
        });

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer,toolbar,  R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                hideKeyboard();
                float moveFactor = (drawerView.getWidth() * slideOffset);

                TranslateAnimation anim = new TranslateAnimation(lastTranslate, moveFactor, 0.0f, 0.0f);
                anim.setDuration(0);
                anim.setFillAfter(true);
                coordinatorLayout.startAnimation(anim);

                lastTranslate = moveFactor;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                if (closeDrawerCallback != null) {
                    closeDrawerCallback.run();
                    closeDrawerCallback = null;
                }
            }

            @Override
            public boolean onOptionsItemSelected(MenuItem item) {
                return super.onOptionsItemSelected(item);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        actionBarDrawerToggle.setToolbarNavigationClickListener(view -> {
            if (!mTwoPane) closeFullPost();
        });

        drawer.addDrawerListener(actionBarDrawerToggle);

        actionBarDrawerToggle.syncState();
    }

    private void forceCurrentScreen(CurrentScreen screen) {
        currentScreen = screen;
        closeFullPost();
        showCurrentScreen();
    }

    private void updateScreenInterface() {
        setCurrentOrientation();

        MenuItem item = navigationView.getMenu().getItem(currentScreen.getItem());
        item.setChecked(true);

        invalidateOptionsMenu();
        setCurrentTitle();
    }

    private void showCurrentScreen() {
        FragmentManager fm = getSupportFragmentManager();

        if (mTwoPane) {
            keyStack.clear();
            currentPosts.clear();
        } else {
            featuredImage.setVisibility(View.GONE);
        }

        while (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
        }

        currentFragment = getCurrentScreenFragment();
        if (currentFragment != null) {
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            fragmentTransaction.replace(R.id.container, currentFragment, currentScreen.getTag())
                    .commit();
        }

        updateScreenInterface();
    }

    private Fragment getCurrentScreenFragment() {
        switch (currentScreen) {
            case MAIN:
                return PostListFragment.newInstance();
            case FAVORITE:
                return FavoriteListFragment.newInstance();
            case BANDALUKI:
                return BandaLukiListFragment.newInstance();
            case LIBRARY:
                return FileListFragment.newInstance(new LibraryViewModel());
            case AGITATION:
                return FileListFragment.newInstance(new AgitationListViewModel());
            case DONATE:
                return DonateFragment.newInstance();
            case POST_NEWS:
                return PostNewsFragment.newInstance();
            case ABOUT_US:
                return AboutUsFragment.newInstance();
        }
        return null;
    }

    private boolean isBandaLuki() {
       return currentScreen == CurrentScreen.BANDALUKI && keyStack.size() == 0;
    }

    private boolean isPost() {
        return coordinator.currentListViewModel().hasOpenedDetails.get();
    }

    private void setCurrentOrientation() {
        if (isBandaLuki() && !mTwoPane) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (!mTwoPane) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (isBandaLuki()) {
            inflater.inflate(R.menu.search_menu, menu);
            return true;
        } else if (isPost()) {
            inflater.inflate(R.menu.share_menu, menu);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }
    }
    //endregion

    //region Animation and view updates
    private void animateBackButton(boolean toBack, Runnable onFinishCallback) {
        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        if ((drawerArrow.getProgress() == 1 && toBack) || (drawerArrow.getProgress() == 0 && !toBack)){
            if (onFinishCallback != null) onFinishCallback.run();
            return;
        }

        anim.addUpdateListener(animation -> {
            // Use animation position to blend colors.
            float fraction = animation.getAnimatedFraction();
            float position = toBack ? fraction : 1 - fraction;
            drawerArrow.setProgress(position);
        });
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (onFinishCallback != null) onFinishCallback.run();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                if (onFinishCallback != null) onFinishCallback.run();
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        anim.setDuration(300).start();
    }

    private void animateToolbar(boolean collapsed) {
        toolbarTitle.animate().alpha(collapsed ? 1 : 0);
    }

    private void setCurrentTitle() {
        MenuItem item = navigationView.getMenu().getItem(currentScreen.getItem());
        toolbarTitle.setText(item.getTitle());
    }


    private void updateToolbarTitle(ControllableAppBarLayout.State state) {
        if (featuredImage.getVisibility() != View.GONE) {
            switch (state) {
                case COLLAPSED:
                    if (lastState != ControllableAppBarLayout.State.COLLAPSED) {
                        animateToolbar(true);
                    }
                    break;
                case EXPANDED:
                    if (lastState == ControllableAppBarLayout.State.COLLAPSED) {
                        animateToolbar(false);
                    }
                    break;
                case IDLE:
                    if (lastState == ControllableAppBarLayout.State.COLLAPSED) {
                        animateToolbar(false);
                    }
                    break;
            }
            lastState = state;
        } else {
            toolbarTitle.setAlpha(1);
        }
    }
    //endregion

    //region Opening posts and web links
    @Override
    public void onBackPressed() {
        if (!closeFullPost()) {
            super.onBackPressed();
        }
    }

    public void openLink(Uri link, Runnable onFinish) {
        List<String> pathList = link.getPathSegments();
        if (pathList.size() > 0) {
            startLoading();
            String host = link.getHost();
            String slug = pathList.get(pathList.size() - 1);
            PostListCoordinator.ViewModelGetPostCallback callback = new PostListCoordinator.ViewModelGetPostCallback() {
                @Override
                public void onFailure() {
                    if (onFinish != null) onFinish.run();
                    stopLoading();
                    openWebURL(link.toString());
                }

                @Override
                public void onSuccess(PostModel postModel) {
                    if (onFinish != null) onFinish.run();
                    stopLoading();
                    openFullPost(postModel, false);
                }
            };

            if (host.contains(Network.REVBEL_URL)) {
                coordinator.getArticleBySlug(slug, callback);
            } else if (host.contains(Network.BANDALUKI_URL)) {
                coordinator.getBanditBySlug(slug, callback);
            }
        } else {
            if (onFinish != null) onFinish.run();
        }
    }

    private void showDetailedFragment(Fragment fragment, String title, String key, @Nullable PostModel post, boolean clear) {
        FragmentManager fm = getSupportFragmentManager();
        if (clear && mTwoPane && keyStack.size() > 0) {
            currentPosts.clear();
            while (keyStack.size() > 0) {
                keyStack.pop();
                fm.popBackStackImmediate();
            }
        }
        hideKeyboard();

        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(STACK_NAME);
        int containerId = mTwoPane ? R.id.post_detail_container : R.id.container;
        ft.replace(containerId, fragment, FullPostFragment.FRAGMENT_TAG)
                .commit();

        keyStack.push(key);
        if (post != null) currentPosts.put(key, post);

        coordinator.currentListViewModel().hasOpenedDetails.set(true);

        if (mTwoPane) {
            detailContainer.setVisibility(View.VISIBLE);
            closeButton.show();
        } else{
            actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
            animateBackButton(true, null);
            if (post != null && post.featuredMediaUrl != null) toolbarTitle.setAlpha(0);
        }

        toolbarTitle.setText(post != null ? post.getTitleSpanned() : title);
        setCurrentOrientation();
        invalidateOptionsMenu();
    }

    private void downloadFileInPost(Uri uri) {
        filesListener = new FilesListener();
        filesListener.checkReadWriteDownloadFolderPermissions(this, () -> downloadFileDialog(uri.getLastPathSegment(), () -> {
            downloadFileDialog = new ProgressDialog(this);

            downloadFileDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            downloadFileDialog.setCancelable(true);
            downloadFileDialog.setOnCancelListener(dialogInterface -> {
                Toast.makeText(this, downloadProgressCancel, Toast.LENGTH_LONG).show();

                downloadFileDialog.dismiss();
                downloadFileDialog = null;
            });

            downloadFileDialog.setTitle(downloadProgressTitle);
            downloadFileDialog.setMessage(downloadProgressMessage + uri.toString());
            downloadFileDialog.show();

            coordinator.downloadFile(uri, (bytesRead, contentLength, done) -> {
                int procents = Math.round((float) bytesRead / contentLength * 100);
                if (downloadFileDialog != null) downloadFileDialog.setProgress(procents);
            }, () -> {
                if (downloadFileDialog != null) {
                    openFile(uri.getLastPathSegment());

                    downloadFileDialog.dismiss();
                    downloadFileDialog = null;
                } else {
                    Toast.makeText(this, downloadProgressSuccess, Toast.LENGTH_LONG).show();
                }
            }, () -> {
                Toast.makeText(this, downloadProgressError, Toast.LENGTH_LONG).show();

                if (downloadFileDialog != null) {
                    downloadFileDialog.dismiss();
                    downloadFileDialog = null;
                }
            });
        }), null);
    }

    public void openWebURL(String webURL) {
        Uri uri = Uri.parse(webURL);
        String host = uri.getHost();
        boolean isRevbel = host.contains(REVBEL_URL);
        if (isRevbel) {
            if (coordinator.canBeDownloaded(uri)) {
                downloadFileInPost(uri);
                return;
            }
        }
        if (!isRevbel) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(Intent.createChooser(browserIntent, chooseMessage));
        } else {
            if (!mTwoPane) {
                featuredImage.setVisibility(View.GONE);
            }
            Bundle arguments = new Bundle();
            arguments.putString(WEB_VIEW_URL, webURL);
            WebViewFragment webViewFragment = new WebViewFragment();
            webViewFragment.setArguments(arguments);
            showDetailedFragment(webViewFragment, webURL, webURL, null, false);
        }
    }

    public void openFullPost(PostModel post, boolean clear) {
        if (!mTwoPane) {
            if (post.featuredMediaUrl != null) {
                featuredImage.setVisibility(View.VISIBLE);
                Glide
                        .with(this)
                        .load(post.featuredMediaUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(featuredImage);
            } else {
                featuredImage.setVisibility(View.GONE);
            }
        }

        Bundle arguments = new Bundle();
        arguments.putInt(FullPostFragment.POST_MODEL_ID, post.getId());
        arguments.putString(FullPostFragment.POST_MODEL_TYPE, post.getType());
        FullPostFragment detailedFragment = new FullPostFragment();
        detailedFragment.setArguments(arguments);
        showDetailedFragment(detailedFragment, post.title, post.fullPostLink, post, clear);
    }

    public boolean closeFullPost() {
        if (endVideoCallback != null) {
            hideMediaFullscreen();
            return true;
        } else if (keyStack.size() > 0) {
            String key = keyStack.pop();
            if (currentPosts.containsKey(key)) currentPosts.remove(key);

            hideKeyboard();
            getSupportFragmentManager().popBackStack();
            if (keyStack.size() == 0) {
                setCurrentTitle();

                closeButton.setVisibility(View.GONE);
                coordinator.currentListViewModel().hasOpenedDetails.set(false);
                if (mTwoPane) {
                    if (detailContainer.getVisibility() != View.GONE) {
                        detailContainer.setVisibility(View.GONE);
                    }
                } else {
                    featuredImage.setVisibility(View.GONE);
                    updateToolbarTitle(appBarLayout.getState());

                    animateBackButton(false, () -> actionBarDrawerToggle.setDrawerIndicatorEnabled(true));
                }
            } else {
                String last = keyStack.lastElement();

                if (currentPosts.containsKey(last)) {
                    PostModel post = currentPosts.get(last);
                    toolbarTitle.setText(post.getTitleSpanned());

                    if (!mTwoPane) {
                        if (featuredImage.getVisibility() == View.GONE) {
                            toolbarTitle.setAlpha(0);
                            featuredImage.setVisibility(View.VISIBLE);
                        }
                        if (post.featuredMediaUrl != null) {
                            Glide
                                    .with(this)
                                    .load(post.featuredMediaUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(featuredImage);
                        } else {
                            featuredImage.setVisibility(View.GONE);
                        }
                    }
                } else {
                    toolbarTitle.setText(last);
                    featuredImage.setVisibility(View.GONE);
                    updateToolbarTitle(appBarLayout.getState());
                }
            }
            setCurrentOrientation();
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }
    //endregion

    //region Keyboard logic
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        if (view != null && imm.isActive()) {
            view.clearFocus();
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    //endregion

    //region Fullscreen video
    public void showMediaFullscreen(View view, WebChromeClient.CustomViewCallback callback, boolean lanscape, boolean cancelOnTap) {
        if (lanscape) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        videoFullscreenDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        videoFullscreenDialog.setContentView(view);
        videoFullscreenDialog.setOnDismissListener(dialog1 -> hideMediaFullscreen());
        if (cancelOnTap) {
            view.setOnClickListener(view1 -> videoFullscreenDialog.dismiss());
        }
        videoFullscreenDialog.show();

        endVideoCallback = callback;
    }

    public void hideMediaFullscreen() {
        if (videoFullscreenDialog != null) {
            videoFullscreenDialog.dismiss();
            videoFullscreenDialog = null;

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            if (endVideoCallback != null) {
                endVideoCallback.onCustomViewHidden();
                endVideoCallback = null;
            }
        }
    }
    //endregion

    //region Shared controls logic
    private int getPriorityForFragment(Fragment fragment) {
        if (fragment instanceof WebViewFragment) {
            return 1;
        } else  if (fragment instanceof FullPostFragment) {
            return 1;
        } if (fragment instanceof PostListFragment) {
            return 2;
        } else if (fragment instanceof FileListFragment) {
            return 2;
        } else if (fragment instanceof FavoriteListFragment) {
            return 2;
        } else if (fragment instanceof DonateFragment) {
            return 2;
        } else if (fragment instanceof AboutUsFragment) {
            return 2;
        }
        return 3;
    }

    public void updateControlButtonTapEvent(Fragment fragment, Runnable event) {
        int priority = getPriorityForFragment(fragment);
        FragmentEventWrapper wrapper = new FragmentEventWrapper(fragment, event);
        controlledFragments.put(priority, wrapper);
    }

    public void onFragmentHide(Fragment fragment) {
        for (Map.Entry<Integer, FragmentEventWrapper> entry : controlledFragments.entrySet()) {
            if (entry.getValue().fragment == fragment) {
                controlledFragments.remove(entry.getKey());
                break;
            }
        }
        stopLoading();
    }

    public void showControlButton(Fragment fragment) {
        if (controlledFragments.firstEntry() != null || controlledFragments.firstEntry().getValue().fragment == fragment && controlButton != null && controlButton.getVisibility() == View.GONE) {
            controlButton.show();
        }
    }

    public void hideControlButton(Fragment fragment) {
        if (controlledFragments.firstEntry() != null || controlledFragments.firstEntry().getValue().fragment == fragment ) {
            if (controlButton.getVisibility() == View.VISIBLE) {
                controlButton.hide();
            }
        }
    }

    public void alertControlButton(Fragment fragment) {
        if (controlledFragments.firstEntry() != null || controlledFragments.firstEntry().getValue().fragment == fragment) {

        }
    }

    public void startLoading() {
        if (loadingIndicator.getVisibility() != View.VISIBLE) {
            loadingIndicator.smoothToShow();
        }
    }

    public void stopLoading() {
        if (loadingIndicator.getVisibility() == View.VISIBLE) {
            loadingIndicator.smoothToHide();
        }
    }
    //endregion

    //region Crashlytics usage dialog
    private void checkAndOpenCrashlyticsDialog(Runnable onFinish) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hasCrashlyticsSettings = sharedPref.contains(USE_CRASHLYTICS);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (!hasCrashlyticsSettings) {
            ViewUtilities.showAlertDialog(this, crashlyticsTitle, crashlyticsMessage, R.drawable.ic_crashlytics, () -> {
                editor.putBoolean(USE_CRASHLYTICS, true);
                editor.apply();
                if (onFinish != null) onFinish.run();
            }, () -> {
                editor.putBoolean(USE_CRASHLYTICS, false);
                editor.apply();
                if (onFinish != null) onFinish.run();
            });
        } else {
            if (onFinish != null) onFinish.run();
        }
    }
    //endregion

    //region Orbot dialog
    private void checkAndOpenNetworkDialog(Runnable onFinish) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hasTorSettings = sharedPref.contains(NETWORK_USE_TOR_KEY);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (!hasTorSettings) {
            TorDialogFragment dialog = new TorDialogFragment();
            dialog.hasTor = orbotHelper.isInstalled();
            boolean hasBitmask = Utilities.hasAppInstalled(this, Network.BITMASK_PACKAGE_NAME);
            dialog.hasBitmask = hasBitmask;
            dialog.callback = new TorDialogFragment.TorDialogFragmentCallback() {
                @Override
                public void onUsingTor() {
                    editor.putBoolean(NETWORK_USE_TOR_KEY, true);
                    editor.putBoolean(NETWORK_USE_VPN_KEY, false);
                    editor.putBoolean(NETWORK_USE_MIRROR_KEY, false);
                    editor.apply();
                    if (!orbotHelper.isInstalled()) {
                        orbotHelper.installOrbot(MainActivity.this);
                    }
                    if (onFinish != null) onFinish.run();
                }

                @Override
                public void onUsingBitmask() {
                    editor.putBoolean(NETWORK_USE_TOR_KEY, false);
                    editor.putBoolean(NETWORK_USE_VPN_KEY, true);
                    editor.putBoolean(NETWORK_USE_MIRROR_KEY, false);
                    editor.apply();
                    if (!hasBitmask) {
                        Utilities.openGooglePlay(MainActivity.this, Network.BITMASK_PACKAGE_NAME);
                    }
                    if (onFinish != null) onFinish.run();
                }

                @Override
                public void onUsingWeb() {
                    editor.putBoolean(NETWORK_USE_TOR_KEY, false);
                    editor.putBoolean(NETWORK_USE_VPN_KEY, false);
                    editor.putBoolean(NETWORK_USE_MIRROR_KEY, true);
                    editor.apply();
                    if (onFinish != null) onFinish.run();
                }
            };

            dialog.show(getSupportFragmentManager(), "Tor dialog");
        } else if (coordinator.isUsingTor() && !orbotHelper.isInstalled()) {
            orbotHelper.installOrbot(MainActivity.this);
            if (onFinish != null) onFinish.run();
        } else {
            if (onFinish != null) onFinish.run();
        }
    }
    //endregion

    //region Library and book activities
    public void downloadFileDialog(String fileName, Runnable onSuccess) {
        ViewUtilities.showAlertDialog(this, downloadTitle, downloadMessage + "\"" + fileName + "\"?", R.drawable.download_book, () -> {
            if (onSuccess != null) onSuccess.run();
        }, () -> {
            Toast.makeText(this, downloadCancelMessage, Toast.LENGTH_LONG).show();
        });
    }

    public void openFile(String localURL) {
        File targetFile = new File(PostListCoordinator.getFileFromEnvironment(), localURL);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        String fileExt =  FileViewModel.fileExt(localURL);

        if (fileExt != null) {
            String mimeType = myMime.getMimeTypeFromExtension(fileExt.substring(1));

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", targetFile);
                intent.setDataAndType(fileUri, mimeType);

                openBookDialog(intent, fileUri, targetFile);
            } else {
                Uri fileUri = Uri.fromFile(targetFile);
                intent.setDataAndType(fileUri, mimeType);

                openBookDialog(intent, fileUri, targetFile);
            }
        }
    }

    private void openBookDialog(Intent intent, Uri fileUri, File targetFile) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hasBookSettings = sharedPref.contains(OPEN_INTERNAL_PDF);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (!hasBookSettings) {
            OpenBookDialogFragment dialogFragment = new OpenBookDialogFragment();
            dialogFragment.callback = new OpenBookDialogFragment.OpenBookDialogFragmentCallback() {
                @Override
                public void onUsingExternalActivity(boolean save) {
                    openBookExternal(intent, fileUri, targetFile);
                    if (save) {
                        editor.putBoolean(OPEN_INTERNAL_PDF, false);
                        editor.apply();
                    }
                }
                @Override
                public void onUsingInternalActivity(boolean save) {
                    openBookInternal(targetFile);
                    if (save) {
                        editor.putBoolean(OPEN_INTERNAL_PDF, true);
                        editor.apply();
                    }
                }
            };
            dialogFragment.show(getSupportFragmentManager(), "Open book dialog");
        } else  {
            if (sharedPref.getBoolean(OPEN_INTERNAL_PDF, false)) {
                openBookInternal(targetFile);
            } else {
                openBookExternal(intent, fileUri, targetFile);
            }
        }
    }

    private void openBookExternal(Intent intent, Uri fileUri, File targetFile) {
        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        openActivityForFile(intent, targetFile);
    }

    private void openActivityForFile(Intent intent, File targetFile) {
        Runnable internal = () -> {
            openBookInternal(targetFile);
            Toast.makeText(this, noExternalPDFMessage, Toast.LENGTH_LONG).show();
        };
        try {
            ResolveInfo info = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null) {
                startActivity(Intent.createChooser(intent, chooseMessage));
            } else {
                internal.run();
            }
        } catch (ActivityNotFoundException e) {
            internal.run();
        }
    }

    private void openBookInternal(File targetFile) {
        Intent intent = new Intent(this, DocumentActivity.class);
        intent.setData(Uri.fromFile(targetFile));
        startActivity(intent);
    }
    //endregion
}
