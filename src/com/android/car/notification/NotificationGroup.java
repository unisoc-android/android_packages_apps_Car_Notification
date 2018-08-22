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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Data structure representing a notification card in car.
 * A notification group can hold either:
 * <ol>
 * <li>One notification with no group header</li>
 * <li>One group header with no child notifications</li>
 * <li>A group of notifications with a group header notification</li>
 * </ol>
 */
class NotificationGroup {

    private String mGroupKey;
    private final List<StatusBarNotification> mNotifications = new ArrayList<>();
    @Nullable
    private List<String> mChildTitles;
    @Nullable
    private StatusBarNotification mGroupHeaderNotification;

    NotificationGroup() {
    }

    void addNotification(StatusBarNotification statusBarNotification) {
        assertSameGroupKey(PreprocessingManager.getGroupKey(statusBarNotification));
        mNotifications.add(statusBarNotification);
        // Sort the child notifications by the group key
        // If a group key is not supplied, sort by the posted time in the descending order
        Collections.sort(
                mNotifications,
                Comparator.comparing(StatusBarNotification::getGroupKey, String::compareTo)
                        .thenComparing((left, right)
                                -> left.getPostTime() < right.getPostTime() ? 1 : -1));
    }

    void setGroupHeaderNotification(StatusBarNotification groupHeaderNotification) {
        assertSameGroupKey(PreprocessingManager.getGroupKey(groupHeaderNotification));
        // There exists a group summary notification
        if (mGroupHeaderNotification != null) {
            mNotifications.add(groupHeaderNotification);
            return;
        }
        mGroupHeaderNotification = groupHeaderNotification;
    }

    void setGroupKey(@NonNull String groupKey) {
        mGroupKey = groupKey;
    }

    @NonNull
    String getGroupKey() {
        return mGroupKey;
    }

    int getChildCount() {
        return mNotifications.size();
    }

    /**
     * Returns true when it has a group header and >1 child notifications
     */
    boolean isGroup() {
        return mGroupHeaderNotification != null && getChildCount() > 1;
    }

    @NonNull
    List<StatusBarNotification> getChildNotifications() {
        return mNotifications;
    }

    @Nullable
    StatusBarNotification getGroupHeaderNotification() {
        return mGroupHeaderNotification;
    }

    /**
     * Sets the list of child notification titles.
     */
    void setChildTitles(List<String> childTitles) {
        mChildTitles = childTitles;
    }

    /**
     * Returns the list of child notification titles.
     */
    @Nullable
    List<String> getChildTitles() {
        return mChildTitles;
    }

    /**
     * Generates the list of the child notification titles for a group summary notification.
     */
    List<String> generateChildTitles() {
        List<String> titles = new ArrayList<>();

        for(StatusBarNotification notification : mNotifications) {
            Bundle extras = notification.getNotification().extras;
            if (extras.containsKey(Notification.EXTRA_TITLE)) {
                titles.add(extras.getString(Notification.EXTRA_TITLE));

            } else if (extras.containsKey(Notification.EXTRA_TITLE_BIG)) {
                titles.add(extras.getString(Notification.EXTRA_TITLE_BIG));

            } else if (extras.containsKey(Notification.EXTRA_MESSAGES)) {
                List<Notification.MessagingStyle.Message> messages =
                        Notification.MessagingStyle.Message.getMessagesFromBundleArray(
                                extras.getParcelableArray(Notification.EXTRA_MESSAGES));
                Notification.MessagingStyle.Message lastMessage = messages.get(messages.size() - 1);
                titles.add(lastMessage.getSenderPerson().getName().toString());

            } else if (extras.containsKey(Notification.EXTRA_SUB_TEXT)) {
                titles.add(extras.getString(Notification.EXTRA_SUB_TEXT));
            }
        }

        return titles;
    }

    /**
     * Returns a single notification that represents this NotificationGroup:
     *
     * <p> If the NotificationGroup is a valid grouped notification or has no child notifications,
     * the group header notification is returned.
     *
     * <p> If the NotificationGroup has only 1 child notification,
     * or has more than 1 child notifications without a valid group header,
     * the first child notification is returned.
     *
     * @return the notification that represents this NotificationGroup
     */
    @NonNull
    StatusBarNotification getSingleNotification() {
        if (isGroup() || getChildCount() == 0) {
            return getGroupHeaderNotification();

        } else {
            return mNotifications.get(0);
        }
    }

    @NonNull
    StatusBarNotification getNotificationForSorting() {
        if (mGroupHeaderNotification != null) {
            return getGroupHeaderNotification();
        }
        return getSingleNotification();
    }

    private void assertSameGroupKey(String groupKey) {
        if (mGroupKey == null) {
            setGroupKey(groupKey);
        } else if (!mGroupKey.equals(groupKey)) {
            throw new IllegalStateException(
                    "Group key mismatch when adding a notification to a group. " +
                            "mGroupKey: " + mGroupKey + "; groupKey:" + groupKey);
        }
    }
}
