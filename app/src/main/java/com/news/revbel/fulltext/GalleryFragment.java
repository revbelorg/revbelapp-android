package com.news.revbel.fulltext;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.news.revbel.utilities.AspectRatioImageView;
import com.news.revbel.utilities.MediaFullscreenProtocol;
import com.news.revbel.utilities.WrapContentHeightViewPager;
import com.news.revbel.viewmodel.ImageModel;
import com.news.revbel.R;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment {
    public interface GalleryFragmentDeleteCallback {
        void deleteImage(ImageModel model);
    }
    private WrapContentHeightViewPager pager;
    private List<ImageModel> imageModels;
    private ArrayList<ImageView> thumbs = new ArrayList<>();

    private boolean showBigPicture;
    private final static String SHOW_PICTURE_KEY = "SHOW_PICTURE_KEY";

    private boolean canBeDeleted;
    private final static String CAN_BE_DELETED_KEY = "CAN_BE_DELETED_KEY";

    private int thumbSize = -1;
    private final static String THUMB_SIZE_KEY = "THUMB_SIZE_KEY";

    private GalleryAdapter galleryAdapter;
    public GalleryFragmentDeleteCallback deleteCallback;
    private View view;

    public static GalleryFragment newInstance(List<ImageModel> models) {
        GalleryFragment fragment = new GalleryFragment();
        fragment.imageModels = models;
        fragment.showBigPicture = true;
        fragment.canBeDeleted = false;
        return fragment;
    }

    public static GalleryFragment newInstance(List<ImageModel> models, int thumbSize, boolean showBigPicture, boolean canBeDeleted, GalleryFragmentDeleteCallback callback) {
        GalleryFragment fragment = new GalleryFragment();
        fragment.imageModels = models;
        fragment.showBigPicture = showBigPicture;
        fragment.canBeDeleted = canBeDeleted;
        fragment.deleteCallback = callback;
        fragment.thumbSize = thumbSize;
        return fragment;
    }

    public GalleryFragment() {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOW_PICTURE_KEY, showBigPicture);
        outState.putBoolean(CAN_BE_DELETED_KEY, canBeDeleted);
        outState.putInt(THUMB_SIZE_KEY, thumbSize);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.content_image_gallery, container, false);

        galleryAdapter = new GalleryAdapter();
        galleryAdapter.inflater = inflater;

        pager = (WrapContentHeightViewPager)view.findViewById(R.id.pager);
        pager.setAdapter(galleryAdapter);

        if (savedInstanceState == null) {
            updateThumbs();
            updateImageModels();
        } else  {
            showBigPicture = savedInstanceState.getBoolean(SHOW_PICTURE_KEY);
            canBeDeleted = savedInstanceState.getBoolean(CAN_BE_DELETED_KEY);
            thumbSize = savedInstanceState.getInt(THUMB_SIZE_KEY);
            updateThumbs();
            updateImageModels();
        }

        return view;
    }

    private void updateThumbs() {
        pager.setVisibility(showBigPicture ? View.VISIBLE : View.GONE);

        if (thumbSize > -1) {
            View thumbScroll = view.findViewById(R.id.thumb_scroll);
            ViewGroup.LayoutParams layoutParams = thumbScroll.getLayoutParams();
            layoutParams.height = thumbSize;
            thumbScroll.setLayoutParams(layoutParams);
        }
        if (showBigPicture) {
            pager.setAdapter(galleryAdapter);
            pager.setOffscreenPageLimit(5);
        }
    }

    private void updateImageModels() {
        if (view == null) view = getView();
        if (view != null) {
            LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.thumbnails);

            linearLayout.removeAllViews();
            pager.invalidate();
            pager.getAdapter().notifyDataSetChanged();

            thumbs.clear();

            for (int i = 0; i < imageModels.size(); i++) {
                ImageModel imageModel = imageModels.get(i);
                int thumbnailSize = thumbSize == -1 ? ((FrameLayout.LayoutParams) pager.getLayoutParams()).bottomMargin - 4 : thumbSize;

                final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(thumbnailSize, thumbnailSize);
                params.setMargins(0, 0, 2, 0);

                ImageView thumbView = new ImageView(getContext());
                if (imageModel.thumbUrl.getScheme().contains("http")) {
                    Glide
                            .with(getContext())
                            .load(imageModel.thumbUrl)
                            .priority(Priority.HIGH)
                            .fitCenter()
                            .crossFade()
                            .into(thumbView);
                } else {
                    Glide
                            .with(getContext())
                            .fromMediaStore()
                            .asBitmap()
                            .load(imageModel.thumbUrl)
                            .centerCrop()
                            .into(thumbView);

                }
                thumbView.setLayoutParams(params);

                final int finalI = i;

                if (!canBeDeleted) {
                    thumbView.setOnClickListener(v -> pager.setCurrentItem(finalI));
                } else {
                    thumbView.setOnClickListener(v -> {
                        int position = thumbs.indexOf(v);
                        ImageModel model = imageModels.remove(position);
                        linearLayout.removeView(thumbs.get(position));
                        thumbs.remove(position);
                        if (deleteCallback != null) deleteCallback.deleteImage(model);
                    });
                }
                linearLayout.addView(thumbView);

                thumbs.add(thumbView);
            }
        }
    }

    public void updateImagesWithList(List<ImageModel> list) {
        imageModels = list;
        updateImageModels();
    }

    private class GalleryAdapter extends PagerAdapter {
        LayoutInflater inflater;

        private GalleryAdapter() {

        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return imageModels.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            View itemView = inflater.inflate(R.layout.content_image_gallery_item, container, false);
            container.addView(itemView);
            AVLoadingIndicatorView progress = (AVLoadingIndicatorView) itemView.findViewById(R.id.progress_indicator);
            AspectRatioImageView imageView =
                    (AspectRatioImageView) itemView.findViewById(R.id.image);
            imageView.setAspectRatioEnabled(true);
            imageView.setDominantMeasurement(AspectRatioImageView.MEASUREMENT_WIDTH);
            imageView.setAspectRatio(1);

            imageView.setOnClickListener(view -> {
                if (getActivity() instanceof MediaFullscreenProtocol) {
                    ImageView fullSizeImage = new ImageView(getActivity());
                    ((MediaFullscreenProtocol) getActivity()).showMediaFullscreen(fullSizeImage, null, false, true);
                    Glide
                            .with(getContext())
                            .load(imageModels.get(position).imageUrl)
                            .dontAnimate()
                            .dontTransform()
                            .fitCenter()
                            .into(fullSizeImage);
                }
            });

            progress.smoothToShow();
            Glide
                    .with(getContext())
                    .load(imageModels.get(position).imageUrl)
                    .asBitmap()
                    .skipMemoryCache(true)
                    .listener(new RequestListener<String, Bitmap>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                            progress.smoothToHide();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            imageView.setAspectRatioEnabled(false);
                            pager.requestLayout();
                            pager.invalidate();
                            progress.smoothToHide();
                            return false;
                        }
                    })
                    .dontAnimate()
                    .fitCenter()
                    .into(imageView);

            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            RelativeLayout itemView = (RelativeLayout) object;
            container.removeView(itemView);
        }
    }
}
