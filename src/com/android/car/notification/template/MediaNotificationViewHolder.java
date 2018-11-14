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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.car.widget.ColumnCardView;

import com.android.car.notification.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Media notification view template that displays a media notification with controls.
 */
public class MediaNotificationViewHolder extends CarNotificationBaseViewHolder {
    private static final String TAG = "car_notification_media";
    private static final int MAX_NUM_ACTIONS = 5;
    private final Context mContext;
    private final CarNotificationHeaderView mHeaderView;
    private final CarNotificationBodyView mBodyView;
    private final View mParentView;
    private final ColumnCardView mCardView;
    private final List<ImageButton> mButtons;
    private final View mActionBarView;
    private StatusBarNotification mStatusBarNotification;

    public MediaNotificationViewHolder(View view) {
        super(view);
        mContext = view.getContext();
        mParentView = view;
        mCardView = view.findViewById(R.id.column_card_view);
        mHeaderView = view.findViewById(R.id.notification_header);
        mBodyView = view.findViewById(R.id.notification_body);
        mActionBarView = view.findViewById(R.id.action_bar);
        mButtons = new ArrayList<>();
        mButtons.add(view.findViewById(R.id.action_1));
        mButtons.add(view.findViewById(R.id.action_2));
        mButtons.add(view.findViewById(R.id.action_3));
        mButtons.add(view.findViewById(R.id.action_4));
        mButtons.add(view.findViewById(R.id.action_5));
    }

    /**
     * Binds a {@link StatusBarNotification} to a car media notification template.
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

        // colors
        Notification.Builder builder = Notification.Builder.recoverBuilder(mContext, notification);
        Context packageContext = statusBarNotification.getPackageContext(mContext);
        MediaNotificationProcessor processor =
                new MediaNotificationProcessor(mContext, packageContext);
        int averageColor = processor.processNotification(notification, builder);
        int primaryColor = builder.getPrimaryTextColor();

        mCardView.setCardBackgroundColor(averageColor);
        mActionBarView.setBackgroundColor(averageColor);

        // header
        mHeaderView.bindWithMediaColor(statusBarNotification, primaryColor);

        // body
        Bundle extraData = notification.extras;
        CharSequence title = extraData.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extraData.getCharSequence(Notification.EXTRA_TEXT);
        Icon icon = notification.getLargeIcon();
        mBodyView.bind(title, text, icon);
        mBodyView.setPrimaryTextColor(primaryColor);
        mBodyView.setSecondaryTextColor(builder.getSecondaryTextColor());

        // action buttons
        Notification.Action[] actions = notification.actions;
        if (actions == null || actions.length == 0) {
            return;
        }
        int length = Math.min(actions.length, MAX_NUM_ACTIONS);
        for (int i = 0; i < length; i++) {
            Notification.Action action = actions[i];
            ImageButton button = mButtons.get(i);
            button.setVisibility(View.VISIBLE);

            Drawable drawable = packageContext.getResources().getDrawable(action.icon);
            drawable.setTint(primaryColor);
            button.setImageDrawable(drawable);

            if (action.actionIntent != null) {
                button.setOnClickListener(v -> {
                    try {
                        action.actionIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "Cannot send pendingIntent in action button");
                    }
                });
            }
        }
    }

    /**
     * Resets the basic notification view empty for recycling.
     */
    @Override
    void reset() {
        super.reset();

        mParentView.setClickable(false);
        mParentView.setOnClickListener(null);

        mButtons.forEach(button -> {
            button.setImageDrawable(null);
            button.setVisibility(View.GONE);
        });
    }

    @Override
    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }

}
