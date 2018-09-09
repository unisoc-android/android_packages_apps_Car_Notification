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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Notification actions view that contains the buttons that fire actions.
 */
public class CarNotificationActionsView extends RelativeLayout {

    private static final String TAG = "CarNotificationAction";
    // Maximum 3 actions
    // https://developer.android.com/reference/android/app/Notification.Builder.html#addAction
    private static final int MAX_NUM_ACTIONS = 3;
    private final List<Button> mActionButtons = new ArrayList<>();
    private View mActionsView;
    private int mIconSize;
    private int mCarActionBarColor;
    private int mCarCardColor;

    public CarNotificationActionsView(Context context) {
        super(context);
        init(context);
    }

    public CarNotificationActionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CarNotificationActionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CarNotificationActionsView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mCarActionBarColor = context.getResources().getColor(R.color.notification_action_bar_color);
        mCarCardColor = context.getResources().getColor(R.color.car_card);
        mIconSize = context.getResources().getDimensionPixelOffset(R.dimen.car_primary_icon_size);
        inflate(context, R.layout.car_notification_actions_view, /* root= */ this);
    }

    @Override
    protected void onFinishInflate() {
        mActionsView = findViewById(R.id.notification_actions);
        mActionButtons.add(findViewById(R.id.action_1));
        mActionButtons.add(findViewById(R.id.action_2));
        mActionButtons.add(findViewById(R.id.action_3));
    }

    /**
     * Binds the notification action buttons.
     *
     * @param statusBarNotification the notification that contains the actions.
     * @param isInGroup whether this notification card is part of a group.
     */
    public void bind(StatusBarNotification statusBarNotification, boolean isInGroup) {
        reset();

        Notification notification = statusBarNotification.getNotification();
        Notification.Action[] actions = notification.actions;
        if (actions == null || actions.length == 0) {
            return;
        }

        setVisibility(View.VISIBLE);
        mActionsView.setBackgroundColor(isInGroup ? mCarCardColor : mCarActionBarColor);

        int length = Math.min(actions.length, MAX_NUM_ACTIONS);
        for (int i = 0; i < length; i++) {
            Notification.Action action = actions[i];
            Button button = mActionButtons.get(i);
            button.setVisibility(View.VISIBLE);
            button.setText(action.title);

            Icon icon = action.getIcon();
            if (icon != null) {
                Drawable drawable = icon.loadDrawable(getContext());
                if (drawable != null) {
                    drawable.setBounds(0, 0, mIconSize, mIconSize);
                    button.setCompoundDrawablesRelative(drawable, null, null, null);
                }
            }

            button.setOnClickListener(v -> {
                try {
                    action.actionIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Cannot send pendingIntent in action button");
                }
            });
        }
    }

    /**
     * Resets the notification actions empty for recycling.
     */
    void reset() {
        setVisibility(View.GONE);
        mActionsView.setBackgroundColor(mCarActionBarColor);
        for (Button button : mActionButtons) {
            button.setVisibility(View.GONE);
            button.setText(null);
            button.setCompoundDrawables(null, null, null, null);
            button.setOnClickListener(null);
        }
    }
}
