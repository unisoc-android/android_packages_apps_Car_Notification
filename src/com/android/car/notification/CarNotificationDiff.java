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

import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

/**
 * DiffUtil for car notifications. Two notifications are considered the same if they have the same:
 * <ol>
 * <li> Package name
 * <li> Number of StatusBarNotifications contained
 * <li> The order of each StatusBarNotification
 * <li> The id, package name, targeted user and the tag of each individual StatusBarNotification
 * </ol>
 */
class CarNotificationDiff extends DiffUtil.Callback {
    private final List<NotificationGroup> mOldList;
    private final List<NotificationGroup> mNewList;

    CarNotificationDiff(
            @NonNull List<NotificationGroup> oldList,
            @NonNull List<NotificationGroup> newList) {
        mOldList = oldList;
        mNewList = newList;
    }

    @Override
    public int getOldListSize() {
        return mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        NotificationGroup oldItem = mOldList.get(oldItemPosition);
        NotificationGroup newItem = mNewList.get(newItemPosition);
        return areNotificationGroupsTheSame(oldItem, newItem);
    }

    /**
     * Returns whether two {@link NotificationGroup}s are considered the same.
     * Two grouped notifications are considered the same if they have the same:
     * <ol>
     * <li> Package name
     * <li> Number of StatusBarNotifications contained
     * <li> Content of the group header notification
     * <li> The order of each StatusBarNotification
     * <li> The identifier of each individual StatusBarNotification
     * </ol>
     */
    static boolean areNotificationGroupsTheSame(
            NotificationGroup oldItem, NotificationGroup newItem) {

        // return true if referencing the same object, or both are null
        if (oldItem == newItem) {
            return true;
        }

        if (oldItem == null || newItem == null) {
            return false;
        }

        if (!oldItem.getPackageName().equals(newItem.getPackageName())
                || oldItem.getChildCount() != newItem.getChildCount()) {
            return false;
        }

        if (!areStatusBarNotificationsTheSame(
                oldItem.getGroupHeaderNotification(), newItem.getGroupHeaderNotification())) {
            return false;
        }

        List<StatusBarNotification> oldNotifications = oldItem.getChildNotifications();
        List<StatusBarNotification> newNotifications = newItem.getChildNotifications();

        for (int i = 0; i < oldItem.getChildCount(); i++) {
            StatusBarNotification oldNotification = oldNotifications.get(i);
            StatusBarNotification newNotification = newNotifications.get(i);
            if (!areStatusBarNotificationsTheSame(oldNotification, newNotification)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Whether two {@link StatusBarNotification}s are considered the same.
     * Two notifications are considered the same if they have the same:
     * <ol>
     * <li> Id
     * <li> Package name
     * <li> Targeted user
     * <li> Tag
     * </ol>
     */
    static boolean areStatusBarNotificationsTheSame(
            StatusBarNotification oldItem, StatusBarNotification newItem) {

        // return true if referencing the same object, or both are null
        if (oldItem == newItem) {
            return true;
        }

        return oldItem != null
                && newItem != null
                && oldItem.getId() == newItem.getId()
                && oldItem.getPackageName().equals(newItem.getPackageName())
                && oldItem.getUser().equals(newItem.getUser())
                && Objects.equals(oldItem.getTag(), newItem.getTag());
    }

    /**
     * This method will only be called if {@link #areItemsTheSame} returns true.
     */
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // always update if we get a duplicated notification.
        return false;
    }
}
