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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
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
public class GroupNotificationViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "car_notification_group";
    private final Context mContext;
    private final View mParentView;
    private final Button mToggleButton;
    private final RecyclerView mNotificationListView;
    private final CarNotificationViewAdapter mAdapter;
    private final Drawable mExpandDrawable;
    private final Drawable mCollapseDrawable;
    private final Paint mPaint;
    private final int mDividerMargin;
    private final int mDividerHeight;

    public GroupNotificationViewHolder(View view) {
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

        mDividerMargin = mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_1);
        mPaint = new Paint();
        mPaint.setColor(mContext.getColor(R.color.car_list_divider));
        mDividerHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.car_list_divider_height);

        mNotificationListView.setLayoutManager(new LinearLayoutManager(mContext));
        mNotificationListView.addItemDecoration(new GroupedNotificationItemDecoration());
        mNotificationListView.setNestedScrollingEnabled(false);
        mAdapter = new CarNotificationViewAdapter(mContext, /* isGroupNotificationAdapter= */ true);
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

    private class GroupedNotificationItemDecoration extends RecyclerView.ItemDecoration {

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            // not drawing the divider for the last item
            for (int i = 0; i < parent.getChildCount() - 1; i++) {
                drawDivider(c, parent.getChildAt(i));
            }
        }

        /**
         * Draws a divider under {@code container}.
         */
        private void drawDivider(Canvas c, View container) {
            int left = container.getLeft() + mDividerMargin;
            int right = container.getRight() - mDividerMargin;
            int bottom = container.getBottom() + mDividerHeight;
            int top = bottom - mDividerHeight;

            c.drawRect(left, top, right, bottom, mPaint);
        }
    }
}
