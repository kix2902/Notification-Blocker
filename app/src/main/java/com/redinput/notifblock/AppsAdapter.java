package com.redinput.notifblock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.AppsViewHolder> {

    public static class AppsViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        CheckBox hide;
        OnCheckboxAppChecked listener;

        public AppsViewHolder(View itemView, final OnCheckboxAppChecked listener) {
            super(itemView);

            this.listener = listener;
            icon = (ImageView) itemView.findViewById(R.id.app_icon);
            name = (TextView) itemView.findViewById(R.id.app_name);
            hide = (CheckBox) itemView.findViewById(R.id.app_hide);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hide.performClick();
                }
            });

            hide.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    listener.onCheckboxAppChecked(getLayoutPosition(), isChecked);
                }
            });
        }
    }

    private PackageManager pm;
    private LayoutInflater mLayoutInflater;

    private List<ApplicationInfo> apps;
    private OnCheckboxAppChecked checkedListener;
    private SharedPreferences preferences;

    public AppsAdapter(Context context, List<ApplicationInfo> apps, OnCheckboxAppChecked checkedListener) {
        this.apps = apps;
        pm = context.getPackageManager();
        mLayoutInflater = LayoutInflater.from(context);
        this.checkedListener = checkedListener;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        setHasStableIds(true);
    }

    @Override
    public AppsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = mLayoutInflater.inflate(R.layout.app_recycler_row, parent, false);
        return new AppsViewHolder(v, checkedListener);
    }

    @Override
    public void onBindViewHolder(AppsViewHolder holder, int position) {
        ApplicationInfo app = apps.get(position);
        HashSet<String> blocked = new HashSet<>(Arrays.asList(preferences.getString(Utils.PREF_PACKAGES_BLOCKED, "").split(";")));

        holder.icon.setImageDrawable(app.loadIcon(pm));
        holder.name.setText(app.loadLabel(pm));
        holder.hide.setChecked(blocked.contains(app.packageName));
    }

    @Override
    public long getItemId(int position) {
        return (long) apps.get(position).packageName.hashCode();
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public ApplicationInfo getIem(int position) {
        return apps.get(position);
    }

    public void setApps(List<ApplicationInfo> apps) {
        this.apps = apps;
        notifyDataSetChanged();
    }
}
