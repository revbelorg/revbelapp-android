package com.news.revbel.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import com.news.revbel.R;

public class OpenBookDialogFragment extends DialogFragment {
    public interface OpenBookDialogFragmentCallback {
        void onUsingExternalActivity(boolean save);
        void onUsingInternalActivity(boolean save);
    }

    public OpenBookDialogFragmentCallback callback;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_open_book_layout, null);
        RadioGroup torRadioGroup = (RadioGroup) view.findViewById(R.id.radio_tor);

        CheckBox checkBox = (CheckBox) view.findViewById(R.id.save_option);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        builder.setView(view)
                .setPositiveButton(R.string.book_dialog_ok, (dialog, id) -> {
                    if (torRadioGroup.getCheckedRadioButtonId() == R.id.radio_use_external) {
                        callback.onUsingExternalActivity(checkBox.isChecked());
                    } else if (torRadioGroup.getCheckedRadioButtonId() == R.id.radio_use_internal) {
                        callback.onUsingInternalActivity(checkBox.isChecked());
                    }
                    OpenBookDialogFragment.this.getDialog().dismiss();
                });

        AlertDialog dialog = builder.create();
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        dialog.setOnShowListener(dialog1 -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

            positiveButton.setBackgroundColor(Color.BLACK);
        });


        return dialog;
    }

}
