package com.news.revbel.utilities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.widget.RelativeLayout;

import com.news.revbel.R;
import com.wang.avi.AVLoadingIndicatorView;
import com.wang.avi.indicators.BallSpinFadeLoaderIndicator;

import me.tatarka.bindingcollectionadapter2.LayoutManagers;

public class ViewUtilities {
    public static AVLoadingIndicatorView indicatorViewForMedia(Context context, RelativeLayout parent) {
        AVLoadingIndicatorView loader = new AVLoadingIndicatorView(context);
        loader.setIndicator(new BallSpinFadeLoaderIndicator());
        RelativeLayout.LayoutParams loaderParams = new RelativeLayout.LayoutParams(60, 60);
        loaderParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        loader.setIndicatorColor(ContextCompat.getColor(context, android.R.color.black));
        parent.addView(loader, loaderParams);
        return loader;
    }

    public static void hangingViewAnimation(View view) {
        AnimatorSet set = new AnimatorSet();

        final float imgTX = view.getTranslationX();
        final float imgTY = view.getTranslationY();

        set.playSequentially(
                ObjectAnimator.ofPropertyValuesHolder(
                        view,
                        PropertyValuesHolder.ofFloat("translationX", -5f),
                        PropertyValuesHolder.ofFloat("translationY", -5f))
                        .setDuration(50),
                ObjectAnimator.ofPropertyValuesHolder(
                        view,
                        PropertyValuesHolder.ofFloat("translationX", -5f),
                        PropertyValuesHolder.ofFloat("translationY", 5f))
                        .setDuration(50),
                ObjectAnimator.ofPropertyValuesHolder(
                        view,
                        PropertyValuesHolder.ofFloat("translationX", 5f),
                        PropertyValuesHolder.ofFloat("translationY", -5f))
                        .setDuration(50),
                ObjectAnimator.ofPropertyValuesHolder(
                        view,
                        PropertyValuesHolder.ofFloat("translationX", 5f),
                        PropertyValuesHolder.ofFloat("translationY", 5f))
                        .setDuration(50),
                ObjectAnimator.ofPropertyValuesHolder(
                        view,
                        PropertyValuesHolder.ofFloat("translationX", -5f),
                        PropertyValuesHolder.ofFloat("translationY", 5f))
                        .setDuration(50),
                ObjectAnimator.ofPropertyValuesHolder(
                        view,
                        PropertyValuesHolder.ofFloat("translationX", imgTX),
                        PropertyValuesHolder.ofFloat("translationY", imgTY))
                        .setDuration(80)
        );
        set.start();
    }

    public static LayoutManagers.LayoutManagerFactory staggeredGrid(final int spanCount, @LayoutManagers.Orientation final int orientation) {
        return recyclerView -> {
            StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(spanCount, orientation);
            return staggeredGridLayoutManager;
        };
    }

    public static LayoutManagers.LayoutManagerFactory nonScrolledLinear(Context context) {
        return recyclerView -> {
            return new LinearLayoutManager(context) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            };
        };
    }

    public static void showAlertDialog(Context context, String title, String message, @DrawableRes int drawable, Runnable onSuccess, Runnable onFailure) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();

        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setIcon(drawable);
        alertDialog.setCancelable(false);

        if (onSuccess != null) {
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(R.string.dialog_ok), (dialog, which) -> {
                onSuccess.run();
                alertDialog.dismiss();
            });
        }

        if (onFailure != null) {
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(R.string.dialog_cancel), (dialog, which) -> {
                onFailure.run();
                alertDialog.dismiss();
            });
        }
        alertDialog.show();
    }

    public static int blendColors(int from, int to, float ratio) {
        final float inverseRatio = 1f - ratio;

        final float r = Color.red(to) * ratio + Color.red(from) * inverseRatio;
        final float g = Color.green(to) * ratio + Color.green(from) * inverseRatio;
        final float b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio;
        final float a = Color.alpha(to) * ratio + Color.alpha(from) * inverseRatio;


        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }
}
