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

import android.annotation.Nullable;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.car.widget.ColumnCardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Media notification view template that displays a media notification with controls.
 */
public class MediaNotificationViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "car_notification_media";
    private static final int MAX_NUM_ACTIONS = 5;
    private final Context mContext;
    private final CarNotificationHeaderView mHeaderView;
    private final TextView mTitleTextView;
    private final TextView mContentTextView;
    private final View mParentView;
    private final ColumnCardView mCardView;
    private final ImageView mAlbumArtView;
    private final List<ImageButton> mButtons;
    private final View mActionBarView;

    public MediaNotificationViewHolder(View view) {
        super(view);
        mContext = view.getContext();
        mParentView = view;
        mCardView = view.findViewById(R.id.column_card_view);
        mAlbumArtView = view.findViewById(R.id.album_art);
        mActionBarView = view.findViewById(R.id.action_bar);
        mHeaderView = view.findViewById(R.id.notification_header);
        mTitleTextView = view.findViewById(R.id.notification_title);
        mContentTextView = view.findViewById(R.id.notification_text);
        mButtons = new ArrayList<>();
        mButtons.add(view.findViewById(R.id.action_1));
        mButtons.add(view.findViewById(R.id.action_2));
        mButtons.add(view.findViewById(R.id.action_3));
        mButtons.add(view.findViewById(R.id.action_4));
        mButtons.add(view.findViewById(R.id.action_5));
    }

    /**
     * Binds a {@link StatusBarNotification} to a car media notification template.
     *
     * @param statusBarNotification passing {@code null} clears the view.
     */
    public void bind(@Nullable StatusBarNotification statusBarNotification) {
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
        mHeaderView.bind(statusBarNotification, primaryColor);

        // album art
        if (notification.getLargeIcon() != null) {
            mAlbumArtView.setImageIcon(notification.getLargeIcon());
        }

        // title
        Bundle extraData = notification.extras;
        CharSequence title = extraData.getCharSequence(Notification.EXTRA_TITLE);
        if (!TextUtils.isEmpty(title)) {
            mTitleTextView.setVisibility(View.VISIBLE);
            mTitleTextView.setText(title);
            mTitleTextView.setTextColor(primaryColor);
        }

        // artist
        CharSequence text = extraData.getCharSequence(Notification.EXTRA_TEXT);
        if (!TextUtils.isEmpty(text)) {
            mContentTextView.setVisibility(View.VISIBLE);
            mContentTextView.setText(text);
            mContentTextView.setTextColor(builder.getSecondaryTextColor());
        }

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
    private void reset() {
        mParentView.setClickable(false);
        mParentView.setOnClickListener(null);

        mTitleTextView.setText(null);
        mTitleTextView.setVisibility(View.GONE);

        mContentTextView.setText(null);
        mContentTextView.setVisibility(View.GONE);

        mAlbumArtView.setImageIcon(null);

        mButtons.forEach(button -> {
            button.setImageDrawable(null);
            button.setVisibility(View.GONE);
        });
    }
}
