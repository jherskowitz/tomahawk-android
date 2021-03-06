/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.tomahawk_android.activities;

import com.rdio.android.api.OAuth1WebViewActivity;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.RdioAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.database.TomahawkSQLiteHelper;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptResolverUrlResult;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.SuggestionSimpleCursorAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkMenuAdapter;
import org.tomahawk.tomahawk_android.fragments.AlbumsFragment;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;
import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
import org.tomahawk.tomahawk_android.fragments.PlaylistEntriesFragment;
import org.tomahawk.tomahawk_android.fragments.SearchPagerFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.TracksFragment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.services.PlaybackService.PlaybackServiceConnection;
import org.tomahawk.tomahawk_android.services.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener;
import org.tomahawk.tomahawk_android.services.RemoteControllerService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.ThreadManager;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The main Tomahawk activity
 */
public class TomahawkMainActivity extends ActionBarActivity
        implements PlaybackServiceConnectionListener, SlidingUpPanelLayout.PanelSlideListener {

    private final static String TAG = TomahawkMainActivity.class.getSimpleName();

    public static final String SAVED_STATE_ACTION_BAR_HIDDEN = "saved_state_action_bar_hidden";

    public static final String PLAYBACKSERVICE_READY
            = "org.tomahawk.tomahawk_android.playbackservice_ready";

    public static final String SHOW_PLAYBACKFRAGMENT_ON_STARTUP
            = "org.tomahawk.tomahawk_android.show_playbackfragment_on_startup";

    public static final String SLIDING_LAYOUT_COLLAPSED
            = "org.tomahawk.tomahawk_android.sliding_layout_collapsed";

    public static final String SLIDING_LAYOUT_EXPANDED
            = "org.tomahawk.tomahawk_android.sliding_layout_expanded";

    private static long mSessionIdCounter = 0;

    protected HashSet<String> mCurrentRequestIds = new HashSet<String>();

    private PlaybackServiceConnection mPlaybackServiceConnection = new PlaybackServiceConnection(
            this);

    private PlaybackService mPlaybackService;

    private MenuItem mSearchItem;

    private DrawerLayout mDrawerLayout;

    private ListView mDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mTitle;

    private CharSequence mDrawerTitle;

    private TomahawkMainReceiver mTomahawkMainReceiver;

    private Drawable mProgressDrawable;

    private Handler mAnimationHandler;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;

    public static boolean sIsConnectedToWifi;

    // Used to display an animated progress drawable
    private Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            mProgressDrawable.setLevel(mProgressDrawable.getLevel() + 400);
            getSupportActionBar().setLogo(mProgressDrawable);
            mAnimationHandler.postDelayed(mAnimationRunnable, 50);
        }
    };

    private Handler mShouldShowAnimationHandler;

    private Runnable mShouldShowAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            mAnimationHandler.removeCallbacks(mAnimationRunnable);
            if (ThreadManager.getInstance().isActive()
                    || (mPlaybackService != null && mPlaybackService.isPreparing())) {
                mAnimationHandler.post(mAnimationRunnable);
            } else {
                getSupportActionBar().setLogo(R.drawable.ic_launcher);
            }
            mShouldShowAnimationHandler.postDelayed(mShouldShowAnimationRunnable, 500);
        }
    };

    /**
     * Handles incoming broadcasts.
     */
    private class TomahawkMainReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean noConnectivity =
                        intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (!noConnectivity) {
                    AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                            .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                    InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
                }
                ConnectivityManager connMgr = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                sIsConnectedToWifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI) != null
                        && connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
            } else if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
                String requestId = intent.getStringExtra(
                        InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
                if (mCurrentRequestIds.contains(requestId)) {
                    InfoRequestData data = InfoSystem.getInstance().getInfoRequestById(requestId);
                    if (data != null
                            && data.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SELF) {
                        User user = data.getResult(User.class);
                        HatchetAuthenticatorUtils authenticatorUtils
                                = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                        authenticatorUtils.setLoggedInUser(user);
                        updateDrawer();
                    }
                }
            } else if (AuthenticatorManager.CONFIG_TEST_RESULT
                    .equals(intent.getAction())) {
                String authenticatorId = intent
                        .getStringExtra(AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINNAME);
                final int type = intent
                        .getIntExtra(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE, 0);
                if (TomahawkApp.PLUGINNAME_HATCHET.equals(authenticatorId)
                        && (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS
                        || type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT)) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onHatchetLoggedInOut(type
                                    == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS);
                        }
                    });
                }
            } else if (PipeLine.PIPELINE_URLLOOKUPFINISHED.equals(intent.getAction())) {
                String url = intent.getStringExtra(PipeLine.PIPELINE_URLLOOKUPFINISHED_URL);
                Resolver resolver = PipeLine.getInstance().getResolver(
                        intent.getStringExtra(PipeLine.PIPELINE_URLLOOKUPFINISHED_RESOLVERID));
                ScriptResolverUrlResult result = PipeLine.getInstance().getUrlResult(url);
                if (result.type.equals(PipeLine.URL_TYPE_ARTIST)) {
                    FragmentUtils.replace(TomahawkMainActivity.this, getSupportFragmentManager(),
                            AlbumsFragment.class, Artist.get(result.name).getCacheKey(),
                            TomahawkFragment.TOMAHAWK_ARTIST_KEY);
                } else if (result.type.equals(PipeLine.URL_TYPE_ALBUM)) {
                    Artist artist = Artist.get(result.artist);
                    FragmentUtils.replace(TomahawkMainActivity.this, getSupportFragmentManager(),
                            TracksFragment.class, Album.get(result.name, artist).getCacheKey(),
                            TomahawkFragment.TOMAHAWK_ALBUM_KEY);
                } else if (result.type.equals(PipeLine.URL_TYPE_TRACK)) {
                    FragmentUtils.replace(TomahawkMainActivity.this, getSupportFragmentManager(),
                            TracksFragment.class,
                            Query.get(result.title, "", result.artist, false).getCacheKey(),
                            TomahawkFragment.TOMAHAWK_QUERY_KEY);
                } else if (result.type.equals(PipeLine.URL_TYPE_PLAYLIST)) {
                    ArrayList<Query> queries = new ArrayList<Query>();
                    for (ScriptResolverUrlResult track : result.tracks) {
                        Query query = Query.get(track.title, "", track.artist, false);
                        if (resolver != null && resolver.isEnabled() && track.hint != null) {
                            query.addTrackResult(Result.get(track.hint, query.getBasicTrack(),
                                    resolver, query.getCacheKey()));
                        }
                        queries.add(query);
                    }
                    Playlist playlist = Playlist.fromQueryList(result.guid, queries);
                    FragmentUtils.replace(TomahawkMainActivity.this, getSupportFragmentManager(),
                            PlaylistEntriesFragment.class, playlist.getId(),
                            TomahawkFragment.TOMAHAWK_PLAYLIST_KEY);
                }
            }
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        /**
         * Called every time an item inside the {@link android.widget.ListView} is clicked
         *
         * @param parent   The AdapterView where the click happened.
         * @param view     The view within the AdapterView that was clicked (this will be a view
         *                 provided by the adapter)
         * @param position The position of the view in the adapter.
         * @param id       The row id of the item that was clicked.
         */
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HatchetAuthenticatorUtils authenticatorUtils
                    = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            // Show the correct hub, and if needed, display the search editText inside the ActionBar
            FragmentUtils.showHub(TomahawkMainActivity.this, getSupportFragmentManager(),
                    (int) id, authenticatorUtils.getLoggedInUser());
            if (mDrawerLayout != null) {
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DatabaseHelper.getInstance().ensureInit();
        PipeLine.getInstance().ensureInit();
        InfoSystem.getInstance().ensureInit();
        AuthenticatorManager.getInstance().ensureInit();
        CollectionManager.getInstance().ensureInit();

        //Setup our services
        Intent intent = new Intent(this, PlaybackService.class);
        startService(intent);
        bindService(intent, mPlaybackServiceConnection, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.tomahawk_main_activity);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mProgressDrawable = getResources()
                .getDrawable(R.drawable.tomahawk_progress_indeterminate_circular_holo_light);

        mTitle = mDrawerTitle = getTitle();
        getSupportActionBar().setTitle("");

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingUpPanelLayout.setPanelSlideListener(this);

        if (mDrawerLayout != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer,
                    R.string.drawer_open, R.string.drawer_close) {

                /** Called when a drawer has settled in a completely closed state. */
                public void onDrawerClosed(View view) {
                    getSupportActionBar().setTitle(mTitle);
                }

                /** Called when a drawer has settled in a completely open state. */
                public void onDrawerOpened(View drawerView) {
                    getSupportActionBar().setTitle(mDrawerTitle);
                    if (mSearchItem != null) {
                        MenuItemCompat.collapseActionView(mSearchItem);
                    }
                }
            };
            // Set the drawer toggle as the DrawerListener
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        // set customization variables on the ActionBar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.playback_fragment_frame,
                    Fragment.instantiate(this, PlaybackFragment.class.getName(), null),
                    null).commit();
            FragmentUtils.addRootFragment(TomahawkMainActivity.this, getSupportFragmentManager());
        } else {
            boolean actionBarHidden = savedInstanceState
                    .getBoolean(SAVED_STATE_ACTION_BAR_HIDDEN, false);
            if (actionBarHidden) {
                getSupportActionBar().hide();
            }
        }

        // Set default preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains(
                FakePreferenceFragment.FAKEPREFERENCEFRAGMENT_KEY_SCROBBLEEVERYTHING)) {
            preferences.edit().putBoolean(
                    FakePreferenceFragment.FAKEPREFERENCEFRAGMENT_KEY_SCROBBLEEVERYTHING, true)
                    .commit();
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (SHOW_PLAYBACKFRAGMENT_ON_STARTUP.equals(intent.getAction())) {
            // if this Activity is being shown after the user clicked the notification
            FragmentUtils.showHub(TomahawkMainActivity.this, getSupportFragmentManager(),
                    FragmentUtils.HUB_ID_PLAYBACK);
        }
        if (intent.hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)) {
            FragmentUtils.replace(this, getSupportFragmentManager(), FakePreferenceFragment.class);
        }

        if (intent.getData() != null) {
            Uri data = intent.getData();
            List<String> pathSegments = data.getPathSegments();
            String host = data.getHost();
            if (host.contains("hatchet.is") || host.contains("toma.hk") || host.contains("spotify")
                    || host.contains("tomahawk") || host.contains("beatsmusic.com")
                    || host.contains("deezer.com") || host.contains("rdio.com")
                    || host.contains("soundcloud.com")) {
                PipeLine.getInstance().lookupUrl(data.toString());
            } else {
                String albumName = null;
                String trackName = null;
                String artistName = null;
                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(this, data);
                    albumName =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    artistName =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    trackName =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    retriever.release();
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "handleIntent: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
                if (TextUtils.isEmpty(trackName)) {
                    trackName = pathSegments.get(pathSegments.size() - 1);
                }
                Query query = Query.get(trackName, albumName, artistName, false);
                Resolver resolver =
                        PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_USERCOLLECTION);
                if (resolver != null) {
                    query.addTrackResult(Result.get(data.toString(), query.getBasicTrack(),
                            resolver, query.getCacheKey()));
                    FragmentUtils.replace(TomahawkMainActivity.this, getSupportFragmentManager(),
                            TracksFragment.class, query.getCacheKey(),
                            TomahawkFragment.TOMAHAWK_QUERY_KEY);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateDrawer();

        mAnimationHandler = new Handler();
        mShouldShowAnimationHandler = new Handler();
        mShouldShowAnimationHandler.post(mShouldShowAnimationRunnable);

        if (mTomahawkMainReceiver == null) {
            mTomahawkMainReceiver = new TomahawkMainReceiver();
        }

        // Register intents that the BroadcastReceiver should listen to
        registerReceiver(mTomahawkMainReceiver,
                new IntentFilter(PlaybackService.BROADCAST_CURRENTTRACKCHANGED));
        registerReceiver(mTomahawkMainReceiver,
                new IntentFilter(PlaybackService.BROADCAST_PLAYSTATECHANGED));
        registerReceiver(mTomahawkMainReceiver,
                new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED));
        registerReceiver(mTomahawkMainReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(mTomahawkMainReceiver,
                new IntentFilter(AuthenticatorManager.CONFIG_TEST_RESULT));
        registerReceiver(mTomahawkMainReceiver,
                new IntentFilter(PipeLine.PIPELINE_URLLOOKUPFINISHED));
    }

    @Override
    public void onPause() {
        super.onPause();

        mAnimationHandler.removeCallbacks(mAnimationRunnable);
        mShouldShowAnimationHandler.removeCallbacks(mShouldShowAnimationRunnable);
        mAnimationHandler = null;
        mShouldShowAnimationHandler = null;

        if (mTomahawkMainReceiver != null) {
            unregisterReceiver(mTomahawkMainReceiver);
            mTomahawkMainReceiver = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVED_STATE_ACTION_BAR_HIDDEN, !getSupportActionBar().isShowing());
    }

    @Override
    public void onDestroy() {
        if (mPlaybackService != null) {
            unbindService(mPlaybackServiceConnection);
        }

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            RdioAuthenticatorUtils authUtils = (RdioAuthenticatorUtils) AuthenticatorManager
                    .getInstance().getAuthenticatorUtils(TomahawkApp.PLUGINNAME_RDIO);
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Rdio access token is served and yummy");
                if (data != null) {
                    String accessToken = data.getStringExtra(OAuth1WebViewActivity.EXTRA_TOKEN);
                    String accessTokenSecret =
                            data.getStringExtra(OAuth1WebViewActivity.EXTRA_TOKEN_SECRET);
                    authUtils.onRdioAuthorised(accessToken, accessTokenSecret);
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (data != null) {
                    String errorCode = data.getStringExtra(OAuth1WebViewActivity.EXTRA_ERROR_CODE);
                    String errorDescription = data
                            .getStringExtra(OAuth1WebViewActivity.EXTRA_ERROR_DESCRIPTION);
                    authUtils.onLoginFailed(
                            AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                            "Rdio authentication cancelled");
                    Log.e(TAG, "ERROR: " + errorCode + " - " + errorDescription);
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.tomahawk_main_menu, menu);
        // customize the searchView
        mSearchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        SearchView.SearchAutoComplete searchAutoComplete
                = (SearchView.SearchAutoComplete) searchView
                .findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setDropDownBackgroundResource(R.drawable.menu_dropdown_panel_tomahawk);
        View searchEditText = searchView
                .findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchEditText.setBackgroundResource(R.drawable.tomahawk_textfield_activated_holo_light);
        searchView.setQueryHint(getString(R.string.searchfragment_title_string));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !TextUtils.isEmpty(query)) {
                    DatabaseHelper.getInstance().addEntryToSearchHistory(query);
                    FragmentUtils.replace(TomahawkMainActivity.this, getSupportFragmentManager(),
                            SearchPagerFragment.class, query);
                    if (mSearchItem != null) {
                        MenuItemCompat.collapseActionView(mSearchItem);
                    }
                    searchView.clearFocus();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Cursor cursor = DatabaseHelper.getInstance().getSearchHistoryCursor(newText);
                if (cursor.getCount() != 0) {
                    String[] columns = new String[]{
                            TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY};
                    int[] columnTextId = new int[]{android.R.id.text1};

                    SuggestionSimpleCursorAdapter simple = new SuggestionSimpleCursorAdapter(
                            getBaseContext(), R.layout.searchview_dropdown_item,
                            cursor, columns, columnTextId, 0);

                    if (searchView.getSuggestionsAdapter() != null
                            && searchView.getSuggestionsAdapter().getCursor() != null) {
                        searchView.getSuggestionsAdapter().getCursor().close();
                    }
                    searchView.setSuggestionsAdapter(simple);
                    return true;
                } else {
                    cursor.close();
                    return false;
                }
            }
        });
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                SQLiteCursor cursor = (SQLiteCursor) searchView.getSuggestionsAdapter()
                        .getItem(position);
                int indexColumnSuggestion = cursor
                        .getColumnIndex(TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY);

                searchView.setQuery(cursor.getString(indexColumnSuggestion), false);

                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                SQLiteCursor cursor = (SQLiteCursor) searchView.getSuggestionsAdapter()
                        .getItem(position);
                int indexColumnSuggestion = cursor
                        .getColumnIndex(TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY);

                searchView.setQuery(cursor.getString(indexColumnSuggestion), false);

                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item) ||
                super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    @Override
    public void onPlaybackServiceReady() {
        sendBroadcast(new Intent(PLAYBACKSERVICE_READY));
    }

    @Override
    public void setPlaybackService(PlaybackService ps) {
        mPlaybackService = ps;
    }

    public PlaybackService getPlaybackService() {
        return mPlaybackService;
    }

    public void onHatchetLoggedInOut(boolean loggedIn) {
        if (loggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            RemoteControllerService.attemptAskAccess();
        }
        updateDrawer();
    }

    public void updateDrawer() {
        HatchetAuthenticatorUtils authenticatorUtils
                = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        if (authenticatorUtils.getLoggedInUser() == null) {
            mCurrentRequestIds.add(InfoSystem.getInstance()
                    .resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SELF, null));
        }
        // Set up the TomahawkMenuAdapter. Give it its set of menu item texts and icons to display
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        TomahawkMenuAdapter slideMenuAdapter = new TomahawkMenuAdapter(this,
                getResources().getStringArray(R.array.slide_menu_items),
                getResources().obtainTypedArray(R.array.slide_menu_items_icons));
        slideMenuAdapter.setShowHatchetMenu(true);
        slideMenuAdapter.setUser(authenticatorUtils.getLoggedInUser());
        mDrawerList.setAdapter(slideMenuAdapter);

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    public void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    @Override
    public void onBackPressed() {
        View contextMenu = mSlidingUpPanelLayout.findViewById(R.id.context_menu_framelayout);
        if (contextMenu == null && mSlidingUpPanelLayout.isEnabled()
                && (mSlidingUpPanelLayout.isPanelExpanded()
                || mSlidingUpPanelLayout.isPanelAnchored())) {
            mSlidingUpPanelLayout.collapsePanel();
        } else {
            super.onBackPressed();
        }
    }

    public static long getSessionUniqueId() {
        return mSessionIdCounter++;
    }

    public static String getSessionUniqueStringId() {
        return String.valueOf(getSessionUniqueId());
    }

    public static String getLifetimeUniqueStringId() {
        return String.valueOf(System.currentTimeMillis()) + getSessionUniqueStringId();
    }

    @Override
    public void onPanelSlide(View view, float v) {
        if (v > 0.5f && getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
        } else if (v < 0.5f && !getSupportActionBar().isShowing()) {
            getSupportActionBar().show();
        }
    }

    @Override
    public void onPanelCollapsed(View view) {
        getSupportActionBar().show();
        TomahawkApp.getContext().sendBroadcast(new Intent(SLIDING_LAYOUT_COLLAPSED));
    }

    @Override
    public void onPanelExpanded(View view) {
        TomahawkApp.getContext().sendBroadcast(new Intent(SLIDING_LAYOUT_EXPANDED));
    }

    @Override
    public void onPanelAnchored(View view) {
    }

    @Override
    public void onPanelHidden(View view) {
    }

    public void collapsePanel() {
        mSlidingUpPanelLayout.collapsePanel();
    }

    public void showPanel() {
        mSlidingUpPanelLayout.showPanel();
    }
}
