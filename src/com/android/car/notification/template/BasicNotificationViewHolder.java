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
package com.android.car.notification.template;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.android.car.notification.R;
/**
 * Basic notification view template that displays a minimal notification.
 */
public class BasicNotificationViewHolder extends CarNotificationBaseViewHolder {

    private static final String TAG = "car_notification_basic";

    private final Context mContext;
    private final CarNotificationHeaderView mHeaderView;
    private final CarNotificationBodyView mBodyView;
    private final CarNotificationActionsView mActionsView;

    private StatusBarNotification mStatusBarNotification;

    public BasicNotificationViewHolder(View view) {
        super(view);
        mContext = view.getContext();
        mHeaderView = view.findViewById(R.id.notification_header);
        mBodyView = view.findViewById(R.id.notification_body);
        mActionsView = view.findViewById(R.id.notification_actions);
    }

    /**
     * Binds a {@link StatusBarNotification} to a basic car notification template.
     */
    @Override
    public void bind(StatusBarNotification statusBarNotification, boolean isInGroup) {
        reset();
        bindBody(statusBarNotification);
        mHeaderView.bind(statusBarNotification, isInGroup);
        mActionsView.bind(statusBarNotification, isInGroup);
    }

    /**
     * Private method that binds the data to the view.
     */
    private void bindBody(StatusBarNotification statusBarNotification) {
        mStatusBarNotification = statusBarNotification;
        Notification notification = statusBarNotification.getNotification();

        if (notification.contentIntent != null) {
            itemView.setOnClickListener(v -> {
                try {
                    notification.contentIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Cannot send pendingIntent in action button");
                }
            });
        }

        Bundle extraData = notification.extras;
        CharSequence title = extraData.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extraData.getCharSequence(Notification.EXTRA_TEXT);
        Icon icon = notification.getLargeIcon();
        mBodyView.bind(title, text, icon);
    }

    @Override
    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }
}
