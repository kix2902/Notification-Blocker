package com.redinput.notifblock;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnCheckboxAppChecked, SearchView.OnQueryTextListener {

    private PackageManager manager;

    private SearchView searchView;
    private RecyclerView recyclerView;
    private ImageView empty;

    private SharedPreferences mPreferences;

    private List<ApplicationInfo> applications;
    private AppsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        empty = (ImageView) findViewById(R.id.empty);

        searchView = (SearchView) findViewById(R.id.search);
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_apps);
        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        manager = getPackageManager();

        applications = manager.getInstalledApplications(0);

        Collections.sort(applications, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo app1, ApplicationInfo app2) {
                String label1 = app1.loadLabel(manager).toString();
                String label2 = app2.loadLabel(manager).toString();

                return label1.compareToIgnoreCase(label2);
            }
        });

        adapter = new AppsAdapter(MainActivity.this, applications, this);
        recyclerView.setAdapter(adapter);
    }

    private boolean hasAccessGranted() {
        ContentResolver contentResolver = this.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, Utils.SETTING_NOTIFICATION_LISTENER);
        String packageName = this.getPackageName();

        // check to see if the enabledNotificationListeners String contains our package name
        return !(enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!hasAccessGranted()) {
            mPreferences.edit().remove(Utils.PREF_ENABLED).apply();
            Snackbar.make(empty, R.string.snackbar_not_allowed_title, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_not_allowed_action, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                            } else {
                                startActivity(new Intent(Utils.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                            }
                        }
                    })
                    .show();
        }

        refreshState();
    }

    private void refreshState() {
        if (mPreferences.contains(Utils.PREF_ENABLED)) {
            if (mPreferences.getBoolean(Utils.PREF_ENABLED, false)) {
                showApps();
            } else {
                hideApps();
            }
        } else {
            hideApps();
            invalidateOptionsMenu();
        }
    }

    private void showApps() {
        recyclerView.setVisibility(View.VISIBLE);
        searchView.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
    }

    private void hideApps() {
        recyclerView.setVisibility(View.GONE);
        searchView.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        MenuItem item = menu.findItem(R.id.action_switch);
        if (item != null) {
            SwitchCompat action_bar_switch = (SwitchCompat) item.getActionView().findViewById(R.id.action_switch);
            if (action_bar_switch != null) {
                if (hasAccessGranted()) {
                    action_bar_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            mPreferences.edit().putBoolean(Utils.PREF_ENABLED, isChecked).apply();
                            refreshState();
                        }
                    });

                    if (mPreferences.getBoolean(Utils.PREF_ENABLED, false)) {
                        action_bar_switch.setChecked(true);
                    } else {
                        action_bar_switch.setChecked(false);
                    }
                    action_bar_switch.setEnabled(true);

                } else {
                    action_bar_switch.setChecked(false);
                    action_bar_switch.setEnabled(false);
                }
            }
        }
        return true;
    }

    @Override
    public void onCheckboxAppChecked(int position, boolean isChecked) {
        String pkg = adapter.getIem(position).packageName;

        if (mPreferences.contains(Utils.PREF_PACKAGES_BLOCKED)) {
            HashSet<String> pkgs = new HashSet<>(Arrays.asList(mPreferences.getString(Utils.PREF_PACKAGES_BLOCKED, "").split(";")));

            if (isChecked) {
                pkgs.add(pkg);
            } else {
                pkgs.remove(pkg);
            }

            mPreferences.edit().putString(Utils.PREF_PACKAGES_BLOCKED, TextUtils.join(";", pkgs)).apply();

        } else {
            mPreferences.edit().putString(Utils.PREF_PACKAGES_BLOCKED, pkg).apply();
        }
    }


    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            adapter.setApps(applications);

        } else {
            String query = newText.toLowerCase();
            ArrayList<ApplicationInfo> filtered = new ArrayList<>();
            for (ApplicationInfo app : applications) {
                String label = app.loadLabel(manager).toString().toLowerCase();
                if (label.contains(query)) {
                    filtered.add(app);
                }
            }
            adapter.setApps(filtered);
        }
        return true;
    }
}
