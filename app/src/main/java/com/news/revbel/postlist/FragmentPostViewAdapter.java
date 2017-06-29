package com.news.revbel.postlist;

import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.news.revbel.coordinator.PostListCoordinator;
import com.news.revbel.utilities.AspectRatioImageView;
import com.news.revbel.utilities.SimpleRecyclerViewAdapter;
import com.news.revbel.R;
import com.news.revbel.viewmodel.PostListViewModel;
import com.news.revbel.viewmodel.PostModel;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.List;

public class FragmentPostViewAdapter<T> extends SimpleRecyclerViewAdapter<T> {
    public PostListListeners.ItemCellClickedCallback listenersCallback;
    public PostListViewModel postListViewModel;

    @Override
    public void onBindBinding(ViewDataBinding binding, int bindingVariable, @LayoutRes int layoutRes, int position, T item) {
        super.onBindBinding(binding, bindingVariable, layoutRes, position, item);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        PostModel postModel = postListViewModel.items.get(position);

        AspectRatioImageView imageView = (AspectRatioImageView) holder.itemView.findViewById(R.id.post_image);
        imageView.setAspectRatioEnabled(true);
        imageView.setDominantMeasurement(AspectRatioImageView.MEASUREMENT_WIDTH);
        imageView.setAspectRatio(postModel.featuredImageHeight);

        ViewGroup.LayoutParams params =  imageView.getLayoutParams();
        params.height = (int) (params.width * postModel.featuredImageHeight);
        imageView.setLayoutParams(params);

        if (postModel.featuredMediaUrl != null) {
            Glide
                    .with(imageView.getContext().getApplicationContext())
                    .load(postModel.featuredMediaUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .crossFade()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.image_placeholder);
        }

        TextView textTitle = (TextView) holder.itemView.findViewById(R.id.title_post);
        textTitle.setText(postModel.getTitleSpanned());

        TextView excerptTitle = (TextView) holder.itemView.findViewById(R.id.post_excerpt);
        excerptTitle.setText(postModel.getExcerptSpanned());

        final AVLoadingIndicatorView activityIndicator = (AVLoadingIndicatorView) holder.itemView.findViewById(R.id.progress_bar);

        final ImageButton favoriteButton = (ImageButton) holder.itemView.findViewById(R.id.favoriteButton);

        activityIndicator.setVisibility(View.INVISIBLE);
        favoriteButton.setVisibility(View.VISIBLE);

        favoriteButton.setSelected(postModel.isFavorite);
        favoriteButton.setOnClickListener(view -> {
            activityIndicator.setVisibility(View.VISIBLE);
            favoriteButton.setVisibility(View.INVISIBLE);
            postModel.setIsFavorite(!favoriteButton.isSelected(), new PostListCoordinator.ViewModelGetPostCallback() {
                @Override
                public void onFailure() {
                    activityIndicator.setVisibility(View.INVISIBLE);
                    favoriteButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onSuccess(PostModel postModel) {
                    activityIndicator.setVisibility(View.INVISIBLE);
                    favoriteButton.setVisibility(View.VISIBLE);
                    favoriteButton.setSelected(!favoriteButton.isSelected());
                }
            });
        });

        LinearLayout linearLayout = (LinearLayout) holder.itemView.findViewById(R.id.post_container);
        linearLayout.setOnClickListener(v -> listenersCallback.onReadMoreButtonClicked(v, position));
    }
}
