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
    private final boolean mIsGroupNotificationAdapter;
    // book keeping expanded notification groups
    private final List<String> mExpandedNotifications = new ArrayList<>();

    private List<NotificationGroup> mNotifications = new ArrayList<>();
    private boolean mIsDistractionOptimizationRequired;
    private RecyclerView.RecycledViewPool mViewPool;

    /**
     * Constructor for a notification adapter.
     * Can be used both by the root notification list view, or a grouped notification view.
     *
     * @param context                    the context for resources and inflating views
     * @param isGroupNotificationAdapter true if this adapter is used by a grouped notification view
     */
    public CarNotificationViewAdapter(Context context, boolean isGroupNotificationAdapter) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mIsGroupNotificationAdapter = isGroupNotificationAdapter;
        if (!mIsGroupNotificationAdapter) {
            mViewPool = new RecyclerView.RecycledViewPool();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        View view;
        switch (viewType) {
            case NotificationViewType.EXPANDED_GROUP_NOTIFICATION_VIEW_TYPE:
            case NotificationViewType.COLLAPSED_GROUP_NOTIFICATION_VIEW_TYPE:
                view = mInflater.inflate(
                        R.layout.car_group_notification_template, parent, false);
                viewHolder = new NotificationTemplateGroupViewHolder(view);
                break;
            case NotificationViewType.MESSAGE_NOTIFICATION_IN_GROUP_VIEW_TYPE:
                view = mInflater.inflate(
                        R.layout.message_notification_template_inner, parent, false);
                viewHolder = new NotificationTemplateMessagingViewHolder(view);
                break;
            case NotificationViewType.MESSAGING_NOTIFICATION_VIEW_TYPE:
                view = mInflater.inflate(
                        R.layout.car_messaging_notification_template, parent, false);
                viewHolder = new NotificationTemplateMessagingViewHolder(view);
                break;
            case NotificationViewType.PROGRESS_NOTIFICATION_IN_GROUP_VIEW_TYPE:
                view = mInflater.inflate(
                        R.layout.progress_notification_template_inner, parent, false);
                viewHolder = new NotificationTemplateProgressViewHolder(view);
                break;
            case NotificationViewType.PROGRESS_NOTIFICATION_VIEW_TYPE:
                view = mInflater
                        .inflate(R.layout.car_progress_notification_template, parent, false);
                viewHolder = new NotificationTemplateProgressViewHolder(view);
                break;
            case NotificationViewType.BASIC_NOTIFICATION_IN_GROUP_VIEW_TYPE:
                view = mInflater
                        .inflate(R.layout.basic_notification_template_inner, parent, false);
                viewHolder = new NotificationTemplateBasicViewHolder(view);
                break;
            case NotificationViewType.BASIC_NOTIFICATION_VIEW_TYPE:
            default:
                view = mInflater
                        .inflate(R.layout.car_basic_notification_template, parent, false);
                viewHolder = new NotificationTemplateBasicViewHolder(view);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        NotificationGroup notificationGroup = mNotifications.get(position);
        StatusBarNotification notification = notificationGroup.getFirstNotification();

        switch (holder.getItemViewType()) {
            case NotificationViewType.EXPANDED_GROUP_NOTIFICATION_VIEW_TYPE:
                ((NotificationTemplateGroupViewHolder) holder).bind(notificationGroup, this, true);
                break;
            case NotificationViewType.COLLAPSED_GROUP_NOTIFICATION_VIEW_TYPE:
                ((NotificationTemplateGroupViewHolder) holder).bind(notificationGroup, this, false);
                break;
            case NotificationViewType.MESSAGE_NOTIFICATION_IN_GROUP_VIEW_TYPE:
            case NotificationViewType.MESSAGING_NOTIFICATION_VIEW_TYPE:
                ((NotificationTemplateMessagingViewHolder) holder)
                        .bind(notification, /* isInGroup= */ mIsGroupNotificationAdapter);
                break;
            case NotificationViewType.PROGRESS_NOTIFICATION_IN_GROUP_VIEW_TYPE:
            case NotificationViewType.PROGRESS_NOTIFICATION_VIEW_TYPE:
                ((NotificationTemplateProgressViewHolder) holder)
                        .bind(notification, /* isInGroup= */ mIsGroupNotificationAdapter);
                break;
            case NotificationViewType.BASIC_NOTIFICATION_IN_GROUP_VIEW_TYPE:
            case NotificationViewType.BASIC_NOTIFICATION_VIEW_TYPE:
            default:
                ((NotificationTemplateBasicViewHolder) holder)
                        .bind(notification, /* isInGroup= */ mIsGroupNotificationAdapter);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        NotificationGroup notificationGroup = mNotifications.get(position);

        if (notificationGroup.isGroup()) {
            if (mExpandedNotifications.contains(notificationGroup.getPackageName())) {
                return NotificationViewType.EXPANDED_GROUP_NOTIFICATION_VIEW_TYPE;
            } else {
                return NotificationViewType.COLLAPSED_GROUP_NOTIFICATION_VIEW_TYPE;
            }
        }

        Notification notification = notificationGroup.getFirstNotification().getNotification();
        Bundle extras = notification.extras;

        // messaging
        boolean isMessage = Notification.CATEGORY_MESSAGE.equals(notification.category);
        if (isMessage) {
            return mIsGroupNotificationAdapter
                    ? NotificationViewType.MESSAGE_NOTIFICATION_IN_GROUP_VIEW_TYPE
                    : NotificationViewType.MESSAGING_NOTIFICATION_VIEW_TYPE;
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
            return mIsGroupNotificationAdapter
                    ? NotificationViewType.PROGRESS_NOTIFICATION_IN_GROUP_VIEW_TYPE
                    : NotificationViewType.PROGRESS_NOTIFICATION_VIEW_TYPE;
        }

        // basic
        return mIsGroupNotificationAdapter
                ? NotificationViewType.BASIC_NOTIFICATION_IN_GROUP_VIEW_TYPE
                : NotificationViewType.BASIC_NOTIFICATION_VIEW_TYPE;
    }

    @Override
    public int getItemCount() {
        return mNotifications.size();
    }

    void toggleExpansion(String packageName, boolean isExpanded) {
        if (mExpandedNotifications.contains(packageName) && !isExpanded) {
            mExpandedNotifications.remove(packageName);
            int index = findIndexInNotification(packageName);
            notifyItemChanged(index);
        } else if (!mExpandedNotifications.contains(packageName) && isExpanded) {
            mExpandedNotifications.add(packageName);
            int index = findIndexInNotification(packageName);
            notifyItemChanged(index);
        }
    }

    private int findIndexInNotification(String packageName) {
        for (int i = 0; i < mNotifications.size(); i++) {
            if (mNotifications.get(i).getPackageName().equals(packageName)) {
                return i;
            }
        }
        // this should never happen because the contains() is already called
        throw new IllegalStateException("Index not found for in expanded package names");
    }

    /**
     * Gets a notification given its adapter position.
     */
    public NotificationGroup getNotificationAtPosition(int position) {
        if (position < mNotifications.size()) {
            return mNotifications.get(position);
        } else {
            return null;
        }
    }

    /**
     * Updates notifications and update views.
     */
    public void setNotifications(List<NotificationGroup> notifications) {
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

    /**
     * Get root recycler view's view pool so that the child recycler view can share the same
     * view pool with the parent.
     */
    public RecyclerView.RecycledViewPool getViewPool() {
        if (mIsGroupNotificationAdapter) {
            // currently only support one level of expansion.
            throw new IllegalStateException("CarNotificationViewAdapter is a child adapter; "
                    + "its view pool should not be reused.");
        }
        return mViewPool;
    }
}
