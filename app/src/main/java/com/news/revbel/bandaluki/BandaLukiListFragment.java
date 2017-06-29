package com.news.revbel.bandaluki;


import android.app.SearchManager;
import android.content.Context;
import android.content.res.Resources;
import android.databinding.Observable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.news.revbel.MainActivity;
import com.news.revbel.R;
import com.news.revbel.utilities.AutoResizeTextView;
import com.news.revbel.utilities.ControlActivityInterface;
import com.news.revbel.viewmodel.BanditPostModel;
import com.news.revbel.viewmodel.BanditsListViewModel;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.hdodenhof.circleimageview.CircleImageView;

public class BandaLukiListFragment extends Fragment {
    private Unbinder unbinder;
    private ControlActivityInterface activityInterface;

    @BindView(R.id.pager) ViewPager pager;
    @BindView(R.id.recycler_thumb) RecyclerView recyclerThumb;
    @BindView(R.id.search_result) TextView searchResults;
    @BindView(R.id.search_result_container) RelativeLayout searchContainer;
    @BindView(R.id.nothing_label) TextView nothingToShow;

    private MenuItem refreshItem;
    private BanditsListViewModel viewModel;
    private boolean isSearchOpened;
    private LayoutInflater inflater;

    Observable.OnPropertyChangedCallback disableRefreshCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable observable, int i) {
            boolean isLoading = viewModel.isLoading().get();
            updateRefreshButton(isLoading);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof ControlActivityInterface) {
             activityInterface = (ControlActivityInterface) getActivity();
             activityInterface.updateControlButtonTapEvent(this, () -> {
            });
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activityInterface != null) {
            activityInterface.onFragmentHide(this);
        }
    }

    public static BandaLukiListFragment newInstance() {
        BandaLukiListFragment fragment = new BandaLukiListFragment();
        return fragment;
    }

    public BandaLukiListFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;

        if (activityInterface != null) activityInterface.hideControlButton(this);
        View view = inflater.inflate(R.layout.fragment_banda_luki, container, false);
        unbinder = ButterKnife.bind(this, view);

        BanditsAdapter adapter = new BanditsAdapter();

        pager.setAdapter(adapter);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        Runnable updatePager = () -> {
            if (nothingToShow != null && viewModel != null && viewModel.getItems() != null && viewModel.getItems().size() == 0 && viewModel.isSearching()) {
                nothingToShow.setVisibility(View.VISIBLE);
            } else if (nothingToShow != null) {
                nothingToShow.setVisibility(View.GONE);
            }
            if (pager != null) {
                pager.invalidate();
                pager.getAdapter().notifyDataSetChanged();
            }
            if (recyclerThumb != null) {
                recyclerThumb.invalidate();
                recyclerThumb.getAdapter().notifyDataSetChanged();
            }
        };

        viewModel = new BanditsListViewModel(new BanditsListViewModel.BanditsViewModelCallback() {
            @Override
            public void onUpdated() {
                updatePager.run();
            }

            @Override
            public void onFinishUpdating() {
                updatePager.run();
                if (activityInterface != null) activityInterface.stopLoading();
            }
        });
        viewModel.isLoading().addOnPropertyChangedCallback(disableRefreshCallback);

        recyclerThumb.setAdapter(new BanditsThumbAdapter());
        recyclerThumb.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        viewModel.loadBandits(false);
        if (viewModel.getItems().size() == 0 && activityInterface != null) activityInterface.startLoading();

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        refreshItem = menu.findItem(R.id.refresh);
        if (refreshItem != null && refreshItem.getActionView().findViewById(R.id.refresh_image) == null) {
            View view = inflater.inflate(R.layout.content_refresh_button, null);
            refreshItem.setActionView(view);
            refreshItem.getActionView().setOnClickListener(refreshView -> viewModel.loadBandits(true));
        }
        updateRefreshButton(viewModel.isLoading().get());

        MenuItem searchMenuItem = menu.findItem(R.id.search);
        if (searchMenuItem != null) {
            SearchView searchView = (SearchView) searchMenuItem.getActionView();
            SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

            MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    isSearchOpened = false;
                    updatePager();

                    return true;
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    isSearchOpened = true;
                    updatePager();

                    return true;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (!searchView.isIconified()) {
                        searchView.setIconified(true);
                    }

                    searchMenuItem.collapseActionView();
                    updateSearchResults(query);

                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    viewModel.filterSearchResult(s);
                    return false;
                }
            });
        }
    }

    @OnClick(R.id.close_search)
    void onCloseSearchClicked() {
        updateSearchResults(null);
    }

    private void updateRefreshButton(boolean isRefreshing) {
        if (refreshItem != null && refreshItem.getActionView() != null) {
            if (isRefreshing) {
                Animation animation = new RotateAnimation(0.0f, 360.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                animation.setDuration(1000);
                animation.setRepeatCount(Animation.INFINITE);
                refreshItem.getActionView().findViewById(R.id.refresh_image).startAnimation(animation);
            } else {
                refreshItem.getActionView().findViewById(R.id.refresh_image).clearAnimation();
            }
            refreshItem.setEnabled(!isRefreshing);
        }
    }

    private void updateSearchResults(String s) {
        if (s != null && !s.isEmpty()) {
            searchContainer.setVisibility(View.VISIBLE);
            Resources res = getResources();
            String results = String.format(res.getString(R.string.bandaluki_search_result), s);
            searchResults.setText(results);
            viewModel.filterSearchResult(s);
        } else {
            searchContainer.setVisibility(View.GONE);
            searchResults.setText("");
            viewModel.filterSearchResult(null);
        }
    }

    private void updatePager() {
        if (viewModel.getItems().size() > 0) {
            for (int i = pager.getCurrentItem() - 1; i < pager.getCurrentItem() + 2; i++) {
                if (i >= 0 && i < viewModel.getItems().size()) {
                    BanditPostModel postModel = (BanditPostModel) viewModel.getItems().get(i);
                    View item = pager.findViewWithTag(postModel);
                    updateViewForPager(item);
                }
            }
        }
    }

    private void updateViewForPager(View itemView) {
        LinearLayout linearLayout = (LinearLayout) itemView.findViewById(R.id.photo_tags);
        linearLayout.setVisibility(isSearchOpened ? View.GONE : View.VISIBLE);
    }

    private void configureViewForPager(View itemView, BanditPostModel postModel) {
        itemView.setTag(postModel);
        updateViewForPager(itemView);

        ImageView imageView = (ImageView) itemView.findViewById(R.id.photo_image);
        if (postModel.featuredMediaUrl != null) {
            Glide
                    .with(getContext())
                    .load(postModel.featuredMediaUrl)
                    .placeholder(android.R.color.white)
                    .dontTransform()
                    .dontAnimate()
                    .into(imageView);
        }

        AutoResizeTextView textView = (AutoResizeTextView) itemView.findViewById(R.id.title_post);
        textView.setText(postModel.getTitleSpanned());

        TextView categoryTextView = (TextView) itemView.findViewById(R.id.city_textview);
        categoryTextView.setText(postModel.getCityName());

        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            itemView.setOnClickListener(view -> activity.openFullPost(postModel, false));
        }

        LinearLayout linearLayout = (LinearLayout) itemView.findViewById(R.id.photo_tags);
        for (BanditPostModel.TAG_TYPE tag : postModel.getTags()) {
            CircleImageView circleImageView = (CircleImageView) inflater.inflate(R.layout.content_bandaluki_tag, linearLayout, false);

            switch (tag) {
                case AUTO: circleImageView.setImageResource(R.drawable.bl_car_tag);
                    break;
                case ADDRESS: circleImageView.setImageResource(R.drawable.bl_location_tag);
                    break;
                case VIDEO: circleImageView.setImageResource(R.drawable.bl_video_tag);
                    break;
                case PHOTO: circleImageView.setImageResource(R.drawable.bl_photo_tag);
                    break;
                case PROFESSION: circleImageView.setImageResource(R.drawable.bl_profession_tag);
                    break;
                case TELEPHONE: circleImageView.setImageResource(R.drawable.bl_phone_tag);
                    break;
                case RELATIVES: circleImageView.setImageResource(R.drawable.bl_relatives_tag);
                    break;
                case SOCIAL: circleImageView.setImageResource(R.drawable.bl_socialnetwork_tag);
                    break;
            }
            if (tag != BanditPostModel.TAG_TYPE.UNDEFINED) linearLayout.addView(circleImageView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        if (refreshItem != null) refreshItem.getActionView().findViewById(R.id.refresh_image).clearAnimation();
        viewModel.isLoading().removeOnPropertyChangedCallback(disableRefreshCallback);
    }

    private class BanditsThumbAdapter extends RecyclerView.Adapter<BanditsThumbAdapter.ViewHolder> {
        class ViewHolder extends RecyclerView.ViewHolder {
            private CircleImageView photo;

            private ViewHolder(View v) {
                super(v);
                photo = (CircleImageView) v.findViewById(R.id.photo_thumb_circ);
            }
        }

        @Override
        public BanditsThumbAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.content_bandaluki_thumb, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BanditPostModel postModel = (BanditPostModel) viewModel.getItems().get(position);
            if (postModel.featuredMediaUrl != null) {
                Glide
                        .with(getContext())
                        .load(postModel.featuredMediaUrl)
                        .placeholder(android.R.color.white)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .dontAnimate()
                        .into(holder.photo);
            }
            holder.itemView.setOnClickListener(view -> pager.setCurrentItem(position, true));
        }

        @Override
        public int getItemCount() {
            return viewModel.getItems().size();
        }
    }

    private class BanditsAdapter extends PagerAdapter {

        private BanditsAdapter() {

        }

        @Override
        public int getCount() {
            if (viewModel != null) {
                return viewModel.getItems().size();
            } else {
                return 0;
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            View itemView = inflater.inflate(R.layout.content_bandaluki_item, container, false);
            container.addView(itemView);

            BanditPostModel postModel = (BanditPostModel) viewModel.getItems().get(position);
            configureViewForPager(itemView, postModel);

            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View itemView = (View) object;
            container.removeView(itemView);
        }
    }
}
