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
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
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
    private static final String TAG = "CarNotificationAdapter";

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final boolean mIsGroupNotificationAdapter;
    // book keeping expanded notification groups
    private final List<String> mExpandedNotifications = new ArrayList<>();

    private List<NotificationGroup> mNotifications = new ArrayList<>();
    private RecyclerView.RecycledViewPool mViewPool;
    private CarUxRestrictions mCarUxRestrictions;

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
            case NotificationViewType.GROUP_EXPANDED:
            case NotificationViewType.GROUP_COLLAPSED:
                view = mInflater.inflate(
                        R.layout.group_notification_template, parent, false);
                viewHolder = new GroupNotificationViewHolder(view);
                break;
            case NotificationViewType.GROUP_SUMMARY:
                view = mInflater
                        .inflate(R.layout.group_summary_notification_template, parent, false);
                viewHolder = new GroupSummaryNotificationViewHolder(view);
                break;
            case NotificationViewType.EMERGENCY:
                view = mInflater.inflate(
                        R.layout.emergency_notification_template, parent, false);
                viewHolder = new EmergencyNotificationViewHolder(view);
                break;
            case NotificationViewType.MESSAGE_IN_GROUP:
                view = mInflater.inflate(
                        R.layout.message_notification_template_inner, parent, false);
                viewHolder = new MessageNotificationViewHolder(view);
                break;
            case NotificationViewType.MESSAGE:
                view = mInflater.inflate(R.layout.message_notification_template, parent, false);
                viewHolder = new MessageNotificationViewHolder(view);
                break;
            case NotificationViewType.MEDIA:
                view = mInflater.inflate(R.layout.media_notification_template, parent, false);
                viewHolder = new MediaNotificationViewHolder(view);
                break;
            case NotificationViewType.PROGRESS_IN_GROUP:
                view = mInflater.inflate(
                        R.layout.progress_notification_template_inner, parent, false);
                viewHolder = new ProgressNotificationViewHolder(view);
                break;
            case NotificationViewType.PROGRESS:
                view = mInflater
                        .inflate(R.layout.progress_notification_template, parent, false);
                viewHolder = new ProgressNotificationViewHolder(view);
                break;
            case NotificationViewType.INBOX_IN_GROUP:
                view = mInflater
                        .inflate(R.layout.inbox_notification_template_inner, parent, false);
                viewHolder = new InboxNotificationViewHolder(view);
                break;
            case NotificationViewType.INBOX:
                view = mInflater
                        .inflate(R.layout.inbox_notification_template, parent, false);
                viewHolder = new InboxNotificationViewHolder(view);
                break;
            case NotificationViewType.BASIC_IN_GROUP:
                view = mInflater
                        .inflate(R.layout.basic_notification_template_inner, parent, false);
                viewHolder = new BasicNotificationViewHolder(view);
                break;
            case NotificationViewType.BASIC:
            default:
                view = mInflater
                        .inflate(R.layout.basic_notification_template, parent, false);
                viewHolder = new BasicNotificationViewHolder(view);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        NotificationGroup notificationGroup = mNotifications.get(position);

        switch (holder.getItemViewType()) {
            case NotificationViewType.GROUP_EXPANDED:
                ((GroupNotificationViewHolder) holder).bind(notificationGroup, this, true);
                break;
            case NotificationViewType.GROUP_COLLAPSED:
                ((GroupNotificationViewHolder) holder).bind(notificationGroup, this, false);
                break;
            case NotificationViewType.GROUP_SUMMARY:
                ((GroupSummaryNotificationViewHolder) holder).bind(notificationGroup);
                break;
            case NotificationViewType.EMERGENCY: {
                StatusBarNotification notification = notificationGroup.getSingleNotification();
                ((EmergencyNotificationViewHolder) holder).bind(notification);
                break;
            }
            case NotificationViewType.MESSAGE_IN_GROUP:
            case NotificationViewType.MESSAGE: {
                StatusBarNotification notification = notificationGroup.getSingleNotification();
                ((MessageNotificationViewHolder) holder)
                        .bind(notification, /* isInGroup= */ mIsGroupNotificationAdapter);
                break;
            }
            case NotificationViewType.MEDIA: {
                StatusBarNotification notification = notificationGroup.getSingleNotification();
                ((MediaNotificationViewHolder) holder).bind(notification);
                break;
            }
            case NotificationViewType.PROGRESS_IN_GROUP:
            case NotificationViewType.PROGRESS: {
                StatusBarNotification notification = notificationGroup.getSingleNotification();
                ((ProgressNotificationViewHolder) holder)
                        .bind(notification, /* isInGroup= */ mIsGroupNotificationAdapter);
                break;
            }
            case NotificationViewType.INBOX_IN_GROUP:
            case NotificationViewType.INBOX: {
                StatusBarNotification notification = notificationGroup.getSingleNotification();
                ((InboxNotificationViewHolder) holder)
                        .bind(notification, /* isInGroup= */ mIsGroupNotificationAdapter);
                break;
            }
            case NotificationViewType.BASIC_IN_GROUP:
            case NotificationViewType.BASIC:
            default: {
                StatusBarNotification notification = notificationGroup.getSingleNotification();
                ((BasicNotificationViewHolder) holder)
                        .bind(notification, /* isInGroup= */ mIsGroupNotificationAdapter);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        NotificationGroup notificationGroup = mNotifications.get(position);

        if (notificationGroup.isGroup()) {
            if (mExpandedNotifications.contains(notificationGroup.getGroupKey())) {
                return NotificationViewType.GROUP_EXPANDED;
            } else {
                return NotificationViewType.GROUP_COLLAPSED;
            }
        }

        Notification notification =
                notificationGroup.getSingleNotification().getNotification();
        Bundle extras = notification.extras;

        // car emergency
        boolean isEmergency = Notification.CATEGORY_CAR_EMERGENCY.equals(notification.category);
        if (isEmergency) {
            return NotificationViewType.EMERGENCY;
        }

        // messaging
        boolean isMessage = Notification.CATEGORY_MESSAGE.equals(notification.category);
        if (isMessage) {
            return mIsGroupNotificationAdapter
                    ? NotificationViewType.MESSAGE_IN_GROUP : NotificationViewType.MESSAGE;
        }

        // media
        if (notification.isMediaNotification()) {
            return NotificationViewType.MEDIA;
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
                    ? NotificationViewType.PROGRESS_IN_GROUP : NotificationViewType.PROGRESS;
        }

        // inbox
        boolean isInbox = extras.containsKey(Notification.EXTRA_TITLE_BIG)
                && extras.containsKey(Notification.EXTRA_SUMMARY_TEXT);
        if (isInbox) {
            return mIsGroupNotificationAdapter
                    ? NotificationViewType.INBOX_IN_GROUP : NotificationViewType.INBOX;
        }

        // group summary
        boolean isGroupSummary = notificationGroup.getChildTitles() != null;
        if (isGroupSummary) {
            return NotificationViewType.GROUP_SUMMARY;
        }

        // the big text and big picture styles are fallen back to basic template in car
        // i.e. setting the big text and big picture does not have an effect
        boolean isBigText = extras.containsKey(Notification.EXTRA_BIG_TEXT);
        if (isBigText) {
            Log.i(TAG, "Big text style is not supported as a car notification");
        }
        boolean isBigPicture = extras.containsKey(Notification.EXTRA_PICTURE);
        if (isBigPicture) {
            Log.i(TAG, "Big picture style is not supported as a car notification");
        }

        // basic, big text, big picture, car warning and car information
        return mIsGroupNotificationAdapter
                ? NotificationViewType.BASIC_IN_GROUP : NotificationViewType.BASIC;
    }

    @Override
    public int getItemCount() {
        int itemCount = mNotifications.size();

        if (!mIsGroupNotificationAdapter
                && mCarUxRestrictions != null
                && (mCarUxRestrictions.getActiveRestrictions()
                    & CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT) != 0) {

            int maxItemCount = mCarUxRestrictions.getMaxCumulativeContentItems();

            return Math.min(itemCount, maxItemCount);
        }
        return itemCount;
    }

    /**
     * Set the expansion state of a group notification given its group key.
     *
     * @param groupKey   the unique identifier of a {@link NotificationGroup}
     * @param isExpanded whether the group notification should be expanded.
     */
    void setExpanded(String groupKey, boolean isExpanded) {
        if (isExpanded(groupKey) == isExpanded) {
            return;
        }

        if (isExpanded) {
            mExpandedNotifications.add(groupKey);
        } else {
            mExpandedNotifications.remove(groupKey);
        }

        int index = findIndexInNotification(groupKey);
        notifyItemChanged(index);
    }

    /**
     * Returns whether the notification is expanded given its group key.
     */
    boolean isExpanded(String groupKey) {
        return mExpandedNotifications.contains(groupKey);
    }

    private int findIndexInNotification(String groupKey) {
        for (int i = 0; i < mNotifications.size(); i++) {
            if (mNotifications.get(i).getGroupKey().equals(groupKey)) {
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
                        new CarNotificationDiff(mContext, mNotifications, notifications), true);
        mNotifications = notifications;
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Sets the current {@link CarUxRestrictions}.
     */
    public void setCarUxRestrictions(CarUxRestrictions carUxRestrictions) {
        mCarUxRestrictions = carUxRestrictions;
        notifyDataSetChanged();
    }

    /**
     * Gets the current {@link CarUxRestrictions}.
     */
    public CarUxRestrictions getCarUxRestrictions() {
        return mCarUxRestrictions;
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