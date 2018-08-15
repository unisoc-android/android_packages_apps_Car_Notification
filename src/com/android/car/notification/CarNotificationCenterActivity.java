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
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
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
import android.util.Log;

import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Displays all undismissed notifications.
 */
public class CarNotificationCenterActivity extends Activity {

    private static final String TAG = "CarNotificationCenterActivity";

    private final LocalHandler mHandler = new LocalHandler();
    private boolean mNotificationListenerBound;
    private CarNotificationListener mNotificationListener;
    private CarNotificationViewAdapter mAdapter;
    private NotificationManager mNotificationManager;
    private CarHeadsUpNotificationManager mHeadsUpNotificationManager;
    private PreprocessingManager mPreprocessingManager;
    private Car mCar;

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
                    NotificationGroup notificationGroup =
                            mAdapter.getNotificationAtPosition(viewHolder.getAdapterPosition());

                    // TODO: implement dismissal for grouped notification
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

    private ServiceConnection mCarConnectionListener = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                CarUxRestrictionsManager manager = (CarUxRestrictionsManager) mCar.getCarManager(
                        Car.CAR_UX_RESTRICTION_SERVICE);

                mAdapter.setCarUxRestrictions(manager.getCurrentCarUxRestrictions());
                manager.registerListener(
                        restrictionInfo -> mAdapter.setCarUxRestrictions(restrictionInfo));
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in CarConnectionListener", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "Car service disconnected unexpectedly");
        }
    };

    private ServiceConnection mNotificationListenerConnectionListener = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mNotificationListener = ((CarNotificationListener.LocalBinder) binder).getService();
            mNotificationListener.setHandler(mHandler);
            updateNotifications();
            mNotificationListenerBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mNotificationListener.setHandler(null);
            mNotificationListener = null;
            mNotificationListenerBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreprocessingManager = PreprocessingManager.getInstance(getApplicationContext());
        mHeadsUpNotificationManager =
                CarHeadsUpNotificationManager.getInstance(getApplicationContext());
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mCar = Car.createCar(this, mCarConnectionListener);

        setContentView(R.layout.notification_center_activity);
        findViewById(R.id.exit_button_container).setOnClickListener(v -> finish());
        PagedListView listView = findViewById(R.id.notifications);
        mAdapter = new CarNotificationViewAdapter(this, /* isGroupNotificationAdapter= */ false);
        listView.setAdapter(mAdapter);

        new ItemTouchHelper(mItemTouchCallback).attachToRecyclerView(listView.getRecyclerView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect to car service
        if (mCar != null) {
            mCar.connect();
        }

        // Bind notification listener
        Intent intent = new Intent(this, CarNotificationListener.class);
        intent.setAction(CarNotificationListener.ACTION_LOCAL_BINDING);
        bindService(intent, mNotificationListenerConnectionListener, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect from car service
        if (mCar != null) {
            mCar.disconnect();
        }

        // Unbind notification listener
        if (mNotificationListenerBound) {
            unbindService(mNotificationListenerConnectionListener);
            mNotificationListenerBound = false;
        }
    }

    class LocalHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            if (message.what == CarNotificationListener.NOTIFY_NOTIFICATIONS_CHANGED) {
                updateNotifications();

            } else if (message.what == CarNotificationListener.NOTIFY_NOTIFICATION_ADDED) {
                mHeadsUpNotificationManager =
                        CarHeadsUpNotificationManager.getInstance(getApplicationContext());
                mHeadsUpNotificationManager.maybeShowHeadsUp(
                        mNotificationListener.getCarUxRestrictions(),
                        (StatusBarNotification) message.obj,
                        mNotificationListener.getCurrentRanking());
                updateNotifications();
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
        mAdapter.setNotifications(
                mPreprocessingManager.process(
                        mNotificationListener.getCarUxRestrictions(),
                        mNotificationListener.getNotifications(),
                        mNotificationListener.getCurrentRanking()));
    }
}
