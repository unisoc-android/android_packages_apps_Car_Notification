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
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Data structure representing a notification card in car.
 * A notification group can hold either:
 * <ol>
 *     <li>One notification with no group header</li>
 *     <li>A group of notifications with a group header notification</li>
 * </ol>
 */
class NotificationGroup {

    private static final String TAG = "NotificationGroup";

    @NonNull private String mPackageName;
    @NonNull private final List<StatusBarNotification> mNotifications = new ArrayList<>();
    @Nullable private StatusBarNotification mGroupHeaderNotification;

    NotificationGroup() {
    }

    void addNotification(StatusBarNotification statusBarNotification) {
        assertSamePackageName(statusBarNotification.getPackageName());
        mNotifications.add(statusBarNotification);
        Collections.sort(mNotifications, new PostTimeComparator());
    }

    void setGroupHeaderNotification(StatusBarNotification groupHeaderNotification) {
        assertSamePackageName(groupHeaderNotification.getPackageName());
        mGroupHeaderNotification = groupHeaderNotification;
    }

    void setPackageName(@NonNull String packageName) {
        mPackageName = packageName;
    }

    @NonNull String getPackageName() {
        return mPackageName;
    }

    int getChildCount() {
        return mNotifications.size();
    }

    boolean isGroup() {
        return mGroupHeaderNotification != null && getChildCount() > 1;
    }

    StatusBarNotification getFirstNotification() {
        if (isGroup()) {
            Log.w(TAG, "Getting only the first notification from a grouped notification!");
        }
        return mNotifications.get(0);
    }

    @NonNull List<StatusBarNotification> getChildNotifications() {
        return mNotifications;
    }

    @Nullable StatusBarNotification getGroupHeaderNotification() {
        return mGroupHeaderNotification;
    }

    private void assertSamePackageName(String packageName) {
        if (mPackageName == null) {
            setPackageName(packageName);
        } else if (!mPackageName.equals(packageName)) {
            throw new IllegalStateException(
                    "Package name mismatch when adding a notification to a group. " +
                            "mPackageName: " + mPackageName + "; packageName:" + packageName);
        }
    }

    /**
     * Comparator that ranks the notifications in the descending order according to the posted time.
     */
    private static class PostTimeComparator implements Comparator<StatusBarNotification> {
        PostTimeComparator() {
        }

        @Override
        public int compare(StatusBarNotification left, StatusBarNotification right) {
            return left.getPostTime() < right.getPostTime() ? 1 : -1;
        }
    }
}
