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
import android.car.CarNotConnectedException;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Manager that groups and ranks the notifications in the notification center.
 */
public class PreprocessingManager {
    private static final String TAG = "PreprocessingManager";
    private static final String SYSTEM_PACKAGE_NAME = "android";
    private static PreprocessingManager mInstance;
    private final String mEllipsizedString;
    private final boolean mEnableMediaNotification;
    private final boolean mEnableOngoingNotification;
    private CarUxRestrictionsManager mUxRestrictionsManager;

    private PreprocessingManager(Context context) {
        mEllipsizedString = context.getString(R.string.ellipsized_string);
        mEnableMediaNotification =
                context.getResources().getBoolean(R.bool.config_showMediaNotification);
        mEnableOngoingNotification =
                context.getResources().getBoolean(R.bool.config_showOngoingNotification);
    }

    public static PreprocessingManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new PreprocessingManager(context);
        }
        return mInstance;
    }

    /**
     * Process the given notifications.
     *
     * @param rankingMap    the ranking map for the notifications.
     * @return the processed notifications in a new list.
     */
    public List<NotificationGroup> process(
            @NonNull List<StatusBarNotification> notifications,
            @NonNull NotificationListenerService.RankingMap rankingMap) {

        return new ArrayList<>(
                rank(group(optimizeForDriving(filter(notifications))), rankingMap));
    }

    /**
     * Step 1: Process a list of {@link StatusBarNotification}s to be driving optimized.
     */
    private List<StatusBarNotification> filter(List<StatusBarNotification> notifications) {
        if (!mEnableMediaNotification) {
            notifications.removeIf(
                    statusBarNotification ->
                            Notification.CATEGORY_TRANSPORT.equals(
                                    statusBarNotification.getNotification().category));
        }
        if (!mEnableOngoingNotification) {
            notifications.removeIf(statusBarNotification -> statusBarNotification.isOngoing());
        }
        return notifications;
    }

    /**
     * Step 1: Process a list of {@link StatusBarNotification}s to be driving optimized.
     * Note that the string length limit is always respected regardless of whether distraction
     * optimization is required.
     */
    private List<StatusBarNotification> optimizeForDriving(
            List<StatusBarNotification> notifications) {
        notifications.forEach(
                notification -> notification = optimizeForDriving(notification));
        return notifications;
    }

    /**
     * Helper method that optimize a single {@link StatusBarNotification} for driving.
     * Currently only trimming texts that have visual effects in car.
     * Operation is done on the original notification object passed in; no new object is created.
     */
    StatusBarNotification optimizeForDriving(StatusBarNotification notification) {
        CarUxRestrictions carUxRestrictions = getCurrentUxRestrictions();
        int maxStringLength = carUxRestrictions != null
                ? carUxRestrictions.getMaxRestrictedStringLength()
                : Integer.MAX_VALUE;
        Bundle extras = notification.getNotification().extras;
        for (String key : extras.keySet()) {
            switch (key) {
                case Notification.EXTRA_TITLE:
                case Notification.EXTRA_TEXT:
                case Notification.EXTRA_TITLE_BIG:
                case Notification.EXTRA_SUMMARY_TEXT:
                    CharSequence value = extras.getCharSequence(key);
                    if (!TextUtils.isEmpty(value) && value.length() > maxStringLength) {
                        extras.putString(key, value.toString()
                                .substring(0, maxStringLength).concat(mEllipsizedString));
                    }
                default:
                    continue;
            }
        }
        return notification;
    }

    /**
     * Step 2: Group notifications that have the same group key.
     * Never groups system notifications nor car emergency notifications.
     *
     * @param list list of ungrouped {@link StatusBarNotification}s.
     * @return list of grouped notifications as {@link NotificationGroup}s.
     */
    private List<NotificationGroup> group(List<StatusBarNotification> list) {
        SortedMap<String, NotificationGroup> groupedNotifications = new TreeMap<>();

        for (int i = 0; i < list.size(); i++) {
            StatusBarNotification statusBarNotification = list.get(i);
            Notification notification = statusBarNotification.getNotification();

            String groupKey = getGroupKey(statusBarNotification);
            if (!groupedNotifications.containsKey(groupKey)) {
                NotificationGroup notificationGroup = new NotificationGroup();
                groupedNotifications.put(groupKey, notificationGroup);
            }
            if (notification.isGroupSummary()) {
                groupedNotifications.get(groupKey)
                        .setGroupHeaderNotification(statusBarNotification);
            } else {
                groupedNotifications.get(groupKey).addNotification(statusBarNotification);
            }
        }

        // In order for DiffUtil to work, the adapter needs a new data object each time it
        // updates, therefore wrapping the return value in a new list.
        return new ArrayList(groupedNotifications.values());
    }

    /**
     * Step 3: Rank notifications according to the ranking key supplied by the notification.
     */
    private static List<NotificationGroup> rank(
            List<NotificationGroup> notifications,
            NotificationListenerService.RankingMap rankingMap) {

        Collections.sort(notifications, new NotificationComparator(rankingMap));
        return notifications;
    }

    public void setCarUxRestrictionsManager(CarUxRestrictionsManager manager) {
        mUxRestrictionsManager = manager;
    }

    private CarUxRestrictions getCurrentUxRestrictions() {
        if (mUxRestrictionsManager == null) {
            return null;
        }

        try {
            return mUxRestrictionsManager.getCurrentCarUxRestrictions();
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Failed to get UxRestrictions thus running unrestricted", e);
            return null;
        }
    }

    private static class NotificationComparator implements Comparator<NotificationGroup> {
        private final NotificationListenerService.RankingMap mRankingMap;

        NotificationComparator(NotificationListenerService.RankingMap rankingMap) {
            mRankingMap = rankingMap;
        }

        @Override
        public int compare(NotificationGroup left, NotificationGroup right) {
            NotificationListenerService.Ranking leftRanking =
                    new NotificationListenerService.Ranking();
            mRankingMap.getRanking(left.getNotificationForSorting().getKey(), leftRanking);

            NotificationListenerService.Ranking rightRanking =
                    new NotificationListenerService.Ranking();
            mRankingMap.getRanking(right.getNotificationForSorting().getKey(), rightRanking);

            return leftRanking.getRank() - rightRanking.getRank();
        }
    }

    /**
     * Helper method that generates a unique identifier for each grouped notification.
     */
    static String getGroupKey(StatusBarNotification statusBarNotification) {
        String groupKey = statusBarNotification.getGroup();
        String category = statusBarNotification.getNotification().category;
        boolean isEmergency = Notification.CATEGORY_CAR_EMERGENCY.equals(category);
        boolean isMedia = Notification.CATEGORY_TRANSPORT.equals(category);
        boolean isSystem = SYSTEM_PACKAGE_NAME.equals(statusBarNotification.getPackageName());

        if (groupKey == null || isEmergency || isMedia || isSystem) {
            // If a notification is not part of a group, use a unique identifier as the group key
            groupKey = statusBarNotification.getKey();
        } else {
            // Append the package name to the group key,
            // in case it is the default override group key (same for every package)
            groupKey += statusBarNotification.getPackageName();
        }
        return groupKey;
    }
}
