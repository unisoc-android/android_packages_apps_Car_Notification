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
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Touch helper for notification cards that controls swipe to dismiss.
 */
class CarNotificationItemTouchHelper extends ItemTouchHelper.SimpleCallback {

    private final CarNotificationViewAdapter mAdapter;
    private final CarUserManagerHelper mCarUserManagerHelper;
    private final NotificationManager mNotificationManager;

    public CarNotificationItemTouchHelper(
            Context context,
            CarNotificationViewAdapter adapter) {
        super(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        mAdapter = adapter;
        mCarUserManagerHelper = new CarUserManagerHelper(context);
        mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public boolean onMove(
            RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            RecyclerView.ViewHolder target) {
        // no-op
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        NotificationGroup notificationGroup =
                mAdapter.getNotificationAtPosition(viewHolder.getAdapterPosition());

        StatusBarNotification notification =
                notificationGroup.isGroup()
                        ? notificationGroup.getGroupHeaderNotification()
                        : notificationGroup.getSingleNotification();

        if (isCancelable(notification.getNotification())) {
            try {
                mNotificationManager.getService().cancelNotificationWithTag(
                        notification.getPackageName(),
                        notification.getTag(),
                        notification.getId(),
                        mCarUserManagerHelper.getCurrentForegroundUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        } else {
            // TODO: better animation for items not allowed to be dismissed.
        }
    }

    /**
     * Returns whether a notification can be cancelled when an explicit dismiss action is taken.
     */
    static boolean isCancelable(Notification notification) {
        return (notification.flags
                & (Notification.FLAG_FOREGROUND_SERVICE
                | Notification.FLAG_ONGOING_EVENT)) == 0;
    }
}
