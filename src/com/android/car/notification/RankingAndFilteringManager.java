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
import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manager that filters and ranks the notifications in the notification center.
 */
public class RankingAndFilteringManager {

    /**
     * Ranks the given notifications. Currently no filtering is applied to the notifications.
     * In order for {@link DiffUtil} to work, the adapter needs a new data object each time it
     * updates, therefore wrapping the return value in a new list.
     *
     * @param notifications the notifications to be processed.
     * @param rankingMap    the ranking map for the notifications.
     * @return the ranked notifications in a new list.
     */
    public static List<StatusBarNotification> process(
            @NonNull List<StatusBarNotification> notifications,
            @NonNull NotificationListenerService.RankingMap rankingMap) {

        return new ArrayList<>(rank(notifications, rankingMap));
    }

    /**
     * Rank notifications in the following order:
     * <ol>
     *  <li> Category: CAR_EMERGENCY
     *  <li> Importance: HIGH
     *  <li> Category: CAR_WARNING
     *  <li> Category: CAR_INFORMATION
     *  <li> Importance: Default
     *  <li> Importance: Low
     *  <li> Importance: Min
     * </ol>
     */
    private static List<StatusBarNotification> rank(
            List<StatusBarNotification> notifications,
            NotificationListenerService.RankingMap rankingMap) {

        Collections.sort(notifications, new NotificationComparator(rankingMap));
        return notifications;
    }

    private static class NotificationComparator implements Comparator<StatusBarNotification> {
        private final NotificationListenerService.RankingMap mRankingMap;

        NotificationComparator(NotificationListenerService.RankingMap rankingMap) {
            mRankingMap = rankingMap;
        }

        @Override
        public int compare(StatusBarNotification left, StatusBarNotification right) {
            // Category: CATEGORY_CAR_EMERGENCY
            String leftCategory = left.getNotification().category;
            String rightCategory = right.getNotification().category;
            if (Notification.CATEGORY_CAR_EMERGENCY.equals(leftCategory)) {
                return -1;
            } else if (Notification.CATEGORY_CAR_EMERGENCY.equals(rightCategory)) {
                return 1;
            }

            // Importance: IMPORTANCE_HIGH
            NotificationListenerService.Ranking leftRanking =
                    new NotificationListenerService.Ranking();
            mRankingMap.getRanking(left.getKey(), leftRanking);
            int leftImportance = leftRanking.getImportance();

            NotificationListenerService.Ranking rightRanking =
                    new NotificationListenerService.Ranking();
            mRankingMap.getRanking(right.getKey(), rightRanking);
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
