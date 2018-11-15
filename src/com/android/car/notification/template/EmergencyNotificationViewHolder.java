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

import com.android.car.notification.R;
/**
 * Notification view template that displays a car emergency notification.
 */
public class EmergencyNotificationViewHolder extends CarNotificationBaseViewHolder {
    private static final String TAG = "car_emergency";
    private final Context mContext;
    private final CarNotificationHeaderView mHeaderView;
    private final CarNotificationActionsView mActionsView;
    private final CarNotificationBodyView mBodyView;
    private final View mParentView;
    private final int mEmergencyActionBarColor;
    private StatusBarNotification mStatusBarNotification;

    public EmergencyNotificationViewHolder(View view) {
        super(view);
        mContext = view.getContext();
        mParentView = view;
        mHeaderView = view.findViewById(R.id.notification_header);
        mBodyView = view.findViewById(R.id.notification_body);
        mActionsView = view.findViewById(R.id.notification_actions);
        mEmergencyActionBarColor = mContext.getColor(R.color.emergency_action_bar_background_color);
    }

    /**
     * Binds a {@link StatusBarNotification} to a car emergency notification template.
     */
    @Override
    public void bind(StatusBarNotification statusBarNotification, boolean isInGroup) {
        reset();

        mStatusBarNotification = statusBarNotification;
        Notification notification = statusBarNotification.getNotification();

        if (notification.contentIntent != null) {
            mParentView.setOnClickListener(v -> {
                try {
                    notification.contentIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Cannot send pendingIntent in action button");
                }
            });
        }

        mHeaderView.bind(statusBarNotification, isInGroup);
        mActionsView.bind(statusBarNotification, isInGroup);

        // Override the car action bar color that was set in CarNotificationActionsView.bind()
        mActionsView.setBackgroundColor(mEmergencyActionBarColor);

        Bundle extraData = notification.extras;
        CharSequence title = extraData.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extraData.getCharSequence(Notification.EXTRA_TEXT);
        Icon icon = notification.getLargeIcon();
        mBodyView.bind(title, text, icon);
    }

    /**
     * Resets the basic notification view empty for recycling.
     */
    @Override
    void reset() {
        super.reset();
        mParentView.setClickable(false);
        mParentView.setOnClickListener(null);
    }

    @Override
    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }
}
