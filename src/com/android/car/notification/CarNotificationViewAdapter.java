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

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Notification data adapter that binds a notification to the corresponding view.
 */
public class CarNotificationViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context mContext;
    private final LayoutInflater mInflater;
    private List<StatusBarNotification> mNotifications = new ArrayList<>();
    private boolean mIsDistractionOptimizationRequired;

    public CarNotificationViewAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        View view;
        switch (viewType) {
            case NotificationViewType.MESSAGING_NOTIFICATION_VIEW_TYPE:
                view = mInflater
                        .inflate(R.layout.car_messaging_notification_template, parent, false);
                viewHolder = new NotificationTemplateMessagingViewHolder(view, mContext);
                break;
            case NotificationViewType.PROGRESS_NOTIFICATION_VIEW_TYPE:
                view = mInflater
                        .inflate(R.layout.car_progress_notification_template, parent, false);
                viewHolder = new NotificationTemplateProgressViewHolder(view, mContext);
                break;
            case NotificationViewType.BASIC_NOTIFICATION_VIEW_TYPE:
            default:
                view = mInflater
                        .inflate(R.layout.car_basic_notification_template, parent, false);
                viewHolder = new NotificationTemplateBasicViewHolder(view, mContext);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        StatusBarNotification notification = mNotifications.get(position);
        switch (holder.getItemViewType()) {
            case NotificationViewType.MESSAGING_NOTIFICATION_VIEW_TYPE:
                ((NotificationTemplateMessagingViewHolder) holder).bind(notification);
                break;
            case NotificationViewType.PROGRESS_NOTIFICATION_VIEW_TYPE:
                ((NotificationTemplateProgressViewHolder) holder).bind(notification);
                break;
            case NotificationViewType.BASIC_NOTIFICATION_VIEW_TYPE:
            default:
                ((NotificationTemplateBasicViewHolder) holder).bind(notification);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        StatusBarNotification statusBarNotification = mNotifications.get(position);
        Notification notification = statusBarNotification.getNotification();
        Bundle extras = notification.extras;

        // messaging
        boolean isMessage = Notification.CATEGORY_MESSAGE.equals(notification.category);
        if (isMessage) {
            return NotificationViewType.MESSAGING_NOTIFICATION_VIEW_TYPE;
        }

        // progress
        int progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX);
        boolean isIndeterminate = extras.getBoolean(
                Notification.EXTRA_PROGRESS_INDETERMINATE);
        boolean hasValidProgress = isIndeterminate || progressMax != 0;
        boolean isProgress = extras.containsKey(Notification.EXTRA_PROGRESS)
                && extras.containsKey(Notification.EXTRA_PROGRESS_MAX)
                && hasValidProgress
                && !notification.hasCompletedProgress();
        if (isProgress) {
            return NotificationViewType.PROGRESS_NOTIFICATION_VIEW_TYPE;
        }

        // basic
        return NotificationViewType.BASIC_NOTIFICATION_VIEW_TYPE;
    }

    @Override
    public int getItemCount() {
        return mNotifications.size();
    }

    /**
     * Gets a notification given its adapter position.
     */
    public StatusBarNotification getNotificationAtPosition(int position) {
        if (position < mNotifications.size()) {
            return mNotifications.get(position);
        } else {
            return null;
        }
    }

    /**
     * Updates notifications and update views.
     */
    public void setNotifications(List<StatusBarNotification> notifications) {
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(
                        new CarNotificationDiff(mNotifications, notifications), true);
        mNotifications = notifications;
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Sets whether distraction optimization is required and update views.
     */
    public void setIsDistractionOptimizationRequired(boolean isDistractionOptimizationRequired) {
        mIsDistractionOptimizationRequired = isDistractionOptimizationRequired;
        notifyDataSetChanged();
    }

    /**
     * Gets whether distraction optimization is required.
     */
    public boolean getIsDistractionOptimizationRequired() {
        return mIsDistractionOptimizationRequired;
    }
}
