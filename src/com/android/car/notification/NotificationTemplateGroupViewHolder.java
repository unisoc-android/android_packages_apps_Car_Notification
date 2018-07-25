/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.notification;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewHolder that binds a list of notifications as a grouped notification.
 */
public class NotificationTemplateGroupViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "car_notification_group";
    private final Context mContext;
    private final View mParentView;
    private final Button mToggleButton;
    private final RecyclerView mNotificationListView;
    private final CarNotificationViewAdapter mAdapter;
    private final Drawable mExpandDrawable;
    private final Drawable mCollapseDrawable;

    public NotificationTemplateGroupViewHolder(View view) {
        super(view);
        mContext = view.getContext();
        mParentView = view;
        mToggleButton = view.findViewById(R.id.toggle_button);
        mNotificationListView = view.findViewById(R.id.notification_list);

        int carAccentColor = mContext.getColor(R.color.car_body2);
        mExpandDrawable = mContext.getDrawable(R.drawable.expand_more);
        mExpandDrawable.setTint(carAccentColor);
        mCollapseDrawable = mContext.getDrawable(R.drawable.expand_less);
        mCollapseDrawable.setTint(carAccentColor);

        mNotificationListView.setLayoutManager(new LinearLayoutManager(mContext));
        mNotificationListView.setNestedScrollingEnabled(false);
        mAdapter = new CarNotificationViewAdapter(mContext, /* isChildAdapter= */ true);
        mNotificationListView.setAdapter(mAdapter);
    }

    public void bind(
            NotificationGroup group, CarNotificationViewAdapter parentAdapter, boolean isExpanded) {

        // use the same view pool as the parent recycler view
        // so that all child recycler views can share the same view pool with the parent
        // to increase the number of the shared views and reduce memory cost
        mNotificationListView.setRecycledViewPool(parentAdapter.getViewPool());

        // bind expand button
        updateToggleButton(isExpanded);
        mToggleButton.setOnClickListener(
                view -> parentAdapter.toggleExpansion(group.getPackageName(), !isExpanded));

        // bind notification cards
        List<NotificationGroup> list = new ArrayList<>();
        if (isExpanded) {
            // bind all child notifications
            group.getChildNotifications().forEach(notification -> {
                NotificationGroup notificationGroup = new NotificationGroup();
                notificationGroup.addNotification(notification);
                list.add(notificationGroup);
            });
        } else {
            // only show group header
            NotificationGroup notificationGroup = new NotificationGroup();
            notificationGroup.addNotification(group.getGroupHeaderNotification());
            list.add(notificationGroup);
        }
        mAdapter.setNotifications(list);
    }

    private void updateToggleButton(boolean isExpanded) {
        if (isExpanded) {
            mToggleButton.setText(R.string.collapse);
            mToggleButton.setCompoundDrawablesWithIntrinsicBounds(
                    mCollapseDrawable, null, null, null);
        } else {
            mToggleButton.setText(R.string.expand);
            mToggleButton.setCompoundDrawablesWithIntrinsicBounds(
                    mExpandDrawable, null, null, null);
        }
    }
}
