package com.news.revbel.feedback;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.news.revbel.R;
import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;
import com.news.revbel.filelist.FilesListener;
import com.news.revbel.fulltext.GalleryFragment;
import com.news.revbel.utilities.ControlActivityInterface;
import com.news.revbel.utilities.GlideStorageEngine;
import com.news.revbel.viewmodel.ImageModel;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.github.mthli.knife.KnifeText;

import static android.app.Activity.RESULT_OK;


public class PostNewsFragment extends Fragment {
    private final static int REQUEST_CODE_IMAGEPICK = 1392;
    private HashSet<Uri> attachments = new HashSet<>();

    private final static String ATTACHMENTS_KEY = "ATTACHMENTS";
    private final static String TITLE_KEY = "TITLE";
    private final static String TEXT_KEY = "TEXT";

    private GalleryFragment gallery;

    GalleryFragment.GalleryFragmentDeleteCallback deleteCallback = new GalleryFragment.GalleryFragmentDeleteCallback() {
        @Override
        public void deleteImage(ImageModel model) {
            attachments.remove(model.thumbUrl);
            if (attachments.size() == 0) {
                attachmentContainer.setVisibility(View.GONE);
            }
        }
    };

    @Inject NetworkCoordinator coordinator;
    @BindView(R.id.editor) KnifeText editor;
    @BindView(R.id.edit_title) EditText titleEdit;
    @BindView(R.id.attachments) FrameLayout attachmentContainer;

    @BindString(R.string.sendpost_sended) String sentMessage;
    @BindString(R.string.sendpost_error) String errorMessage;
    @BindString(R.string.sendpost_notvalid) String notValidMessage;

    private ControlActivityInterface activityInterface;
    private Unbinder unbinder;

    public PostNewsFragment() {
    }

    public static PostNewsFragment newInstance() {
        PostNewsFragment fragment = new PostNewsFragment();

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof ControlActivityInterface) {
            activityInterface = (ControlActivityInterface) getActivity();
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
    public void onSaveInstanceState(Bundle outState) {
        ArrayList<String> attachmentsString = new ArrayList<>();
        for (Uri url : attachments) {
            attachmentsString.add(url.toString());
        }
        outState.putStringArrayList(ATTACHMENTS_KEY, attachmentsString);
        outState.putString(TITLE_KEY, titleEdit.getText().toString());
        outState.putString(TEXT_KEY, editor.toHtml());
//        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RevApplication.getComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (activityInterface != null) activityInterface.hideControlButton(this);
        View view = inflater.inflate(R.layout.fragment_send_news, container, false);
        unbinder = ButterKnife.bind(this, view);
        if (savedInstanceState != null) {
            titleEdit.setText(savedInstanceState.getString(TITLE_KEY));
            editor.fromHtml(savedInstanceState.getString(TEXT_KEY));
            ArrayList<Uri> attachmentsUri = new ArrayList<>();
            ArrayList<String> attachmentsString = savedInstanceState.getStringArrayList(ATTACHMENTS_KEY);
            gallery = (GalleryFragment) getChildFragmentManager().findFragmentById(R.id.attachments);
            gallery.deleteCallback = deleteCallback;
            if (attachmentsString != null) {
                for (String url : attachmentsString) {
                    attachmentsUri.add(Uri.parse(url));
                }
                updateAttachments(attachmentsUri);
            }
        } else {
            gallery = GalleryFragment.newInstance(Collections.emptyList(), 100, false, true, deleteCallback);

            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.attachments, gallery).commit();
        }
        return view;
    }

    @OnClick(R.id.send_button)
    void onSendButtonClick() {
        String title = titleEdit.getText().toString();
        String html = editor.toHtml();
        boolean isValid = title.trim().length() > 3;
        isValid = isValid && html.trim().length() > 20;
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editor.getWindowToken(), 0);

        if (!isValid) {
            Toast.makeText(getActivity(), notValidMessage, Toast.LENGTH_LONG).show();
        } else {
            ProgressDialog progressDialog = ProgressDialog.show(getActivity(),getString(R.string.sendpost_progress_title),getString(R.string.sendpost_progress_message), true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            coordinator.sendNews(title, html, new ArrayList<Uri>(attachments), new NetworkCoordinator.NetworkCoordinatorErrorCallback() {
                @Override
                public void onFailure(Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), errorMessage + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onNetworkResponse(List items) {
                    progressDialog.dismiss();
                    titleEdit.getText().clear();
                    editor.getText().clear();
                    editor.clearHistory();
                    attachments.clear();
                    attachmentContainer.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), sentMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @OnClick(R.id.upload_button)
    void onUploadFileClicked() {
        FilesListener filesListener = new FilesListener();
        filesListener.checkReadWriteDownloadFolderPermissions(getActivity(), () -> {
            Matisse.from(this)
                    .choose(MimeType.allOf())
                    .countable(true)
                    .maxSelectable(9)
                    .thumbnailScale(0.85f)
                    .theme(R.style.Matisse_Dracula)
                    .imageEngine(new GlideStorageEngine())
                    .forResult(REQUEST_CODE_IMAGEPICK);
        }, null);
    }

    private void updateAttachments(List<Uri> list) {
        attachments.addAll(list);
        ArrayList<ImageModel> models = new ArrayList<>();
        for (Uri img : attachments) {
            ImageModel imageModel = new ImageModel();
            imageModel.thumbUrl = img;
            models.add(imageModel);
        }
        gallery.updateImagesWithList(models);
        if (attachments.size() > 0) attachmentContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMAGEPICK && resultCode == RESULT_OK) {
            List<Uri> list = Matisse.obtainResult(data);
            updateAttachments(list);
        }
    }
}
