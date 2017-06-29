package com.news.revbel.utilities;

import android.support.v4.app.Fragment;

public interface ControlActivityInterface {
    void updateControlButtonTapEvent(Fragment fragment, Runnable event);
    void onFragmentHide(Fragment fragment);
    void showControlButton(Fragment fragment);
    void hideControlButton(Fragment fragment);
    void alertControlButton(Fragment fragment);
    void startLoading();
    void stopLoading();
}
