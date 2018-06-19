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

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;

import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Displays all undismissed notifications.
 */
public class CarNotificationCenterActivity extends Activity {

    private final LocalHandler mHandler = new LocalHandler();
    private boolean mBound;
    private CarNotificationListener mListener;
    private CarNotificationViewAdapter mAdapter;
    private NotificationManager mNotificationManager;

    private ItemTouchHelper.SimpleCallback mItemTouchCallback =
            new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
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
                    StatusBarNotification notification =
                            mAdapter.getNotificationAtPosition(viewHolder.getAdapterPosition());
                    if (isCancelable(notification.getNotification())) {
                        try {
                            mNotificationManager.getService().cancelNotificationWithTag(
                                    notification.getPackageName(),
                                    notification.getTag(),
                                    notification.getId(),
                                    UserHandle.USER_SYSTEM);
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    } else {
                        // TODO: better animation for items not allowed to be dismissed.
                        updateNotifications();
                    }
                }
            };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mListener = ((CarNotificationListener.LocalBinder) binder).getService();
            mListener.setHandler(mHandler);
            updateNotifications();
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mListener.setHandler(null);
            mListener = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_center_activity);
        findViewById(R.id.exit_button_container).setOnClickListener(v -> finish());
        PagedListView listView = findViewById(R.id.notifications);
        mAdapter = new CarNotificationViewAdapter(this);
        listView.setAdapter(mAdapter);

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        new ItemTouchHelper(mItemTouchCallback).attachToRecyclerView(listView.getRecyclerView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, CarNotificationListener.class);
        intent.setAction(CarNotificationListener.ACTION_LOCAL_BINDING);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    class LocalHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            if (message.obj instanceof String) {
                if (CarNotificationListener.NOTIFY_NOTIFICATIONS_CHANGED.equals(message.obj)) {
                    updateNotifications();
                }
            }
        }
    }

    private boolean isCancelable(Notification notification) {
        return (notification.flags
                & (Notification.FLAG_FOREGROUND_SERVICE
                | Notification.FLAG_NO_CLEAR
                | Notification.FLAG_ONGOING_EVENT)) == 0;
    }

    private void updateNotifications() {
        mAdapter.setNotifications(RankingAndFilteringManager.process(
                mListener.getNotifications(), mListener.getCurrentRanking()));
    }
}
