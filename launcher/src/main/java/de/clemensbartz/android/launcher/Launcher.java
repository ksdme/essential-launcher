/*
 * Copyright (C) 2018  Clemens Bartz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.clemensbartz.android.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.clemensbartz.android.launcher.adapters.DrawerListAdapter;
import de.clemensbartz.android.launcher.controllers.DockController;
import de.clemensbartz.android.launcher.controllers.DrawerController;
import de.clemensbartz.android.launcher.controllers.ViewController;
import de.clemensbartz.android.launcher.controllers.WidgetController;
import de.clemensbartz.android.launcher.daos.SharedPreferencesDAO;
import de.clemensbartz.android.launcher.listeners.AbsListViewOnCreateContextMenuListener;
import de.clemensbartz.android.launcher.listeners.AdapterViewOnItemClickListener;
import de.clemensbartz.android.launcher.listeners.SearchViewOnActionExpandListener;
import de.clemensbartz.android.launcher.receivers.PackageChangedBroadcastReceiver;
import de.clemensbartz.android.launcher.tasks.FilterDrawerListAdapterTask;
import de.clemensbartz.android.launcher.tasks.LoadDockTask;
import de.clemensbartz.android.launcher.tasks.LoadDrawerListAdapterTask;
import de.clemensbartz.android.launcher.tasks.LoadSharedPreferencesDAOTask;
import de.clemensbartz.android.launcher.util.IntentUtil;
import de.clemensbartz.android.launcher.util.StrictModeUtil;
import de.clemensbartz.android.launcher.util.ThemeUtil;

/**
 * Launcher class.
 *
 * @author Clemens Bartz
 * @since 1.0
 */
public final class Launcher extends Activity {

    /** The shared preferences dao. */
    @Nullable
    private SharedPreferencesDAO sharedPreferencesDAO = null;

    /** The controller for handling dock items. */
    @Nullable
    private DockController dockController = null;
    /** The controller for the drawer. */
    @Nullable
    private DrawerController drawerController = null;
    /** The controller for switching between views. */
    @Nullable
    private ViewController viewController = null;
    /** The controller for widget handling. */
    @Nullable
    private WidgetController widgetController = null;
    /** The adapter for applications. */
    @Nullable
    private DrawerListAdapter drawerListAdapter = null;

    /** The action bar menu. */
    private Menu actionBarMenu = null;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        // Set theme
        ThemeUtil.setTheme(this);
        // Create activity
        super.onCreate(savedInstanceState);
        // Set layout
        setContentView(R.layout.launcher);
        // Adjust strict mode
        StrictModeUtil.adjustStrictMode();

        // Create shared preference DAO
        sharedPreferencesDAO = SharedPreferencesDAO.getInstance(getPreferences(Context.MODE_PRIVATE));

        // Set up view handling
        viewController = new ViewController((ViewFlipper) findViewById(R.id.vsLauncher));

