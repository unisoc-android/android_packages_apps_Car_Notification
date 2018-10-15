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
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Basic notification view template that displays a progress bar notification.
 * This template is only used in notification center and never as a heads-up notification.
 */
public class ProgressNotificationViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "car_notification_basic";
    private final CarNotificationHeaderView mHeaderView;
    private final CarNotificationBodyView mBodyView;
    private final CarNotificationActionsView mActionsView;
    private final ProgressBar mProgressBarView;
    private final View mParentView;

    public ProgressNotificationViewHolder(View view) {
        super(view);
        mParentView = view;
        mHeaderView = view.findViewById(R.id.notification_header);
        mBodyView = view.findViewById(R.id.notification_body);
        mActionsView = view.findViewById(R.id.notification_actions);
        mProgressBarView = view.findViewById(R.id.progress_bar);
    }

    /**
     * Binds a {@link StatusBarNotification} to a car progress notification template.
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

        mHeaderView.bind(statusBarNotification, /* primaryColor= */ null);
        mActionsView.bind(statusBarNotification, isInGroup);

        Bundle extraData = notification.extras;
        CharSequence title = extraData.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extraData.getCharSequence(Notification.EXTRA_TEXT);
        Icon icon = notification.getLargeIcon();
        mBodyView.bind(title, text, icon);

        mProgressBarView.setVisibility(View.VISIBLE);
        boolean isIndeterminate = extraData.getBoolean(
                Notification.EXTRA_PROGRESS_INDETERMINATE);
        int progress = extraData.getInt(Notification.EXTRA_PROGRESS);
        int progressMax = extraData.getInt(Notification.EXTRA_PROGRESS_MAX);
        mProgressBarView.setIndeterminate(isIndeterminate);
        mProgressBarView.setMax(progressMax);
        mProgressBarView.setProgress(progress);
    }

    /**
     * Resets the basic notification view empty for recycling.
     */
    private void reset() {
        mParentView.setClickable(false);
        mParentView.setOnClickListener(null);

        mProgressBarView.setProgress(0);
        mProgressBarView.setVisibility(View.GONE);
    }
}
