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

import android.annotation.ColorInt;
import android.app.Notification;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.android.car.notification.NotificationClickHandlerFactory;
import com.android.car.notification.R;
import com.android.car.theme.Themes;

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

    private final int mCardBackgroundColor;
    private final int mDefaultTextColor;

    private final List<Button> mActionButtons = new ArrayList<>();

    public CarNotificationActionsView(Context context) {
        super(context);
    }

    public CarNotificationActionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarNotificationActionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CarNotificationActionsView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    {
        mCardBackgroundColor = Themes.getAttrColor(getContext(), android.R.attr.colorPrimary);
        mDefaultTextColor = Themes.getAttrColor(getContext(), android.R.attr.textColorPrimary);
        inflate(getContext(), R.layout.car_notification_actions_view, /* root= */ this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mActionButtons.add(findViewById(R.id.action_1));
        mActionButtons.add(findViewById(R.id.action_2));
        mActionButtons.add(findViewById(R.id.action_3));
    }

    /**
     * Binds the notification action buttons.
     *
     * @param clickHandlerFactory   factory class used to generate {@link OnClickListener}s.
     * @param statusBarNotification the notification that contains the actions.
     */
    public void bind(
            NotificationClickHandlerFactory clickHandlerFactory,
            StatusBarNotification statusBarNotification) {

        Notification notification = statusBarNotification.getNotification();
        Notification.Action[] actions = notification.actions;
        if (actions == null || actions.length == 0) {
            return;
        }

        int length = Math.min(actions.length, MAX_NUM_ACTIONS);
        for (int i = 0; i < length; i++) {
            Notification.Action action = actions[i];
            Button button = mActionButtons.get(i);
            button.setVisibility(View.VISIBLE);
            // clear spannables and only use the text
            button.setText(action.title.toString());

            if (action.actionIntent != null) {
                button.setOnClickListener(
                        clickHandlerFactory.getActionClickHandler(statusBarNotification, i));
            }
        }
    }

    /**
     * Sets the text color for action buttons.
     */
    void setActionTextColor(@ColorInt int color) {
        mActionButtons.forEach(button -> button.setTextColor(color));
    }

    /**
     * Resets the notification actions empty for recycling.
     */
    public void reset() {
        for (Button button : mActionButtons) {
            button.setVisibility(View.GONE);
            button.setText(null);
            button.setTextColor(mDefaultTextColor);
            button.setCompoundDrawables(null, null, null, null);
            button.setOnClickListener(null);
        }
    }
}
