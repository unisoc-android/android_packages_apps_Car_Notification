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
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import com.android.car.notification.NotificationGroup;
import com.android.car.notification.R;

/**
 * Group summary notification view template that displays an automatically generated
 * group summary notification.
 */
public class GroupSummaryNotificationViewHolder extends CarNotificationBaseViewHolder {
    private static final String TAG = "car_notif_group_summary";
    private final TextView mTitle1View;
    private final TextView mTitle2View;
    private final TextView mUnshownCountView;
    private final View mParentView;
    private final Context mContext;
    private StatusBarNotification mStatusBarNotification;

    /**
     * Constructor of the GroupSummaryNotificationViewHolder with a group summary template view.
     *
     * @param view group summary template view supplied by the adapter
     */
    public GroupSummaryNotificationViewHolder(View view) {
        super(view);
        mParentView = view;
        mContext = view.getContext();
        mTitle1View = view.findViewById(R.id.child_notification_title_1);
        mTitle2View = view.findViewById(R.id.child_notification_title_2);
        mUnshownCountView = view.findViewById(R.id.unshown_count);
    }

    /**
     * Binds a {@link NotificationGroup} to a group summary notification template.
     */
    public void bind(NotificationGroup notificationGroup) {
        reset();

        mStatusBarNotification = notificationGroup.getSingleNotification();
        Notification notification = mStatusBarNotification.getNotification();

        if (notification.contentIntent != null) {
            mParentView.setOnClickListener(v -> {
                try {
                    notification.contentIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Cannot send pendingIntent in action button");
                }
            });
        }

        List<String> titles = notificationGroup.getChildTitles();
        if (titles != null && !titles.isEmpty()) {
            mTitle1View.setVisibility(View.VISIBLE);
            mTitle1View.setText(titles.get(0));

            if (titles.size() > 1) {
                mTitle2View.setVisibility(View.VISIBLE);
                mTitle2View.setText(titles.get(1));

                int unshownCount = titles.size() - 2;
                if (unshownCount > 0) {
                    mUnshownCountView.setVisibility(View.VISIBLE);
                    mUnshownCountView.setText(
                            mContext.getString(R.string.unshown_count, unshownCount));
                }
            }
        }
    }

    /**
     * Resets the notification view empty for recycling.
     */
    @Override
    void reset() {
        super.reset();

        mParentView.setClickable(false);
        mParentView.setOnClickListener(null);

        mTitle1View.setText(null);
        mTitle1View.setVisibility(View.GONE);

        mTitle2View.setText(null);
        mTitle2View.setVisibility(View.GONE);

        mUnshownCountView.setText(null);
        mUnshownCountView.setVisibility(View.GONE);
    }

    @Override
    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }
}
