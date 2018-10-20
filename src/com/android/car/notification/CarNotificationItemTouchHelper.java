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
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.NotificationStats;
import android.service.notification.StatusBarNotification;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;

/**
 * Touch helper for notification cards that controls swipe to dismiss.
 */
class CarNotificationItemTouchHelper extends ItemTouchHelper.SimpleCallback {

    private final CarNotificationViewAdapter mAdapter;
    private final IStatusBarService mBarService;

    public CarNotificationItemTouchHelper(
            CarNotificationViewAdapter adapter) {
        super(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        mAdapter = adapter;
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
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
                // rank and count is used for logging and is not need at this time thus -1
                NotificationVisibility notificationVisibility = NotificationVisibility.obtain(
                        notification.getKey(), /* rank */-1, /* count */-1, /* visible */true);
                mBarService.onNotificationClear(
                        notification.getPackageName(),
                        notification.getTag(),
                        notification.getId(),
                        notification.getUser().getIdentifier(),
                        notification.getKey(),
                        NotificationStats.DISMISSAL_SHADE,
                        notificationVisibility
                );
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
