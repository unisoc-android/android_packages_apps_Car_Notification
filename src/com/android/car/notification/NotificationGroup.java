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
 * <li>One notification with no group summary notification</li>
 * <li>One group summary notification with no child notifications</li>
 * <li>A group of notifications with a group summary notification</li>
 * </ol>
 */
public class NotificationGroup {

    private String mGroupKey;
    private final List<StatusBarNotification> mNotifications = new ArrayList<>();
    @Nullable
    private List<String> mChildTitles;
    @Nullable
    private StatusBarNotification mGroupSummaryNotification;

    public void addNotification(StatusBarNotification statusBarNotification) {
        assertSameGroupKey(statusBarNotification.getGroupKey());
        mNotifications.add(statusBarNotification);

        // Sort the child notifications by the sort key
        // If a sort key is not supplied, sort by the posted time in the descending order
        Comparator<StatusBarNotification> comparator = ((Comparator<StatusBarNotification>)
                (left, right) -> {
                    if (left.getNotification().getSortKey() == null
                            || right.getNotification().getSortKey() == null) {
                        return 0;
                    }
                    return left.getNotification().getSortKey().compareTo(
                            right.getNotification().getSortKey());
                })
                .thenComparing(
                        (left, right)
                                -> left.getPostTime() < right.getPostTime() ? 1 : -1);

        Collections.sort(mNotifications, comparator);
    }

    void setGroupSummaryNotification(StatusBarNotification groupSummaryNotification) {
        assertSameGroupKey(groupSummaryNotification.getGroupKey());
        // There exists a group summary notification
        if (mGroupSummaryNotification != null) {
            mNotifications.add(groupSummaryNotification);
            return;
        }
        mGroupSummaryNotification = groupSummaryNotification;
    }

    void setGroupKey(@NonNull String groupKey) {
        mGroupKey = groupKey;
    }

    /**
     * Returns the group key of this notification group. When this group contains only one
     * notification, it is the key of the notification.
     *
     * <p> {@code null} will be returned if the group key has not been set yet.
     */
    @Nullable
    public String getGroupKey() {
        return mGroupKey;
    }

    /**
     * Returns the count of how many child notifications (excluding the group summary notification)
     * this notification group has.
     */
    public int getChildCount() {
        return mNotifications.size();
    }

    /**
     * Returns true when it has a group summary notification and >1 child notifications
     */
    public boolean isGroup() {
        return mGroupSummaryNotification != null && getChildCount() > 1;
    }

    /**
     * Returns the list of the child notifications.
     */
    public List<StatusBarNotification> getChildNotifications() {
        return mNotifications;
    }

    /**
     * Returns the group summary notification.
     */
    @Nullable
    public StatusBarNotification getGroupSummaryNotification() {
        return mGroupSummaryNotification;
    }

    /**
     * Sets the list of child notification titles.
     */
    public void setChildTitles(List<String> childTitles) {
        mChildTitles = childTitles;
    }

    /**
     * Returns the list of child notification titles.
     */
    @Nullable
    public List<String> getChildTitles() {
        return mChildTitles;
    }

    /**
     * Generates the list of the child notification titles for a group summary notification.
     */
    public List<String> generateChildTitles() {
        List<String> titles = new ArrayList<>();

        for (StatusBarNotification notification : mNotifications) {
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
     * the group summary notification is returned.
     *
     * <p> If the NotificationGroup has only 1 child notification,
     * or has more than 1 child notifications without a valid group summary,
     * the first child notification is returned.
     *
     * @return the notification that represents this NotificationGroup
     */
    public StatusBarNotification getSingleNotification() {
        if (isGroup() || getChildCount() == 0) {
            return getGroupSummaryNotification();

        } else {
            return mNotifications.get(0);
        }
    }

    StatusBarNotification getNotificationForSorting() {
        if (mGroupSummaryNotification != null) {
            return getGroupSummaryNotification();
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
