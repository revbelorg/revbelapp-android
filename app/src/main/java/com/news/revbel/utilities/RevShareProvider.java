package com.news.revbel.utilities;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.ActivityChooserView;
import android.support.v7.widget.ShareActionProvider;
import android.view.View;

import com.news.revbel.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class RevShareProvider extends ShareActionProvider {
    /**
     * Creates a new instance.
     *
     * @param context Context for accessing resources.
     */
    public RevShareProvider(Context context) {
        super(context);
    }

    @Override
    public View onCreateActionView() {
        ActivityChooserView chooserView = (ActivityChooserView) super.onCreateActionView();
        Drawable icon;
        if (Build.VERSION.SDK_INT >= 21) {
            icon = getContext().getDrawable(R.drawable.ic_action_share);
        }else{
            icon = getContext().getResources().getDrawable(R.drawable.ic_action_share);
        }

        Class chooserViewClass = chooserView.getClass();


        try {
            Method method = chooserViewClass.getMethod("setExpandActivityOverflowButtonDrawable", Drawable.class);
            method.invoke(chooserView, icon);

            method = chooserViewClass.getMethod("getDataModel");
            Object dataModel = method.invoke(chooserView);

            Class dataClass = dataModel.getClass();
            method = dataClass.getMethod("setHistoryMaxSize", int.class);
            method.invoke(dataModel, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return chooserView;
    }

}
