/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.tomahawk_android.adapters;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link PagerAdapter} which provides functionality to swipe an AlbumArt image. Used in {@link
 * org.tomahawk.tomahawk_android.fragments.PlaybackFragment}
 */
public class AlbumArtSwipeAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {

    //Used to provide fake infinite swiping behaviour, if current Playlist is repeating
    private static final int FAKE_INFINITY_COUNT = 20000;

    private TomahawkMainActivity mActivity;

    private LayoutInflater mLayoutInflater;

    private FragmentManager mFragmentManager;

    private int mFakeInfinityOffset;

    private boolean mByUser;

    private boolean mSwiped;

    private ViewPager mViewPager;

    private MultiColumnClickListener mClickListener;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;

    private PlaybackService mPlaybackService;

    private int mCurrentViewPage = 0;

    /**
     * Constructs a new AlbumArtSwipeAdapter.
     *
     * @param viewPager ViewPager which this adapter has been connected with
     */
    public AlbumArtSwipeAdapter(TomahawkMainActivity activity, FragmentManager fragmentManager,
            LayoutInflater layoutInflater, ViewPager viewPager,
            MultiColumnClickListener clickListener, SlidingUpPanelLayout slidingUpPanelLayout) {
        mActivity = activity;
        mLayoutInflater = layoutInflater;
        mFragmentManager = fragmentManager;
        mViewPager = viewPager;
        mClickListener = clickListener;
        mSlidingUpPanelLayout = slidingUpPanelLayout;
        mByUser = true;
        mSwiped = false;
    }

    /**
     * Instantiate an item in the {@link PagerAdapter}. Fill it async with the correct AlbumArt
     * image.
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mLayoutInflater.inflate(
                org.tomahawk.tomahawk_android.R.layout.album_art_view_pager_item, container, false);
        if (mPlaybackService != null && mPlaybackService.getPlaylist().size() > 0) {
            if (mPlaybackService.isRepeating()) {
                position = position % mPlaybackService.getPlaylist().size();
            }
            Query query = mPlaybackService.getPlaylist().getEntries().get(position).getQuery();
            refreshTrackInfo(view, query);
        } else {
            refreshTrackInfo(view, null);
        }
        if (view != null) {
            container.addView(view);
        }
        return view;
    }

    /**
     * @return If current {@link Playlist} is empty or null, return 1. If current {@link Playlist}
     * is repeating, return FAKE_INFINITY_COUNT. Else return the current {@link Playlist}'s length.
     */
    @Override
    public int getCount() {
        if (mPlaybackService == null || mPlaybackService.getPlaylist().size() == 0) {
            return 1;
        }
        if (mPlaybackService.isRepeating()) {
            return FAKE_INFINITY_COUNT;
        }
        return mPlaybackService.getPlaylist().size();
    }

