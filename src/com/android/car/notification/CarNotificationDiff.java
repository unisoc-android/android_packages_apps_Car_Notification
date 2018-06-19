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
 * <li> Id
 * <li> Package name
 * <li> Targeted user
 * <li> Tag
 * </ol>
 */
class CarNotificationDiff extends DiffUtil.Callback {
    private final List<StatusBarNotification> mOldList;
    private final List<StatusBarNotification> mNewList;

    CarNotificationDiff(
            @NonNull List<StatusBarNotification> oldList,
            @NonNull List<StatusBarNotification> newList) {
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
        StatusBarNotification oldItem = mOldList.get(oldItemPosition);
        StatusBarNotification newItem = mNewList.get(newItemPosition);
        return areNotificationsTheSame(oldItem, newItem);
    }

    /**
     * Whether two notifications are considered the same.
     * Two notifications are considered the same if they have the same:
     * <ol>
     * <li> Id
     * <li> Package name
     * <li> Targeted user
     * <li> Tag
     * </ol>
     */
    static boolean areNotificationsTheSame(
            StatusBarNotification oldItem, StatusBarNotification newItem) {
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
