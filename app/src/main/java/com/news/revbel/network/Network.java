package com.news.revbel.network;


import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.ObservableField;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.utilities.Attachment;
import com.news.revbel.utilities.MailService;
import com.news.revbel.utilities.ProxyUtilities;
import com.news.revbel.utilities.Utilities;
import com.news.revbel.viewmodel.FileViewModel;
import com.novoda.merlin.Merlin;
import com.novoda.merlin.MerlinsBeard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.webkit.WebkitProxy;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

import static com.news.revbel.coordinator.SettingsCoordinator.NETWORK_USE_MIRROR_KEY;
import static com.news.revbel.coordinator.SettingsCoordinator.NETWORK_USE_TOR_KEY;
import static com.news.revbel.coordinator.SettingsCoordinator.NETWORK_USE_VPN_KEY;

@Singleton
public class Network implements SharedPreferences.OnSharedPreferenceChangeListener {
    public enum TorSettings {
        TOR_SOCKS, TOR_HTTP, NOT_TOR, UNDEFINED;
    }

    public enum NetUsage {
        USE_TOR, USE_VPN, USE_WEB, UNDEFINED;

        public static NetUsage getUsage(int netUsage) {
            switch (netUsage) {
                case 0: return USE_TOR;
                case 1: return USE_VPN;
                case 2: return USE_WEB;
            }
            return UNDEFINED;
        }

        public boolean isUsingTor() {
            return this == USE_TOR;
        }

        public boolean isUsingVPN() {
            return this == USE_VPN;
        }
    }

    public enum SERVER {
        REVBEL, BANDALUKI
    }

    public static String BITMASK_PACKAGE_NAME = "se.leap.bitmaskclient";

    static ObservableField<TorSettings> usingTor = new ObservableField<>(TorSettings.UNDEFINED);

    public NetUsage useTor = NetUsage.UNDEFINED;

    private OkHttpClient httpClient;
    private Context context;
    private OrbotHelper torHelper;
    private boolean orbotInitialized = false;

    private Merlin merlin;
    private MerlinsBeard merlinsBeard;

    private ArrayList<Runnable> runnables = new ArrayList<>();

    private ArrayList<OkHttpClient> downloads = new ArrayList<>();
    private ArrayList<Runnable> fileDownloadList = new ArrayList<>();

    public final static String REVBEL_URL = "revbel.";
    public final static String BANDALUKI_URL = "bandaluki.";
    public final static String OLD_SERVER_HOST = "revbel.org";
    public final static String NEW_SERVER_HOST = "revbel.cc";
    private JSONNetworkParser parser;

    public interface NetworkErrorCallback {
        void onFailure(Exception e);

        void onPostResponse(JSONArray items);
    }

    public interface NetworkCallback {
        void onFailure();

        void onPostResponse(JSONArray items);
    }

    public interface NetworkFileCallback {
        void onFailure();

        void onPostResponse(File file);
    }

    //region Server urls
    public static String revbelUrl = OLD_SERVER_HOST;

    private String bandalukiLink() {
        return "bandaluki.info";
    }

