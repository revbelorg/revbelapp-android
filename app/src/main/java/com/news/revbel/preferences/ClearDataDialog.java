package com.news.revbel.preferences;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

import com.news.revbel.R;
import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.SettingsCoordinator;

import javax.inject.Inject;

public class ClearDataDialog extends DialogPreference {
    @Inject SettingsCoordinator settingsCoordinator;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ClearDataDialog(Context context) {
        super(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ClearDataDialog(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        RevApplication.getComponent().inject(this);
    }

    public ClearDataDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        RevApplication.getComponent().inject(this);
    }

    public ClearDataDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        RevApplication.getComponent().inject(this);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {

            new AsyncTask<Void, Void, Void>() {
                ProgressDialog dialog;
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    String title = getContext().getResources().getString(R.string.pref_clear_data_progress_title);
                    String message = getContext().getResources().getString(R.string.pref_clear_data_progress_message);
                    dialog = ProgressDialog.show(ClearDataDialog.this.getContext(), title, message, true);
                }

                @Override
                protected Void doInBackground(Void... voids) {
                    settingsCoordinator.clearAllData();
                    Thread.currentThread();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    dialog.dismiss();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        }
    }
}
