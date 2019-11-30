package com.redinput.notifblock

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.redinput.notifblock.AppsAdapter.AppsViewHolder
import java.util.*

class AppsAdapter(context: Context, private var apps: List<ApplicationInfo>, checkedListener: OnCheckboxAppChecked) : RecyclerView.Adapter<AppsViewHolder>() {

    class AppsViewHolder(itemView: View, var listener: OnCheckboxAppChecked) : RecyclerView.ViewHolder(itemView) {
        var icon: ImageView
        var name: TextView
        var hide: CheckBox

        init {
            icon = itemView.findViewById(R.id.app_icon)
            name = itemView.findViewById(R.id.app_name)
            hide = itemView.findViewById(R.id.app_hide)
            itemView.setOnClickListener { hide.performClick() }
            hide.setOnCheckedChangeListener { buttonView, isChecked -> listener.onCheckboxAppChecked(layoutPosition, isChecked) }
        }
    }

    private val pm: PackageManager
    private val mLayoutInflater: LayoutInflater
    private val checkedListener: OnCheckboxAppChecked
    private val preferences: SharedPreferences
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsViewHolder {
        val v = mLayoutInflater.inflate(R.layout.app_recycler_row, parent, false)
        return AppsViewHolder(v, checkedListener)
    }

    override fun onBindViewHolder(holder: AppsViewHolder, position: Int) {
        val app = apps[position]
        val blocked = HashSet(Arrays.asList(*preferences.getString(Utils.PREF_PACKAGES_BLOCKED, "")!!.split(";").toTypedArray()))
        holder.icon.setImageDrawable(app.loadIcon(pm))
        holder.name.text = app.loadLabel(pm)
        holder.hide.isChecked = blocked.contains(app.packageName)
    }

    override fun getItemId(position: Int): Long {
        return apps[position].packageName.hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    fun getIem(position: Int): ApplicationInfo {
        return apps[position]
    }

    fun setApps(apps: List<ApplicationInfo>) {
        this.apps = apps
        notifyDataSetChanged()
    }

    init {
        pm = context.packageManager
        mLayoutInflater = LayoutInflater.from(context)
        this.checkedListener = checkedListener
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        setHasStableIds(true)
    }
}