    private String revbelLink() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean usingMirror = sharedPref.getBoolean(NETWORK_USE_MIRROR_KEY, false);
        return (usingMirror ? OLD_SERVER_HOST : NEW_SERVER_HOST);
    }

    private String bandaLukiServerUrlForPosts() {
        return "https://"+ bandalukiLink() +"/wp-json/wp/v2/bandits?context=embed";
    }

    private String bandalukiUrlForComments() {
        return "https://" + bandalukiLink() + "/wp-json/wp/v2/comments";
    }

    private String bandalukiUrlForSlug() {
        return "https://" + bandalukiLink() + "/wp-json/wp/v2/bandits?filter[name]=";
    }

    private String serverUrlForPosts() {
        return "https://" + revbelLink() + "/wp-json/wp/v2/posts?context=embed";
    }

    private String serverUrlForPages() {
        return "https://" + revbelLink() + "/wp-json/wp/v2/pages/";
    }

    private String serverUrlForCategories() {
        return "https://" + revbelLink() + "/wp-json/wp/v2/categories";
    }

    private String serverUrlForComments() {
        return "https://" + revbelLink() + "/wp-json/wp/v2/comments";
    }

    private String serverUrlForSlug() {
        return "https://" + revbelLink() + "/wp-json/wp/v2/posts?filter[name]=";
    }
    //endregion

    Network(Context context, OrbotHelper helper) {
        this.context = context;

        torHelper = helper;

        merlin = new Merlin.Builder().withBindableCallbacks().build(context);
        merlinsBeard = MerlinsBeard.from(context);

        merlin.registerBindable(networkStatus -> updateState());
        merlin.bind();

        Network.revbelUrl = revbelLink();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        boolean hasTorSettings = sharedPref.contains(NETWORK_USE_TOR_KEY);

        if (hasTorSettings) {
            boolean usingTor = sharedPref.getBoolean(NETWORK_USE_TOR_KEY, true);
            boolean usingVPN = sharedPref.getBoolean(NETWORK_USE_VPN_KEY, true);
            int result = usingTor ? 0 : usingVPN ? 1 : 2;
            setNetUsage(NetUsage.getUsage(result));
        } else {
            if (!orbotInitialized) torHelper.init();
        }
    }

    //region Preferences callback
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(NETWORK_USE_TOR_KEY) || key.equals(NETWORK_USE_VPN_KEY)) {
            boolean usingTor = sharedPreferences.getBoolean(NETWORK_USE_TOR_KEY, true);
            boolean usingVPN = sharedPreferences.getBoolean(NETWORK_USE_VPN_KEY, true);
            int result = usingTor ? 0 : usingVPN ? 1 : 2;
            setNetUsage(NetUsage.getUsage(result));
        } else if (key.equals(NETWORK_USE_MIRROR_KEY)) {
            Network.revbelUrl = revbelLink();
        }
    }
    //endregion

    //region Toggle connections logics
    private void setNetworkClient(OkHttpClient client) {
        httpClient = client;
    }

    private void setNetUsage(NetUsage useTor) {
        if (this.useTor != useTor) {
            this.useTor = useTor;

            Network.revbelUrl = revbelLink();

            parser = null;
            setNetworkClient(null);

            if (useTor.isUsingTor()) {
                registerTorCallback();
            } else {
                Network.usingTor.set(TorSettings.NOT_TOR);

                Runnable block = () -> {
                    createOkHttpDirect();
                    updateState();
                };
                if (!useTor.isUsingVPN()) {
                    block.run();
                } else {
                    if (isRunningVPN()) {
                        block.run();
                    } else {
                        waitForVPNRunning(block);
                    }
                }

                try {
                    WebkitProxy.resetProxy("com.news.revbel.RevApplication", context);
                } catch (Exception e) {
                    Log.d("Tor", "Cannot reset proxy for WebView.");
                }
            }
        }
    }
    //endregion

    //region Direct connection
    private void createOkHttpDirect() {
        setNetworkClient(new OkHttpClient());
        parser = new JSONNetworkParser(httpClient);
    }
    //endregion

    //region Tor connection
    private void checkTor(JSONNetworkParser parser, Runnable isTor, Runnable isNotTor) {
        parser.getJSONArrayFromUrl("https://check.torproject.org/api/ip", new JSONNetworkParser.JSONCallback() {
            @Override
            public void onFailure() {
                RevApplication.runOnUI(isNotTor);
            }

            @Override
            public void onTimeout() {
                RevApplication.runOnUI(isNotTor);
            }

            @Override
            public void onResponse(Object jsonResponse) {
                JSONObject torResopnse = (JSONObject) jsonResponse;
                try {
                    boolean tor = torResopnse.getBoolean("IsTor");
                    if (tor) {
                        RevApplication.runOnUI(isTor);
                    } else {
                        RevApplication.runOnUI(isNotTor);
                    }
                } catch (Exception e) {
                    RevApplication.runOnUI(isNotTor);
                }

            }
        });
    }

    private void createOkHttpWithTor() {
        if (parser == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder().socketFactory(ProxyUtilities.torSocksFactory());
            setNetworkClient(builder.build());
            JSONNetworkParser socksParser = new JSONNetworkParser(httpClient);
            checkTor(socksParser, () -> {
                Network.usingTor.set(TorSettings.TOR_SOCKS);
                parser = socksParser;

                try {
                    WebkitProxy.setProxy("com.news.revbel.RevApplication", context, null, "127.0.0.1", 8118);
                } catch (Exception e) {
                    Log.d("Tor", "Cannot set proxy for WebView.");
                }

                updateState();
            }, () -> {
                OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8118)));
                setNetworkClient(httpBuilder.build());
                JSONNetworkParser httpParser = new JSONNetworkParser(httpClient);
                checkTor(httpParser, () -> {
                    Network.usingTor.set(TorSettings.TOR_HTTP);
                    parser = httpParser;

                    try {
                        WebkitProxy.setProxy("com.news.revbel.RevApplication", context, null, "127.0.0.1", 8118);
                    } catch (Exception e) {
                        Log.d("Tor", "Cannot set proxy for WebView.");
                    }

                    updateState();
                }, this::createOkHttpWithTor);
            });
        }
    }

    private void registerTorCallback() {
        if (!orbotInitialized) torHelper.init();

        if (useTor == NetUsage.USE_TOR) {
            createOkHttpWithTor();
            torHelper.requestStart(context);
        }
    }
    //endregion

    //region VPN connection
    private void waitForVPNRunning(Runnable callback) {
        AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                while (!isRunningVPN()) {
                    try {
                        Thread.currentThread();
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    callback.run();
                }
            }
        };
        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean isRunningVPN() {
        List<String> networkList = new ArrayList<>();
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isUp())
                    networkList.add(networkInterface.getName());
            }
        } catch (Exception ex) {
            return false;
        }
        for (String network : networkList) {
            if (network.contains("tun")) return true;
        }
        return false;
    }
    //endregion

    //region Network helper methods
    public boolean getInternetAvailability() {
        return merlinsBeard.isConnected() && parser != null;
    }

    public void scheduleNetworkOperation(Runnable runnable) {
        runnables.add(runnable);
    }

    private void updateState() {
        if (getInternetAvailability()) {
            for (Runnable runnable : runnables) {
                runnable.run();
            }
            runnables.clear();
        }
    }
    //endregion

    //region Category requests
    public void getCategoriesForParent(final String parentId, final NetworkCallback callback){
        parser.getJSONArrayFromUrl(serverUrlForCategories() + "?parent=" + parentId, new JSONNetworkParser.JSONCallback() {
            @Override
            public void onFailure() {
                callback.onFailure();
            }

            @Override
            public void onResponse(final Object jsonResponse) {
                JSONArray categoriesArray = (JSONArray) jsonResponse;
                callback.onPostResponse(categoriesArray);
            }

            @Override
            public void onTimeout() {
                getCategoriesForParent(parentId, callback);
            }
        });
    }
    //endregion

    //region Banda Luki API
    public void getBandits(int page, String label, String withArgs, final NetworkCallback callback) {
        String url = bandaLukiServerUrlForPosts();
        if (withArgs != null) url += "&" + withArgs;
        url += "&page=" + page + "&_embed";
        parser.getJSONArrayFromUrl(url, label, new JSONNetworkParser.JSONCallback() {
            @Override
            public void onFailure() {
                callback.onFailure();
            }

            @Override
            public void onTimeout() {
                getBandits(page, label, withArgs, callback);
            }

            @Override
            public void onResponse(Object jsonResponse) {
                JSONArray response = (JSONArray) jsonResponse;
                callback.onPostResponse(response);
            }
        });
    }
    //endregion

    //region Posts requests
    public void getPosts(int page, String label, String withArgs, final NetworkCallback callback)
    {
        String url = serverUrlForPosts() + "&" + withArgs + "&page=" + page+"&_embed";
        parser.getJSONArrayFromUrl(url, label, new JSONNetworkParser.JSONCallback() {
            @Override
            public void onFailure() {
                callback.onFailure();
            }

            @Override
            public void onResponse(final Object jsonResponse)  {
                JSONArray response = (JSONArray) jsonResponse;
                callback.onPostResponse(response);
            }

            @Override
            public void onTimeout() {
                getPosts(page, label, withArgs, callback);
            }
        });
    }

    public void getPostFullText(final String fullPostLink, final NetworkCallback callback) {
        if (fullPostLink != null) {
            parser.getJSONArrayFromUrl(fullPostLink+"?_embed", new JSONNetworkParser.JSONCallback() {
                @Override
                public void onFailure() {
                    callback.onFailure();
                }

                @Override
                public void onResponse(Object jsonResponse)  {
                    JSONObject response = (JSONObject) jsonResponse;
                    callback.onPostResponse(Utilities.covertJsonObjectToJsonArray(response));
                }

                @Override
                public void onTimeout() {
                    getPostFullText(fullPostLink, callback);
                }
            });
        }
    }

    public void getPostBySlug(SERVER server, String slug, final NetworkCallback callback) {
        String link = (server == SERVER.BANDALUKI ? bandalukiUrlForSlug() : serverUrlForSlug()) + slug + "&_embed";
        parser.getJSONArrayFromUrl(link, new JSONNetworkParser.JSONCallback() {
            @Override
            public void onFailure() {
                callback.onFailure();
            }

            @Override
            public void onResponse(Object jsonResponse)  {
                JSONArray response = (JSONArray) jsonResponse;
                callback.onPostResponse(response);
            }

            @Override
            public void onTimeout() {
                getPostBySlug(server, slug, callback);
            }
        });
    }
    //endregion

    //region Reply requests
    public void getRepliesForPost(final int postId, String date, final NetworkCallback callback) {
        String getReply = serverUrlForComments() + "?post=" + postId + "&before=" + date;
        parser.getJSONArrayFromUrl(getReply, new JSONNetworkParser.JSONCallback() {
            @Override
            public void onFailure() {
                callback.onFailure();
            }

            @Override
            public void onResponse(Object jsonResponse) {
                JSONArray repliesJSON = (JSONArray) jsonResponse;
                callback.onPostResponse(repliesJSON);
            }

            @Override
            public void onTimeout() {
                getRepliesForPost(postId, date, callback);
            }
        });
    }

    public void postReplyForParent(SERVER server, int postId, int parentId, String authorName, String authorEmail, String textContent, final NetworkErrorCallback callback) {
        RequestBody body = new FormBody.Builder().add("post", Integer.toString(postId))
                .add("author_name", authorName)
                .add("author_email", authorEmail)
                .add("parent", Integer.toString(parentId))
                .add("content", textContent).build();
        parser.postJSONToURL(server == SERVER.REVBEL ? serverUrlForComments() : bandalukiUrlForComments(), body, new JSONNetworkParser.JSONErrorCallback() {
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Object jsonResponse) {
                JSONObject reply = (JSONObject) jsonResponse;
                callback.onPostResponse(Utilities.covertJsonObjectToJsonArray(reply));
            }
        });
    }
    //endregion

    //region File lists requests
    public void getFilesFromPage(final String page, final NetworkCallback callback) {
        parser.getJSONArrayFromUrl(serverUrlForPages() + page, new JSONNetworkParser.JSONCallback() {
            @Override
            public void onFailure() {
                callback.onFailure();
            }

            @Override
            public void onResponse(final Object jsonResponse) {
                JSONObject response = (JSONObject) jsonResponse;
                callback.onPostResponse(Utilities.covertJsonObjectToJsonArray(response));
            }

            @Override
            public void onTimeout() {
                getFilesFromPage(page, callback);
            }
        });
    }

    public void downloadFileFromUrl(String url, File root, final String path, final Progress.ProgressListener progressListener, NetworkFileCallback callback) {
        Runnable runnable = () -> {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            if (httpClient != null) {
                builder = httpClient.newBuilder();
            } else {
                if (useTor.isUsingTor()) {
                    builder = builder.socketFactory(ProxyUtilities.torSocksFactory());
                }
            }

            final OkHttpClient client = builder
                    .addNetworkInterceptor(chain -> {
                        Response originalResponse = chain.proceed(chain.request());
                        return originalResponse.newBuilder()
                                .body(new Progress.ProgressResponseBody(originalResponse.body(), progressListener))
                                .build();
                    })
                    .build();

            downloads.add(client);

            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    downloads.remove(client);
                    callback.onFailure();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    File downloadedFile = new File(root, path);

                    if (!downloadedFile.exists()) {
                        downloadedFile.getParentFile().mkdirs();
                        if (!downloadedFile.createNewFile()) {
                            callback.onFailure();
                            return;
                        }
                    } else {
                        int i = 0;
                        while (downloadedFile.exists()) {
                            i+=1;

                            String newPath = FileViewModel.insertBeforeExt("-" + i, path);
                            downloadedFile = new File(PostListCoordinator.getFileFromEnvironment(), newPath);
                        }
                    }
                    BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));

                    try {
                        sink.writeAll(response.body().source());

                        callback.onPostResponse(downloadedFile);
                    } catch (IOException e) {
                        callback.onFailure();
                        downloadedFile.delete();
                    } finally {
                        sink.close();

                        downloads.remove(client);
                        if (fileDownloadList.size() > 0) {
                            fileDownloadList.get(fileDownloadList.size() - 1).run();
                            fileDownloadList.remove(fileDownloadList.size() - 1);
                        }
                    }
                }
            });
        };

        if (downloads.size() > 1) {
            fileDownloadList.add(0, runnable);
        } else {
            runnable.run();
        }
    }
    //endregion

    //region Sending with email
    public void sendFeedback(String text, NetworkErrorCallback callback) {
        sendMail("feedback@revbel.org", "Пользователь приложения", "Отзыв", text, null, null, callback);
    }

    public void sendMail(String from, String fromName, String title, String text, String html, List<Attachment> attachment, NetworkErrorCallback callback) {
        MailService service = new MailService(from, fromName, "revolutionaruaction@riseup.net", title, text, html, attachment);
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void[] voids) {
                try {
                    service.sendAuthenticated();
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Exception result) {
                super.onPostExecute(result);
                if (result == null) {
                    callback.onPostResponse(null);
                } else {
                    callback.onFailure(result);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    //endregion
}
