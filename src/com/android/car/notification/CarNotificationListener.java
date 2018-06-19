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

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * NotificationListenerService that fetches all notifications from system.
 */
public class CarNotificationListener extends NotificationListenerService {
    private static final String TAG = "CarNotListener";
    static final String ACTION_LOCAL_BINDING = "local_binding";
    static final String NOTIFY_NOTIFICATIONS_CHANGED = "notify_notifications_changed";
    private CarNotificationCenterActivity.LocalHandler mHandler;
    private CarHeadsUpNotificationManager mHeadsUpNotificationManager;
    private List<StatusBarNotification> mNotifications = new ArrayList<>();
    private RankingMap mRankingMap;

    @Override
    public IBinder onBind(Intent intent) {
        mHeadsUpNotificationManager =
                CarHeadsUpNotificationManager.getInstance(getApplicationContext());
        return ACTION_LOCAL_BINDING.equals(intent.getAction())
                ? new LocalBinder() : super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        mHeadsUpNotificationManager.maybeShowHeadsUp(sbn, rankingMap);

        mNotifications.removeIf(notification ->
                CarNotificationDiff.areNotificationsTheSame(notification, sbn));
        mNotifications.add(sbn);
        mRankingMap = rankingMap;
        onNotificationChanged();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        mNotifications.removeIf(notification ->
                CarNotificationDiff.areNotificationsTheSame(notification, sbn));
        onNotificationChanged();
    }

    @Override
    public void onNotificationRankingUpdate(RankingMap rankingMap) {
        mRankingMap = rankingMap;
        onNotificationChanged();
    }

    /**
     * Get all active notifications.
     *
     * @return a list of all active notifications.
     */
    List<StatusBarNotification> getNotifications() {
        return mNotifications;
    }

    @Override
    public RankingMap getCurrentRanking() {
        return mRankingMap;
    }

    @Override
    public void onListenerConnected() {
        mNotifications = new ArrayList<>(Arrays.asList(getActiveNotifications()));
        mRankingMap = super.getCurrentRanking();
    }

    @Override
    public void onListenerDisconnected() {
    }

    public void setHandler(CarNotificationCenterActivity.LocalHandler handler) {
        mHandler = handler;
    }

    private void onNotificationChanged() {
        if (mHandler == null) {
            return;
        }
        Message msg = Message.obtain(mHandler);
        msg.obj = NOTIFY_NOTIFICATIONS_CHANGED;
        mHandler.sendMessage(msg);
    }

    class LocalBinder extends Binder {
        public CarNotificationListener getService() {
            return CarNotificationListener.this;
        }
    }
}
