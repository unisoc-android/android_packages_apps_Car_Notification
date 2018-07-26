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
import android.app.NotificationManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

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

    /**
     * Process the given notifications.
     *
     * @param notifications the notifications to be processed.
     * @param rankingMap    the ranking map for the notifications.
     * @return the processed notifications in a new list.
     */
    public static List<NotificationGroup> process(
            @NonNull List<StatusBarNotification> notifications,
            @NonNull NotificationListenerService.RankingMap rankingMap) {

        return new ArrayList<>(rank(group(notifications), rankingMap));
    }

    /**
     * Step 1: Groups notifications from the same app.
     *
     * @param list list of ungrouped {@link StatusBarNotification}s.
     * @return list of grouped notifications as {@link NotificationGroup}s.
     */
    private static List<NotificationGroup> group(List<StatusBarNotification> list) {
        SortedMap<String, NotificationGroup> groupedNotifications = new TreeMap<>();

        for (int i = 0; i < list.size(); i++) {
            StatusBarNotification statusBarNotification = list.get(i);
            Notification notification = statusBarNotification.getNotification();
            String packageName = statusBarNotification.getPackageName();

            if (groupedNotifications.containsKey(packageName)) {
                if (notification.isGroupSummary()) {
                    groupedNotifications
                            .get(packageName).setGroupHeaderNotification(statusBarNotification);
                } else {
                    groupedNotifications.get(packageName).addNotification(statusBarNotification);
                }
            } else {
                NotificationGroup notificationGroup = new NotificationGroup();
                groupedNotifications.put(packageName, notificationGroup);
                if (notification.isGroupSummary()) {
                    notificationGroup.setGroupHeaderNotification(statusBarNotification);
                } else {
                    notificationGroup.addNotification(statusBarNotification);
                }
            }
        }

        // In order for DiffUtil to work, the adapter needs a new data object each time it
        // updates, therefore wrapping the return value in a new list.
        return new ArrayList(groupedNotifications.values());
    }

    /**
     * Step 2: Rank notifications in the following order:
     * <ol>
     * <li> Category: CAR_EMERGENCY
     * <li> Importance: HIGH
     * <li> Category: CAR_WARNING
     * <li> Category: CAR_INFORMATION
     * <li> Importance: Default
     * <li> Importance: Low
     * <li> Importance: Min
     * </ol>
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
            // Category: CATEGORY_CAR_EMERGENCY
            String leftCategory = left.getFirstNotification().getNotification().category;
            String rightCategory = right.getFirstNotification().getNotification().category;
            if (Notification.CATEGORY_CAR_EMERGENCY.equals(leftCategory)) {
                return -1;
            } else if (Notification.CATEGORY_CAR_EMERGENCY.equals(rightCategory)) {
                return 1;
            }

            // Importance: IMPORTANCE_HIGH
            NotificationListenerService.Ranking leftRanking =
                    new NotificationListenerService.Ranking();
            mRankingMap.getRanking(left.getFirstNotification().getKey(), leftRanking);
            int leftImportance = leftRanking.getImportance();

            NotificationListenerService.Ranking rightRanking =
                    new NotificationListenerService.Ranking();
            mRankingMap.getRanking(right.getFirstNotification().getKey(), rightRanking);
            int rightImportance = rightRanking.getImportance();

            if (leftImportance == NotificationManager.IMPORTANCE_HIGH) {
                return -1;
            } else if (rightImportance == NotificationManager.IMPORTANCE_HIGH) {
                return 1;
            }

            // Category: CATEGORY_CAR_WARNING
            if (Notification.CATEGORY_CAR_WARNING.equals(leftCategory)) {
                return -1;
            } else if (Notification.CATEGORY_CAR_WARNING.equals(rightCategory)) {
                return 1;
            }

            // Category: CATEGORY_CAR_INFORMATION
            if (Notification.CATEGORY_CAR_INFORMATION.equals(leftCategory)) {
                return -1;
            } else if (Notification.CATEGORY_CAR_INFORMATION.equals(rightCategory)) {
                return 1;
            }

            // Importance: natural order
            return rightImportance - leftImportance;
        }
    }
}
