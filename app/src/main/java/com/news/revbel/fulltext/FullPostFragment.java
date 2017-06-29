package com.news.revbel.fulltext;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.ShareActionProvider;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.news.revbel.RevApplication;
import com.news.revbel.composers.FullTextViewComposer;
import com.news.revbel.utilities.ControlActivityInterface;
import com.news.revbel.utilities.PostOpenInterface;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.viewmodel.ArticlePostModel;
import com.news.revbel.viewmodel.PostModel;
import com.news.revbel.R;
import com.news.revbel.databinding.FragmentFullPostBinding;
import com.news.revbel.viewmodel.ReplyModel;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class FullPostFragment extends Fragment {
    @Inject PostListCoordinator coordinator;

    @BindView(R.id.container) View containerView;
    @BindView(R.id.reply_button) Button replyButton;
    @BindView(R.id.nameEdit) EditText nameEdit;
    @BindView(R.id.emailText) EditText emailEdit;
    @BindView(R.id.textEdit) EditText textEdit;
    @BindView(R.id.fragment_full_post) LinearLayout containerLayout;

    @BindString(R.string.full_post_answering_title) String answeringTitle;
    @BindString(R.string.full_post_answering_message) String replyingMessage;
    @BindString(R.string.full_post_answered) String replyTitle;
    @BindString(R.string.full_post_answer_need_name) String replyNeedName;
    @BindString(R.string.full_post_answer_need_email) String replyNeedEmail;
    @BindString(R.string.full_post_answer_need_correct_email) String replyNeedCorrectEmail;
    @BindString(R.string.full_post_answer_need_text) String replyNeedText;
    @BindString(R.string.full_post_answered_error) String replyErrorTitle;
    @BindString(R.string.full_post_answered_error_action) String replyedErrorAction;
    @BindColor(R.color.green) int actionColor;

    private Unbinder unbinder;
    private FragmentFullPostBinding binding;

    private ControlActivityInterface activityInterface;
    private FullTextViewComposer composer;

    public static final String POST_MODEL_ID = "post_model_id";
    public static final String POST_MODEL_TYPE = "post_model_type";
    public static final String FRAGMENT_TAG = "FRAGMENT_TAG";
    private PostModel postModel;

    private boolean loadingURL = false;

    public FullPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RevApplication.getComponent().inject(this);

        if (getArguments().containsKey(POST_MODEL_ID)) {
            int postId = getArguments().getInt(POST_MODEL_ID);
            String postType = getArguments().getString(POST_MODEL_TYPE);
            postModel = coordinator.getPost(postId, postType);
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
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_full_post, container, false);
        binding.setItem(postModel);
        binding.setReplies(postModel.replies);
        binding.executePendingBindings();
        View view = binding.getRoot();

        unbinder = ButterKnife.bind(this, view);

        containerView.setVisibility(View.GONE);
        composer = new FullTextViewComposer(getActivity(), this, containerLayout);

        if (activityInterface != null) activityInterface.startLoading();

        Runnable postText = () -> {
            String text = postModel.text.get();
            if (text != null && text.length() > 0) {
                composer.loadHTMLString(text, () -> {
                    if (containerView != null) containerView.setVisibility(View.VISIBLE);
                    if (activityInterface != null) {
                        activityInterface.stopLoading();
                    }
                });
            }
        };
        postModel.getFullPost(null);

        postText.run();
        postModel.text.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                postText.run();
            }
        });

        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_share);

        if (item != null) {
            ShareActionProvider shareProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
            String shareText = postModel.link;
            if (shareProvider != null) {
                Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain").setText(shareText).getIntent();
                shareProvider.setShareIntent(shareIntent);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View view = containerLayout.getChildAt(i);
            if (view instanceof WebView) {
                ((WebView) view).loadData("", "text/html", "UTF-8");
            }
        }
        composer.cleanUpView();
        unbinder.unbind();
    }

    public void showWebURL(Uri link) {
        if (getActivity() instanceof PostOpenInterface) {
            PostOpenInterface activity = (PostOpenInterface) getActivity();
            activity.openWebURL(link.toString());
        }
    }

    public void showPostByLink(Uri link) {
        if (!loadingURL) {
            loadingURL = true;
            if (getActivity() instanceof PostOpenInterface) {
                PostOpenInterface activity = (PostOpenInterface) getActivity();
                activity.openLink(link, () -> loadingURL = false);
            }
        }
    }

    private void setEnabledComment(boolean enabled) {
        nameEdit.setEnabled(enabled);
        emailEdit.setEnabled(enabled);
        textEdit.setEnabled(enabled);
        replyButton.setEnabled(enabled);
    }

    private void clearComment() {
        nameEdit.setText("");
        emailEdit.setText("");
        textEdit.setText("");
    }

    private EditText findFirstErroredEdit() {
        if (nameEdit.getText().toString().isEmpty()) {
            Toast.makeText(getActivity(), replyNeedName, Toast.LENGTH_SHORT).show();
            return nameEdit;
        }
        String email = emailEdit.getText().toString();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (email.isEmpty()) {
                Toast.makeText(getActivity(), replyNeedEmail, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), replyNeedCorrectEmail, Toast.LENGTH_SHORT).show();
            }
            return emailEdit;
        }
        if (textEdit.getText().length() < 5) {
            Toast.makeText(getActivity(), replyNeedText, Toast.LENGTH_SHORT).show();
            return textEdit;
        }
        return null;
    }

    @OnClick(R.id.load_more)
    void onLoadMoreClick() {
        if (activityInterface != null) activityInterface.startLoading();
        postModel.replies.getOlderPosts(() -> {
            if (activityInterface != null) activityInterface.stopLoading();
        });
    }

    @OnClick(R.id.reply_button)
    void onReplyClick() {
        EditText editText = findFirstErroredEdit();
        if (editText == null) {
            setEnabledComment(false);
            ProgressDialog sendingComment = ProgressDialog.show(getActivity(), answeringTitle, replyingMessage, true);

            postModel.postReplyOnPost(0,
                    nameEdit.getText().toString(),
                    emailEdit.getText().toString(),
                    textEdit.getText().toString(), new PostModel.ViewModelPostReplyCallback() {
                        @Override
                        public void onFailure(Exception e) {
                            setEnabledComment(true);
                            String fullErrorText = replyErrorTitle + e.toString();
                            Snackbar snackbar = Snackbar.make(containerView, fullErrorText, BaseTransientBottomBar.LENGTH_LONG);
                            snackbar.setAction(replyedErrorAction, view -> onReplyClick()).setActionTextColor(actionColor).show();
                            sendingComment.dismiss();
                        }

                        @Override
                        public void onSuccess(ReplyModel replyModel) {
                            setEnabledComment(true);
                            clearComment();
                            sendingComment.dismiss();
                            Toast.makeText(getActivity(), replyTitle, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            if(editText.requestFocus()) {
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        }
    }
}
