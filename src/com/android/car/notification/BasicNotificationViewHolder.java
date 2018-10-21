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
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Basic notification view template that displays a minimal notification.
 */
public class BasicNotificationViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "car_notification_basic";
    private final Context mContext;
    private final CarNotificationHeaderView mHeaderView;
    private final CarNotificationBodyView mBodyView;
    private final CarNotificationActionsView mActionsView;
    private final View mParentView;
    private final FrameLayout mBigContentView;

    public BasicNotificationViewHolder(View view) {
        super(view);
        mContext = view.getContext();
        mParentView = view;
        mHeaderView = view.findViewById(R.id.notification_header);
        mBodyView = view.findViewById(R.id.notification_body);
        mActionsView = view.findViewById(R.id.notification_actions);
        mBigContentView = view.findViewById(R.id.big_content_view);
    }

    /**
     * Binds a {@link StatusBarNotification} to a basic car notification template.
     *
     * @param statusBarNotification passing {@code null} clears the view.
     * @param isInGroup whether this notification card is part of a group.
     */
    public void bind(StatusBarNotification statusBarNotification, boolean isInGroup) {
        reset();
        if (statusBarNotification == null) {
            return;
        }

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

        if (notification.bigContentView != null) {
            View view = notification.bigContentView.apply(mContext, /* parent= */ mBigContentView);
            mBigContentView.setVisibility(View.VISIBLE);
            mBigContentView.addView(view);
            mHeaderView.setVisibility(View.GONE);
            mBodyView.setVisibility(View.GONE);
            mActionsView.setVisibility(View.GONE);
            // If a notification came with a custom content view,
            // do not bind anything else other than the custom view.
            return;
        }

        mHeaderView.bind(statusBarNotification, /* primaryColor= */ null);
        mActionsView.bind(statusBarNotification, isInGroup);

        Bundle extraData = notification.extras;

        CharSequence title = extraData.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extraData.getCharSequence(Notification.EXTRA_TEXT);
        Icon icon = notification.getLargeIcon();
        mBodyView.bind(title, text, icon);
    }

    /**
     * Resets the basic notification view empty for recycling.
     */
    private void reset() {
        mParentView.setClickable(false);
        mParentView.setOnClickListener(null);

        mBigContentView.removeAllViews();
        mBigContentView.setVisibility(View.GONE);
    }
}