    /**
     * Remove the given {@link View} from the {@link ViewPager}
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    /**
     * @return true if view is equal to object
     */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    /**
     * Dummy method
     *
     * @return always null
     */
    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    /**
     * Is being called, whenever a new Page in our {@link AlbumArtSwipeAdapter} has been selected/
     * swiped to
     */
    @Override
    public void onPageSelected(int position) {
        if (mPlaybackService != null && isByUser()) {
            setSwiped(true);
            if (position == mCurrentViewPage - 1) {
                mPlaybackService.previous();
            } else if (position == mCurrentViewPage + 1) {
                mPlaybackService.next();
            }
        }
        mCurrentViewPage = position;
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    /**
     * @param entry        to set the current item to
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    public void setCurrentItem(PlaylistEntry entry, boolean smoothScroll) {
        int position = mPlaybackService.getPlaylist().getIndexOfEntry(entry);
        if (mPlaybackService.isRepeating()) {
            position += mFakeInfinityOffset;
        }
        if (position != mViewPager.getCurrentItem()) {
            if (mPlaybackService.isRepeating()) {
                if (position
                        == (mCurrentViewPage % mPlaybackService.getPlaylist().size()) + 1
                        || ((mCurrentViewPage % mPlaybackService.getPlaylist().size())
                        == mPlaybackService.getPlaylist().size() - 1 && position == 0)) {
                    setCurrentToNextItem(smoothScroll);
                } else if (position
                        == (mCurrentViewPage % mPlaybackService.getPlaylist().size()) - 1
                        || ((mCurrentViewPage % mPlaybackService.getPlaylist().size()) == 0
                        && position == mPlaybackService.getPlaylist().size() - 1)) {
                    setCurrentToPreviousItem(smoothScroll);
                } else {
                    mViewPager.setCurrentItem(position, false);
                }
            } else {
                mViewPager.setCurrentItem(position, smoothScroll);
            }
            mCurrentViewPage = mViewPager.getCurrentItem();
        }
    }

    /**
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    public void setCurrentToNextItem(boolean smoothScroll) {
        mViewPager.setCurrentItem(mCurrentViewPage + 1, smoothScroll);
    }

    /**
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    public void setCurrentToPreviousItem(boolean smoothScroll) {
        mViewPager.setCurrentItem(mCurrentViewPage - 1, smoothScroll);
    }

    /**
     * update the {@link Playlist} of the {@link AlbumArtSwipeAdapter} to the given {@link
     * Playlist}
     */
    public void updatePlaylist() {
        if (mPlaybackService != null) {
            Playlist playlist = mPlaybackService.getPlaylist();
            int size = playlist.size();
            notifyDataSetChanged();
            if (size > 0) {
                mFakeInfinityOffset = size * ((FAKE_INFINITY_COUNT / 2) / size);
                setByUser(false);
                setCurrentItem(mPlaybackService.getCurrentEntry(), false);
                setByUser(true);
            }
        }
    }

    /**
     * @return whether or not previous swipe was done by user
     */
    public boolean isByUser() {
        return mByUser;
    }

    /**
     * Set whether or not previous swipe was done by user
     */
    public void setByUser(boolean byUser) {
        this.mByUser = byUser;
    }

    /**
     * @return whether or not previous skipping to next/previous {@link org.tomahawk.libtomahawk.collection.Track}
     * was induced by swiping
     */
    public boolean isSwiped() {
        return mSwiped;
    }

    /**
     * Set whether or not previous skipping to next/previous {@link org.tomahawk.libtomahawk.collection.Track}
     * was induced by swiping
     */
    public void setSwiped(boolean isSwiped) {
        this.mSwiped = isSwiped;
    }

    /**
     * Set this {@link AlbumArtSwipeAdapter}'s {@link PlaybackService} reference
     */
    public void setPlaybackService(PlaybackService mPlaybackService) {
        this.mPlaybackService = mPlaybackService;
        updatePlaylist();
    }

    private void refreshTrackInfo(View view, final Query query) {
        TextView artistTextView = (TextView) view.findViewById(R.id.now_playing_artist);
        TextView titleTextView = (TextView) view.findViewById(R.id.now_playing_title);
        ImageButton playPauseButton = (ImageButton) view.findViewById(R.id.now_playing_button1);
        ImageButton loveButton = (ImageButton) view.findViewById(R.id.now_playing_button2);
        ImageButton returnButton = (ImageButton) view.findViewById(R.id.now_playing_button3);
        ImageView imageView = (ImageView) view.findViewById(R.id.album_art_image);
        ImageView nowPlayingAlbumArt = (ImageView) view.findViewById(R.id.now_playing_album_art);
        if (query != null) {
            mSlidingUpPanelLayout.showPanel();
            boolean landscapeMode = mActivity.getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;
            TomahawkUtils.loadImageIntoImageView(mActivity, imageView, query.getImage(),
                    Image.getLargeImageSize(), landscapeMode);
            TomahawkUtils.loadImageIntoImageView(mActivity, nowPlayingAlbumArt, query.getImage(),
                    Image.getSmallImageSize());
            if (!mSlidingUpPanelLayout.isPanelExpanded()) {
                playPauseButton.setEnabled(true);
                playPauseButton.setVisibility(View.VISIBLE);
                if (mPlaybackService.isPlaying()) {
                    TomahawkUtils.loadDrawableIntoImageView(mActivity, playPauseButton,
                            R.drawable.ic_player_pause);
                } else {
                    TomahawkUtils.loadDrawableIntoImageView(mActivity, playPauseButton,
                            R.drawable.ic_player_play);
                }
                playPauseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPlaybackService.playPause();
                    }
                });
            } else {
                playPauseButton.setEnabled(false);
                playPauseButton.setVisibility(View.GONE);
            }

            // Update all relevant TextViews
            if (artistTextView != null) {
                if (query.getArtist() != null && query.getArtist().getName() != null) {
                    artistTextView.setText(query.getArtist().toString());
                } else {
                    artistTextView.setText(R.string.playbackactivity_unknown_string);
                }
            }
            if (titleTextView != null) {
                if (query.getName() != null) {
                    titleTextView.setText(query.getName());
                } else {
                    titleTextView.setText(R.string.playbackactivity_unknown_string);
                }
            }
            if (loveButton != null) {
                if (DatabaseHelper.getInstance().isItemLoved(query)) {
                    loveButton.setImageResource(R.drawable.ic_action_loved);
                } else {
                    loveButton.setImageResource(R.drawable.ic_action_notloved);
                }
                loveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CollectionManager.getInstance().toggleLovedItem(query);
                        mActivity.sendBroadcast(
                                new Intent(PlaybackService.BROADCAST_CURRENTTRACKCHANGED));
                    }
                });
            }
            returnButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSlidingUpPanelLayout.isPanelExpanded()) {
                        mSlidingUpPanelLayout.collapsePanel();
                    }
                    FragmentUtils.replace(mActivity, mFragmentManager,
                            mPlaybackService.getReturnFragmentClass(),
                            mPlaybackService.getReturnFragmentArgs());
                }
            });
            imageView.setOnLongClickListener(new ClickListener(query, mClickListener));
        } else {
            mSlidingUpPanelLayout.hidePanel();
            //No track has been given, so we update the view state accordingly
            // Update all relevant TextViews

            if (artistTextView != null) {
                artistTextView.setText("");
            }
            if (titleTextView != null) {
                titleTextView.setText(R.string.playbackactivity_no_track);
            }
            if (loveButton != null) {
                loveButton.setImageResource(R.drawable.ic_action_notloved);
            }
        }
    }
}
