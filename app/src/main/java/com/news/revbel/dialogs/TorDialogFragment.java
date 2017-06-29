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
import android.widget.RadioGroup;
import android.widget.TextView;

import com.news.revbel.R;

public class TorDialogFragment extends DialogFragment {
    public interface TorDialogFragmentCallback {
        void onUsingTor();
        void onUsingBitmask();
        void onUsingWeb();
    }

    public TorDialogFragmentCallback callback;
    public boolean hasTor;
    public boolean hasBitmask;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_tor_layout, null);

        TextView textView = (TextView) view.findViewById(R.id.radio_use_Tor);
        textView.setText(getResources().getString(hasTor ? R.string.network_installed_dialog_radio_ok : R.string.network_dialog_radio_orbot));

        textView = (TextView) view.findViewById(R.id.radio_use_Bitmask);
        textView.setText(getResources().getString(hasBitmask ? R.string.network_dialog_radio_installed_bitmask : R.string.network_dialog_radio_bitmask));

        RadioGroup torRadioGroup = (RadioGroup) view.findViewById(R.id.radio_tor);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        builder.setCancelable(false);
        builder.setView(view)
                .setPositiveButton(R.string.network_dialog_ok, (dialog, id) -> {
                    if (torRadioGroup.getCheckedRadioButtonId() == R.id.radio_use_Tor) {
                        callback.onUsingTor();
                    } else if (torRadioGroup.getCheckedRadioButtonId() == R.id.radio_use_Bitmask) {
                        callback.onUsingBitmask();
                    } else if (torRadioGroup.getCheckedRadioButtonId() == R.id.radio_not_use_Tor) {
                        callback.onUsingWeb();
                    }
                    TorDialogFragment.this.getDialog().dismiss();
                });

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialog1 -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

            positiveButton.setBackgroundColor(Color.BLACK);
        });

        return dialog;
    }

}
