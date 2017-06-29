package com.news.revbel.filelist;

import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.news.revbel.R;
import com.news.revbel.utilities.AspectRatioImageView;
import com.news.revbel.utilities.SimpleRecyclerViewAdapter;
import com.news.revbel.viewmodel.FileViewModel;
import com.news.revbel.viewmodel.ListedFilesViewModel;

import java.util.List;

public class FileListRecycleAdapter<T> extends SimpleRecyclerViewAdapter<T> {
    public ListedFilesViewModel listedFilesViewModel;

    @Override
    public void onBindBinding(ViewDataBinding binding, int bindingVariable, @LayoutRes int layoutRes, int position, T item) {
         super.onBindBinding(binding, bindingVariable, layoutRes, position, item);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        FileViewModel viewModel = listedFilesViewModel.items.get(position);

        TextView textView = (TextView) holder.itemView.findViewById(R.id.text_id);
        textView.setText(viewModel.bookTitle);

        AspectRatioImageView imageView = (AspectRatioImageView) holder.itemView.findViewById(R.id.fileImage);
        if (viewModel.imageURL != null && !viewModel.imageURL.isEmpty()) {
            imageView.setAspectRatioEnabled(true);
            imageView.setDominantMeasurement(AspectRatioImageView.MEASUREMENT_WIDTH);
            imageView.setAspectRatio(viewModel.imageHeightAspect);

            ViewGroup.LayoutParams params = imageView.getLayoutParams();
            params.height = (int) (params.width * viewModel.imageHeightAspect);
            imageView.setLayoutParams(params);

            Glide
                    .with(imageView.getContext())
                    .load(viewModel.imageURL)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
            imageView.setVisibility(View.VISIBLE);
        } else if (imageView.getVisibility() != View.GONE) {
            imageView.setVisibility(View.GONE);
        }

    }
}
