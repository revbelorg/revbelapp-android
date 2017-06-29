package com.news.revbel.postlist;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.news.revbel.R;
import com.news.revbel.viewmodel.TaxonomyModel;
import com.news.revbel.viewmodel.CategoryPostListViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class BreadcrumbSpinner extends android.support.v7.widget.AppCompatSpinner {
    interface BreadcrumbSpinnerItemCallback {
        void onItemSelected(AdapterView<?> parent, View view, int position);
    }

    BreadcrumbSpinnerItemCallback callback;

    public BreadcrumbSpinner(Context context) {
        super(context);
    }

    public BreadcrumbSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public BreadcrumbSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public void setCategory(TaxonomyModel category, CategoryPostListViewModel postListViewModel) {
        final List<String> categoryList = category.childTaxonomies;
        ArrayList<String> spinnerArray = new ArrayList<>();
        spinnerArray.add("Все");
        for (String catId : categoryList) {
            spinnerArray.add(postListViewModel.coordinator.categoryListViewModel.listAll.get(catId).name);
        }

        setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                callback.onItemSelected(parent, view, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("Log", "Nothing selected!");
            }
        });

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getContext(),
                R.layout.breadcrumb_category_item,
                spinnerArray);

        spinnerArrayAdapter.setDropDownViewResource(R.layout.breadcrumb_category_dropdown_item);
        setAdapter(spinnerArrayAdapter);

        this.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                if (android.os.Build.VERSION.SDK_INT > 18) {
                    this.cancelPendingInputEvents();
                }
                return true;
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                BreadcrumbSpinner.this.performClick();
                return true;
            }
            return false;
        });
    }

    public void setAdapter(SpinnerAdapter adapter) {
        super.setAdapter(adapter != null ? new BreadcrumbSpinnerAdapter(adapter) : null);
    }

    private final class BreadcrumbSpinnerAdapter implements SpinnerAdapter {

        private final SpinnerAdapter mBaseAdapter;

        BreadcrumbSpinnerAdapter(SpinnerAdapter baseAdapter) {
            mBaseAdapter = baseAdapter;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return mBaseAdapter.getView(getSelectedItemPosition(), convertView, parent);
        }

        public final SpinnerAdapter getBaseAdapter() {
            return mBaseAdapter;
        }

        public int getCount() {
            return mBaseAdapter.getCount();
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return mBaseAdapter.getDropDownView(position, convertView, parent);
        }

        public Object getItem(int position) {
            return mBaseAdapter.getItem(position);
        }

        public long getItemId(int position) {
            return mBaseAdapter.getItemId(position);
        }

        public int getItemViewType(int position) {
            return mBaseAdapter.getItemViewType(position);
        }

        public int getViewTypeCount() {
            return mBaseAdapter.getViewTypeCount();
        }

        public boolean hasStableIds() {
            return mBaseAdapter.hasStableIds();
        }

        public boolean isEmpty() {
            return mBaseAdapter.isEmpty();
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            mBaseAdapter.registerDataSetObserver(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            mBaseAdapter.unregisterDataSetObserver(observer);
        }
    }
}