        // Set up widget handling
        final boolean supportingWidgets = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 || getPackageManager().hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS);
        if (supportingWidgets) {
            widgetController = new WidgetController(this, sharedPreferencesDAO);
            widgetController.startListening();
        }

        // Load the default drawable
        final Drawable dockDefault;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dockDefault = getDrawable(R.drawable.ic_round);
        } else {
            dockDefault = getResources().getDrawable(R.drawable.ic_round);
        }

        if (dockDefault == null) {
            throw new NullPointerException("Could not load ic_launcher drawable.");
        }

        // Set up dock handling
        final ArrayList<ImageView> dockImageViews = new ArrayList<>(DockController.NUMBER_OF_ITEMS);
        dockImageViews.add((ImageView) findViewById(R.id.ivDock1));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock2));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock3));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock4));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock5));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock6));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock7));
        dockController = new DockController(this, getPackageManager(), sharedPreferencesDAO, dockDefault, dockImageViews);

        // Create and assign adapter to views
        drawerListAdapter = new DrawerListAdapter(this, dockDefault);
        // Create and assign the drawer controller
        drawerController = new DrawerController(drawerListAdapter, sharedPreferencesDAO);

        // Get all detail content views
        final List<AbsListView> listViews = Arrays.asList(
                (AbsListView) findViewById(R.id.gvApplications),
                (AbsListView) findViewById(R.id.lvApplications)
        );
        // Assign adapter
        for (final AbsListView listView : listViews) {
            final int topPx = 0;

            final ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                final ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                marginLayoutParams.setMargins(
                        marginLayoutParams.leftMargin,
                        topPx + marginLayoutParams.topMargin,
                        marginLayoutParams.rightMargin,
                        marginLayoutParams.bottomMargin
                );
            } else {
                listView.setPadding(
                        listView.getPaddingLeft(),
                        topPx + listView.getPaddingTop(),
                        listView.getPaddingRight(),
                        listView.getPaddingBottom()
                );
            }

            registerForContextMenu(listView);
            listView.setAdapter(drawerListAdapter);
            listView.setOnItemClickListener(new AdapterViewOnItemClickListener(this));
            listView.setOnCreateContextMenuListener(new AbsListViewOnCreateContextMenuListener(getPackageManager(), drawerController, drawerListAdapter, dockController, this));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize DAOs
        new LoadSharedPreferencesDAOTask(this, sharedPreferencesDAO, viewController, widgetController).execute();

        // Register receivers
        final PackageChangedBroadcastReceiver receiver = PackageChangedBroadcastReceiver.getInstance();
        receiver.setDockController(dockController);
        receiver.setDrawerController(drawerController);
        receiver.setDrawerListAdapter(drawerListAdapter);
        receiver.setSharedPreferencesDAO(sharedPreferencesDAO);

        registerReceiver(receiver, IntentUtil.createdChangeBroadReceiverFilter());

        // Update drawer
        if (LoadDrawerListAdapterTask.getRunningTask() != null) {
            LoadDrawerListAdapterTask.getRunningTask().cancel(true);
        }

        final LoadDrawerListAdapterTask loadDrawerListAdapterTask = new LoadDrawerListAdapterTask(this, drawerController, drawerListAdapter);
        LoadDrawerListAdapterTask.setRunningTask(loadDrawerListAdapterTask);
        loadDrawerListAdapterTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    public void onBackPressed() {
        if (viewController != null) {
            viewController.showHome();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (viewController != null) {
            viewController.setActionBar(getActionBar());
            viewController.showHome();
        }

        if (LoadDockTask.getRunningTask() != null) {
            LoadDockTask.getRunningTask().cancel(true);
        }

        final LoadDockTask loadDockTask = new LoadDockTask(sharedPreferencesDAO, dockController);
        LoadDockTask.setRunningTask(loadDockTask);
        loadDockTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    protected void onDestroy() {
        if (widgetController != null) {
            widgetController.stopListening();
        }

        // Prevent leakage
        try {
            unregisterReceiver(PackageChangedBroadcastReceiver.getInstance());
        } catch (final Exception e) {
            // do nothing here
        }

        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(@NonNull final MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                if (viewController != null) {
                    viewController.showDetail();
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onActivityResult(
            final int requestCode,
            final int resultCode,
            @NonNull final Intent data) {

        // Only widget controller can handle these requests.
        if (widgetController != null) {
            widgetController.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onContextItemSelected(@Nullable final MenuItem item) {
        if (item != null && item.getIntent() != null && IntentUtil.isCallable(getPackageManager(), item.getIntent())) {
            startActivity(item.getIntent());

            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(@Nullable final Menu menu) {
        // Check for valid menu
        if (menu == null) {
            return false;
        }

        actionBarMenu = menu;

        if (viewController != null) {
            viewController.setActionBarMenu(actionBarMenu);
        }

        // Inflate menu
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.actionbar_options_menu, menu);

        // Check for widgets and disable button accordingly
        if (widgetController == null) {
            menu.findItem(R.id.abm_choose_widget).setVisible(false);
            menu.findItem(R.id.abm_layout_widget).setVisible(false);
            menu.findItem(R.id.abm_remove_widget).setVisible(false);
        } else {
            final boolean widgetConfigured = widgetController.isAppWidgetConfigured();

            menu.findItem(R.id.abm_layout_widget).setVisible(widgetConfigured);
            menu.findItem(R.id.abm_remove_widget).setVisible(widgetConfigured);
        }

        // Assign text watcher to search field
        final MenuItem abmSearchViewMenuItem = menu.findItem(R.id.app_bar_search);
        final View view = abmSearchViewMenuItem.getActionView();

        if (view instanceof SearchView) {
            final SearchView searchView = (SearchView) view;
            searchView.setOnQueryTextListener(drawerListAdapter);
            abmSearchViewMenuItem.setOnActionExpandListener(new SearchViewOnActionExpandListener());
        }

        // Check for hiding apps
        if (drawerListAdapter != null) {
            menu.findItem(R.id.abm_show_hidden).setChecked(drawerListAdapter.isShowingHiddenApps());
        }
        // Check for grid
        if (viewController != null) {
            menu.findItem(R.id.abm_grid_toggle).setChecked(viewController.getCurrentDetailIndex() == ViewController.GRID_ID);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@Nullable final MenuItem item) {
        // Check for non-existing item
        if (item == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.abm_choose_widget:
                if (widgetController == null) {
                    // something wrong here, item should not be visible... hiding
                    item.setVisible(false);

                    return true;
                }

                widgetController.requestWidgetChoosing();

                if (viewController == null) {
                    return super.onOptionsItemSelected(item);
                }

                viewController.showHome();

                return true;
            case R.id.abm_layout_widget:
                if (widgetController == null) {
                    // something wrong here, item should not be visible... hiding
                    item.setVisible(false);

                    return true;
                }

                widgetController.requestWidgetLayoutChange();

                if (viewController == null) {
                    return super.onOptionsItemSelected(item);
                }

                viewController.showHome();

                return true;
            case R.id.abm_remove_widget:
                if (widgetController == null) {
                    // something wrong here, item should not be visible... hiding
                    item.setVisible(false);

                    return super.onOptionsItemSelected(item);
                }

                widgetController.requestWidgetRemoval();

                if (viewController == null) {
                    return super.onOptionsItemSelected(item);
                }

                viewController.showHome();

                return true;
            case R.id.abm_grid_toggle:
                if (viewController == null || sharedPreferencesDAO == null) {
                    return super.onOptionsItemSelected(item);
                }

                final boolean isCurrentDetailGrid = viewController.getCurrentDetailIndex() == ViewController.GRID_ID;
                item.setChecked(!isCurrentDetailGrid);

                final int newLayout;
                if (!isCurrentDetailGrid) {
                    newLayout = ViewController.GRID_ID;
                } else {
                    newLayout = ViewController.LIST_ID;
                }
                viewController.setCurrentDetailIndex(newLayout);
                // Update database
                sharedPreferencesDAO.putInt(ViewController.KEY_DRAWER_LAYOUT, newLayout);

                viewController.showDetail();

                return true;
            case R.id.abm_show_hidden:
                if (drawerListAdapter == null) {
                    return super.onOptionsItemSelected(item);
                }

                final boolean isShowingHiddenApps = drawerListAdapter.isShowingHiddenApps();

                drawerListAdapter.setShowHiddenApps(!isShowingHiddenApps);
                item.setChecked(!isShowingHiddenApps);

                new FilterDrawerListAdapterTask(drawerListAdapter).execute();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Get the menu item from the action bar.
     * @param itemID the item id for the action bar
     * @return the specified menu item or <code>null</code>, if none are found
     */
    @Nullable
    public MenuItem getActionBarMenuItem(final int itemID) {
        if (actionBarMenu == null) {
            return null;
        }

        return actionBarMenu.findItem(itemID);
    }
}
