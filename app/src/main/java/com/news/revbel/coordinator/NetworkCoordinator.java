package com.news.revbel.coordinator;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.news.revbel.RevApplication;
import com.news.revbel.database.FileDataModel;
import com.news.revbel.database.TaxonomyDataModel;
import com.news.revbel.database.PostDataModel;
import com.news.revbel.network.LibraryHTMLParser;
import com.news.revbel.network.Network;
import com.news.revbel.network.Progress;
import com.news.revbel.utilities.Attachment;
import com.news.revbel.utilities.Utilities;
import com.news.revbel.viewmodel.ArticlePostModel;
import com.news.revbel.viewmodel.BanditPostModel;
import com.news.revbel.viewmodel.FileViewModel;
import com.news.revbel.viewmodel.TaxonomyModel;
import com.news.revbel.viewmodel.CategoryPostListViewModel;
import com.news.revbel.viewmodel.ListedFilesViewModel;
import com.news.revbel.viewmodel.PostModel;
import com.news.revbel.viewmodel.ReplyModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.helper.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

@Singleton
public class NetworkCoordinator {
    @Inject Context context;
    @Inject Network network;

    public interface NetworkCoordinatorErrorCallback {
        void onFailure(Exception e);
        void onNetworkResponse(List items);
    }

    public interface NetworkCoordinatorCallback {
        void onFailure();
        void onDatabaseResponse(List items);
        void onNetworkResponse(List items);
    }

    public NetworkCoordinator() {
        RevApplication.getComponent().inject(this);
    }

    boolean isUsingTor() { return network.useTor.isUsingTor(); }

    public void postReplyOnArticle(int id, int parentId, String userName, String userEmail, String text, NetworkCoordinatorErrorCallback callback) {
        postReplyOnPost(Network.SERVER.REVBEL, id, parentId, userName, userEmail, text, callback);
    }

    public void postReplyOnBandit(int id, int parentId, String userName, String userEmail, String text, NetworkCoordinatorErrorCallback callback) {
        postReplyOnPost(Network.SERVER.BANDALUKI, id, parentId, userName, userEmail, text, callback);
    }

    private void postReplyOnPost(Network.SERVER server, int id, int parentId, String userName, String userEmail, String text, NetworkCoordinatorErrorCallback callback) {
        if (network.getInternetAvailability()) {
            network.postReplyForParent(server, id, parentId, userName, userEmail, text, new Network.NetworkErrorCallback() {
                @Override
                public void onFailure(Exception e) {
                    RevApplication.runOnUI(() -> callback.onFailure(e));
                }

                @Override
                public void onPostResponse(JSONArray comment) {
                    try {
                        JSONObject reply = comment.getJSONObject(0);
                        String newAuthorName = reply.getString("author_name");
                        int replyId = reply.getInt("id");
                        int newParentId = reply.getInt("parent");
                        String textContent = reply.getJSONObject("content").getString("rendered");
                        Date date = Utilities.stringToDate(reply.getString("date"));
                        final ReplyModel replyModel = new ReplyModel(replyId, id, newParentId, newAuthorName, date, textContent, false);
                        RevApplication.runOnUI(() -> callback.onNetworkResponse(Collections.singletonList(replyModel)));
                    } catch (JSONException e) {
                        RevApplication.runOnUI(() -> callback.onFailure(e));
                    }
                }
            });
        } else {
            callback.onFailure(new IOException("No internet"));
        }
    }

    public void downloadFileFromUrl(String url, File file, String path, Progress.ProgressListener progressListener, NetworkCoordinatorCallback callback) {
        network.downloadFileFromUrl(url, file, path, progressListener, new Network.NetworkFileCallback() {
            @Override
            public void onFailure() {
                RevApplication.runOnUI(callback::onFailure);
            }

            @Override
            public void onPostResponse(File file) {
                RevApplication.runOnUI(() -> callback.onNetworkResponse(Collections.singletonList(file)));
            }
        });
    }

    public void getListedFiles(String pageId, ListedFilesViewModel listedModel, NetworkCoordinatorCallback callback) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<FileDataModel> results = realm
                .where(FileDataModel.class).equalTo("pageOwner", pageId)
                .findAllSorted("id", Sort.ASCENDING);
        ArrayList<FileViewModel> list = new ArrayList<>();
        for (FileDataModel model : results) {
            list.add(new FileViewModel(model, listedModel));
        }
        callback.onDatabaseResponse(list);
        realm.close();

