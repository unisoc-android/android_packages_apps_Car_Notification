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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

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
class PreprocessingManager {
    private static final String SYSTEM_PACKAGE_NAME = "android";
    private static PreprocessingManager mInstance;
    private final String mEllipsizedString;
    private final Context mContext;
    private final PackageManager mPackageManager;

    private PreprocessingManager(Context context) {
        mContext = context.getApplicationContext();
        mPackageManager = context.getPackageManager();
        mEllipsizedString = context.getString(R.string.ellipsized_string);
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
     * @param notifications the notifications to be processed.
     * @param rankingMap    the ranking map for the notifications.
     * @return the processed notifications in a new list.
     */
    public List<NotificationGroup> process(
            CarUxRestrictions carUxRestrictions,
            @NonNull List<StatusBarNotification> notifications,
            @NonNull NotificationListenerService.RankingMap rankingMap) {

        return new ArrayList<>(
                rank(group(optimizeForDriving(carUxRestrictions, notifications)), rankingMap));
    }

    /**
     * Step 1: Process a list of {@link StatusBarNotification}s to be driving optimized.
     * Note that the string length limit is always respected regardless of whether distraction
     * optimization is required.
     */
    private List<StatusBarNotification> optimizeForDriving(
            CarUxRestrictions carUxRestrictions, List<StatusBarNotification> notifications) {
        notifications.forEach(
                notification -> notification = optimizeForDriving(carUxRestrictions, notification));
        return notifications;
    }

    /**
     * Helper method that optimize a single {@link StatusBarNotification} for driving.
     * Currently only trimming texts that have visual effects in car.
     * Operation is done on the original notification object passed in; no new object is created.
     */
    StatusBarNotification optimizeForDriving(
            CarUxRestrictions carUxRestrictions, StatusBarNotification notification) {
        int maxStringLength = carUxRestrictions.getMaxRestrictedStringLength();
        Bundle extras = notification.getNotification().extras;
        for (String key : extras.keySet()) {
            switch (key) {
                case Notification.EXTRA_TITLE:
                case Notification.EXTRA_TEXT:
                case Notification.EXTRA_TITLE_BIG:
                case Notification.EXTRA_SUMMARY_TEXT:
                    String value = extras.getString(key);
                    if (!TextUtils.isEmpty(value) && value.length() > maxStringLength) {
                        extras.putString(
                                key, value.substring(0, maxStringLength).concat(mEllipsizedString));
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
        boolean isEmergency =
                Notification.CATEGORY_CAR_EMERGENCY.equals(
                        statusBarNotification.getNotification().category);
        boolean isSystem = SYSTEM_PACKAGE_NAME.equals(statusBarNotification.getPackageName());

        if (groupKey == null || isEmergency || isSystem) {
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
