package com.news.revbel.feedback;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.news.revbel.R;
import com.news.revbel.utilities.RevKnifeText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.github.mthli.knife.KnifeText;


public class RichEditorFragment extends Fragment {
    private Unbinder unbinder;

    @BindView(R.id.editor) RevKnifeText knife;
    @BindView(R.id.action_bold) ImageButton boldButton;
    @BindView(R.id.action_italic) ImageButton italicButton;
    @BindView(R.id.action_undo) ImageButton undoButton;
    @BindView(R.id.action_redo) ImageButton redoButton;

    public RichEditorFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_rich_editor, container, false);
        unbinder = ButterKnife.bind(this, view);

        updateRedoUndoButtons();

        knife.setHintTextColor(ContextCompat.getColor(getActivity(), android.R.color.darker_gray));

        knife.setOnKeyListener((view1, i, keyEvent) -> {
            updateRedoUndoButtons();
            return false;
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.action_bold)
    void onBoldClick() {
        if (knife.getSelectionStart() < knife.getSelectionEnd()) {
            knife.bold(!knife.contains(KnifeText.FORMAT_BOLD));
        } else {
            knife.toggleBold();
            if (knife.isBoldCursor()) {
                boldButton.setBackgroundColor(Color.argb(50, 255, 255, 255));
            } else {
                boldButton.setBackgroundColor(Color.argb(0,0,0,0));
            }
        }

        updateRedoUndoButtons();
    }

    @OnClick(R.id.action_italic)
    void onItalicClick() {
        if (knife.getSelectionStart() < knife.getSelectionEnd()) {
            knife.italic(!knife.contains(KnifeText.FORMAT_ITALIC));
        } else {
            knife.toggleItalic();
            if (knife.isItalicCursor()) {
                italicButton.setBackgroundColor(Color.argb(50, 255, 255, 255));
            } else {
                italicButton.setBackgroundColor(Color.argb(0,0,0,0));
            }
        }
        updateRedoUndoButtons();
    }

    @OnClick(R.id.action_insert_bullets)
    void onBulletClick() {
        knife.bullet(!knife.contains(KnifeText.FORMAT_BULLET));
        updateRedoUndoButtons();
    }

    @OnClick(R.id.action_blockquote)
    void onQuoteClick() {
        knife.quote(!knife.contains(KnifeText.FORMAT_QUOTE));
        updateRedoUndoButtons();
    }

    @OnClick(R.id.action_undo)
    void onUndoClick() {
        knife.undo();
        updateRedoUndoButtons();
    }

    @OnClick(R.id.action_redo)
    void onRedoClick() {
        knife.redo();
        updateRedoUndoButtons();
    }

    @OnClick(R.id.action_insert_link)
    void onInsertLinkClick() {
        final int start = knife.getSelectionStart();
        final int end = knife.getSelectionEnd();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_link, null, false);
        final EditText editText = (EditText) view.findViewById(R.id.edit);
        builder.setView(view);
        builder.setTitle(R.string.link_dialog_title);


        builder.setPositiveButton(R.string.link_dialog_button_ok, (dialog, which) -> {
            String link = editText.getText().toString().trim();
            if (link.isEmpty()) {
                return;
            }
            if (end > start) {
                knife.link(link, start, end);
            } else {
                knife.append(link);
                knife.link(link, end, knife.getText().length());
            }
            updateRedoUndoButtons();
        });

        builder.setNegativeButton(R.string.link_dialog_button_cancel, (dialog, which) -> {

        });

        builder.create().show();
    }

    @OnClick(R.id.action_clear_format)
    void onClearFormatClick() {
        knife.clearFormats();
    }

    private void updateRedoUndoButtons() {
        undoButton.setEnabled(knife.undoValid());

        undoButton.setAlpha(undoButton.isEnabled() ? 1.f : 0.5f);
    }
}