        Network.NetworkCallback callbackNetwork = new Network.NetworkCallback() {
            @Override
            public void onFailure() {
                RevApplication.runOnUI(callback::onFailure);
            }

            @Override
            public void onPostResponse(final JSONArray books) {
                try {
                    JSONObject response = books.getJSONObject(0);
                    final String text = response.getJSONObject("content").getString("rendered");
                    ArrayList<FileViewModel> list = LibraryHTMLParser.parseHTMLLibrary(text, pageId, listedModel);
                    RevApplication.runOnUI(() -> callback.onNetworkResponse(list));
                } catch (JSONException e) {
                    RevApplication.runOnUI(callback::onFailure);
                }
            }
        };
        if (network.getInternetAvailability()) {
            network.getFilesFromPage(pageId, callbackNetwork);
        } else {
            network.scheduleNetworkOperation(() -> network.getFilesFromPage(pageId, callbackNetwork));
        }
    }

    PostModel getPostByIdSync(String id, String type) {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<PostDataModel> resultQuery = realm.where(PostDataModel.class).equalTo("uniqueId", PostModel.getUninqueId(Integer.parseInt(id), type));
        PostDataModel postDataModel = resultQuery.findFirst();
        PostModel postModel = PostModel.createFromDatabase(postDataModel);

        return postModel;
    }

    void getBanditBySlug(String slug, NetworkCoordinatorCallback callback) {
        if (network.getInternetAvailability()) {
            network.getPostBySlug(Network.SERVER.BANDALUKI, slug, new Network.NetworkCallback() {
                @Override
                public void onFailure() {
                    RevApplication.runOnUI(callback::onFailure);
                }

                @Override
                public void onPostResponse(JSONArray items) {
                    try {
                        JSONObject object = items.getJSONObject(0);
                        BanditPostModel postModel = new BanditPostModel(object);
                        RevApplication.runOnUI(() -> callback.onNetworkResponse(Collections.singletonList(postModel)));
                    } catch (JSONException e) {
                        RevApplication.runOnUI(callback::onFailure);
                    }

                }
            });
        } else {
            callback.onFailure();
        }
    }

    void getArticleBySlug(String slug, NetworkCoordinatorCallback callback) {
        if (network.getInternetAvailability()) {
            network.getPostBySlug(Network.SERVER.REVBEL, slug, new Network.NetworkCallback() {
                @Override
                public void onFailure() {
                    RevApplication.runOnUI(callback::onFailure);
                }

                @Override
                public void onPostResponse(JSONArray items) {
                    try {
                        JSONObject object = items.getJSONObject(0);
                        ArticlePostModel postModel = new ArticlePostModel(object);
                        RevApplication.runOnUI(() -> callback.onNetworkResponse(Collections.singletonList(postModel)));
                    } catch (JSONException e) {
                        RevApplication.runOnUI(callback::onFailure);
                    }

                }
            });
        } else {
            callback.onFailure();
        }
    }


    public void getPostFullText(String fullPostLink, NetworkCoordinatorCallback callback) {
        Network.NetworkCallback callbackNetwork = new Network.NetworkCallback() {
            @Override
            public void onFailure() {
                RevApplication.runOnUI(callback::onFailure);
            }

            @Override
            public void onPostResponse(JSONArray item) {
                try {
                    JSONObject fullText = item.getJSONObject(0);
                    PostModel post = null;
                    if (fullPostLink.contains(Network.REVBEL_URL)) {
                        post = new ArticlePostModel(fullText);
                    } else if (fullPostLink.contains(Network.BANDALUKI_URL)) {
                        post = new BanditPostModel(fullText);
                    }

                    if (post != null) {
                        final PostModel finalPost = post;
                        RevApplication.runOnUI(() -> callback.onNetworkResponse(new ArrayList<>(Collections.singletonList(finalPost))));
                    } else {
                        RevApplication.runOnUI(callback::onFailure);
                    }
                } catch (JSONException e) {
                    RevApplication.runOnUI(callback::onFailure);
                }
            }
        };

        if (network.getInternetAvailability()) {
            network.getPostFullText(fullPostLink, callbackNetwork);
        } else {
            network.scheduleNetworkOperation(() -> network.getPostFullText(fullPostLink, callbackNetwork));
        }
    }

    private ArrayList<PostModel> getPostModelsFromDatabase(int page, String categoryId, String author, String type, boolean loadAll) {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<PostDataModel> resultQuery = realm.where(PostDataModel.class);

        resultQuery = categoryId != null && !categoryId.equals("0") ? resultQuery.equalTo("categories.uniqueId", categoryId) : resultQuery;
        if (author != null) resultQuery = resultQuery.equalTo("author", author);
        if (type != null) resultQuery = resultQuery.equalTo("type", type);

        RealmResults<PostDataModel> results = resultQuery.findAllSorted("createdAt", Sort.DESCENDING);

        List<PostDataModel> postDataModelList;

        if (loadAll) {
            postDataModelList = results;
        } else {
            postDataModelList = results.subList(Math.min((page - 1) * 10, results.size()), Math.min(page * 10, results.size()));
        }

        ArrayList<PostModel> postModels = new ArrayList<>();

        for (PostDataModel postDataModel : postDataModelList) {
            PostModel postModel = PostModel.createFromDatabase(postDataModel);
            postModels.add(postModel);
        }

        realm.close();

        return postModels;
    }

    public List<PostModel> getBanditsFromDatabase() {
        return getPostModelsFromDatabase(0, null, null, BanditPostModel.type(), true);
    }

    public void getBanditsForPage(int page, String requestLabel, NetworkCoordinatorCallback callback) {
        callback.onDatabaseResponse(getPostModelsFromDatabase(page, null, null, BanditPostModel.type(), false));
        if (network.getInternetAvailability()) {
            network.getBandits(page, requestLabel, null, new Network.NetworkCallback() {
                @Override
                public void onFailure() {
                    RevApplication.runOnUI(callback::onFailure);
                }

                @Override
                public void onPostResponse(JSONArray posts) {
                    try {
                        final ArrayList<PostModel> postArray = new ArrayList<>();

                        for (int i = 0; i < posts.length(); ++i) {
                            JSONObject rec = posts.getJSONObject(i);

                            BanditPostModel post = new BanditPostModel(rec);

                            postArray.add(post);
                        }
                        RevApplication.runOnUI(() -> callback.onNetworkResponse(postArray));
                    } catch (JSONException e) {
                        RevApplication.runOnUI(callback::onFailure);
                    }
                }
            });
        } else {
            callback.onFailure();
        }
    }

    public void getPostsForPage(int page, String requestLabel, String categoryId, CategoryPostListViewModel.PostListType postListType, NetworkCoordinatorCallback callback)
    {
        callback.onDatabaseResponse(getPostModelsFromDatabase(page, categoryId, null, ArticlePostModel.type(), false));
        if (network.getInternetAvailability()) {
            String args = postListType.networkParams();
            if (categoryId != null && !categoryId.equals("0")) {
                args += "&filter[cat]=" + categoryId;
            }
            network.getPosts(page, requestLabel, args, new Network.NetworkCallback() {
                @Override
                public void onFailure() {
                    RevApplication.runOnUI(callback::onFailure);
                }

                @Override
                public void onPostResponse(JSONArray posts) {
                    try {
                        final ArrayList<PostModel> postArray = new ArrayList<>();

                        for (int i = 0; i < posts.length(); ++i) {
                            JSONObject rec = posts.getJSONObject(i);

                            ArticlePostModel post = new ArticlePostModel(rec);

                            postArray.add(post);
                        }
                        RevApplication.runOnUI(() -> callback.onNetworkResponse(postArray));
                    } catch (JSONException e) {
                        RevApplication.runOnUI(callback::onFailure);
                    }
                }
            });
        } else {
            callback.onFailure();
        }
    }

    public void getRepliesForPost(int postId, String date, NetworkCoordinatorCallback callback) {
        if (network.getInternetAvailability()) {
            network.getRepliesForPost(postId, date, new Network.NetworkCallback() {
                @Override
                public void onFailure() {
                    RevApplication.runOnUI(callback::onFailure);
                }

                @Override
                public void onPostResponse(JSONArray items) {
                    try {
                        final ArrayList<ReplyModel> repliesArray = new ArrayList<>();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject reply = items.getJSONObject(i);

                            String authorName = reply.getString("author_name");
                            int replyId = reply.getInt("id");
                            int parentId = reply.getInt("parent");
                            String textContent = reply.getJSONObject("content").getString("rendered");
                            Date date = Utilities.stringToDate(reply.getString("date"));
                            ReplyModel replyModel = new ReplyModel(replyId, postId, parentId, authorName, date, textContent, false);
                            repliesArray.add(replyModel);
                        }

                        RevApplication.runOnUI(() -> callback.onNetworkResponse(repliesArray));
                    } catch (JSONException e) {
                        RevApplication.runOnUI(callback::onFailure);
                    }
                }
            });
        }
    }

    public void getCategoriesForParent(String categoryId, String type, List<String> excludeCategories, NetworkCoordinatorCallback callback) {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<TaxonomyDataModel> resultQuery = realm.where(TaxonomyDataModel.class).equalTo("type", type).equalTo("id", categoryId);
        TaxonomyDataModel result = resultQuery.findFirst();
        if (result != null && result.taxonomyCategories != null) {
            callback.onDatabaseResponse(result.taxonomyCategories);
        }
        realm.close();

        String urlRequest = categoryId + "&exclude=" + StringUtil.join(excludeCategories, ",");
        Network.NetworkCallback callbackNetwork = new Network.NetworkCallback() {
            @Override
            public void onFailure() {
                RevApplication.runOnUI(callback::onFailure);
            }

            @Override
            public void onPostResponse(final JSONArray item) {
                try {
                    final ArrayList<TaxonomyModel> arrayCategories = new ArrayList<>();
                    for (int i = 0; i < (item != null ? item.length() : 0); ++i) {
                        JSONObject category = item.getJSONObject(i);
                        arrayCategories.add(new TaxonomyModel(category));
                    }

                    RevApplication.runOnUI(() -> callback.onNetworkResponse(arrayCategories));
                } catch (JSONException e) {
                    RevApplication.runOnUI(callback::onFailure);
                }
            }
        };

        if (network.getInternetAvailability()) {
            network.getCategoriesForParent(urlRequest, callbackNetwork);
        } else {
            network.scheduleNetworkOperation(() -> network.getCategoriesForParent(urlRequest, callbackNetwork));
        }
    }

    public void listFavoritePosts(NetworkCoordinatorCallback callback) {
        AsyncTask<Void, Void, List> task = new AsyncTask<Void, Void, List>() {
            @Override
            protected List doInBackground(Void... voids) {
                Realm realm = Realm.getDefaultInstance();
                RealmQuery<PostDataModel> resultQuery = realm.where(PostDataModel.class).equalTo("isFavorite", true);

                RealmResults<PostDataModel> results = resultQuery.findAllSorted("createdAt", Sort.DESCENDING);
                final ArrayList<PostModel> postModels = new ArrayList<>();

                for (PostDataModel postDataModel : results) {
                    PostModel postModel = PostModel.createFromDatabase(postDataModel);
                    postModels.add(postModel);
                }

                realm.close();
                return postModels;
            }

            @Override
            protected void onPostExecute(List aList) {
                super.onPostExecute(aList);

                callback.onDatabaseResponse(aList);
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void sendNews(String title, String html, List<Uri> attachLinks, NetworkCoordinatorErrorCallback callback) {
        ArrayList<Attachment> attachments = new ArrayList<>();
        int i = 1;
        for (Uri url : attachLinks) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), url);
                Attachment attachment = new Attachment(String.valueOf(i), bitmap);
                attachments.add(attachment);
            } catch (Exception e) {

            } finally {
                i++;
            }
        }

        html = html.trim();

        network.sendMail("news@revbel.org", "Новость от пользователя", title, null, html, attachments, new Network.NetworkErrorCallback() {
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }

            @Override
            public void onPostResponse(JSONArray items) {
                callback.onNetworkResponse(Collections.singletonList(title));
            }
        });
    }

    public void sendPoll(String email, String name, String text, NetworkCoordinatorErrorCallback callback) {
        network.sendMail(email, name, "Анкета", text, null, null, new Network.NetworkErrorCallback() {
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }

            @Override
            public void onPostResponse(JSONArray items) {
                callback.onNetworkResponse(Collections.singletonList(email));
            }
        });
    }
}
