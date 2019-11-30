package com.redinput.notifblock

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header_toolbar.*
import java.util.*

class MainActivity : AppCompatActivity(), OnCheckboxAppChecked, SearchView.OnQueryTextListener {

    private var mPreferences: SharedPreferences? = null
    private var applications: MutableList<ApplicationInfo>? = null
    private var adapter: AppsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        search?.setOnQueryTextListener(this)
        recycler?.setHasFixedSize(true)
        recycler?.layoutManager = LinearLayoutManager(this@MainActivity)

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
        applications = packageManager.getInstalledApplications(0).toMutableList()
        applications!!.sortWith(Comparator { app1, app2 ->
            val label1 = app1.loadLabel(packageManager).toString()
            val label2 = app2.loadLabel(packageManager).toString()
            label1.compareTo(label2, ignoreCase = true)
        })
        adapter = AppsAdapter(this@MainActivity, applications!!, this)
        recycler.adapter = adapter
    }

    private fun hasAccessGranted(): Boolean {
        val contentResolver = this.contentResolver
        val enabledNotificationListeners = Settings.Secure.getString(contentResolver, Utils.SETTING_NOTIFICATION_LISTENER)
        val packageName = this.packageName
        // check to see if the enabledNotificationListeners String contains our package name
        return !(enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName))
    }

    override fun onResume() {
        super.onResume()
        if (!hasAccessGranted()) {
            mPreferences!!.edit().remove(Utils.PREF_ENABLED).apply()
            Snackbar.make(empty!!, R.string.snackbar_not_allowed_title, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_not_allowed_action) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } else {
                            startActivity(Intent(Utils.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    }
                    .show()
        }
        refreshState()
    }

    private fun refreshState() {
        if (mPreferences!!.contains(Utils.PREF_ENABLED)) {
            if (mPreferences!!.getBoolean(Utils.PREF_ENABLED, false)) {
                showApps()
            } else {
                hideApps()
            }
        } else {
            hideApps()
            invalidateOptionsMenu()
        }
    }

    private fun showApps() {
        recycler.visibility = View.VISIBLE
        search.visibility = View.VISIBLE
        empty.visibility = View.GONE
    }

    private fun hideApps() {
        recycler.visibility = View.GONE
        search.visibility = View.GONE
        empty.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        val item = menu.findItem(R.id.action_switch)
        if (item != null) {
            val action_bar_switch: SwitchCompat = item.actionView.findViewById(R.id.action_switch)
            if (hasAccessGranted()) {
                action_bar_switch.setOnCheckedChangeListener { _, isChecked ->
                    mPreferences!!.edit().putBoolean(Utils.PREF_ENABLED, isChecked).apply()
                    refreshState()
                }
                action_bar_switch.isChecked = mPreferences!!.getBoolean(Utils.PREF_ENABLED, false)
                action_bar_switch.isEnabled = true
            } else {
                action_bar_switch.isChecked = false
                action_bar_switch.isEnabled = false
            }
        }
        return true
    }

    override fun onCheckboxAppChecked(position: Int, isChecked: Boolean) {
        val pkg = adapter!!.getIem(position).packageName
        if (mPreferences!!.contains(Utils.PREF_PACKAGES_BLOCKED)) {
            val pkgs = HashSet(Arrays.asList(*mPreferences!!.getString(Utils.PREF_PACKAGES_BLOCKED, "")!!.split(";").toTypedArray()))
            if (isChecked) {
                pkgs.add(pkg)
            } else {
                pkgs.remove(pkg)
            }
            mPreferences!!.edit().putString(Utils.PREF_PACKAGES_BLOCKED, TextUtils.join(";", pkgs)).apply()
        } else {
            mPreferences!!.edit().putString(Utils.PREF_PACKAGES_BLOCKED, pkg).apply()
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (TextUtils.isEmpty(newText)) {
            adapter!!.setApps(applications!!)
        } else {
            val query = newText.toLowerCase(Locale.getDefault())
            val filtered = ArrayList<ApplicationInfo>()
            for (app in applications!!) {
                val label = app.loadLabel(packageManager).toString().toLowerCase()
                if (label.contains(query)) {
                    filtered.add(app)
                }
            }
            adapter!!.setApps(filtered)
        }
        return true
    }
}