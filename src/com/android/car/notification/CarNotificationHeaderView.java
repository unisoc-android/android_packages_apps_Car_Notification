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
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
/**
 * Notification header view that contains the issuer app icon and name, and extra information.
 */
public class CarNotificationHeaderView extends LinearLayout {

    private static final String TAG = "car_notification_header";

    private ImageView mIconView;

    public CarNotificationHeaderView(Context context) {
        super(context);
        init(context);
    }

    public CarNotificationHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CarNotificationHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CarNotificationHeaderView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.car_notification_header_view, this);
    }

    @Override
    protected void onFinishInflate() {
        mIconView = findViewById(R.id.app_icon);
    }

    /**
     * Binds the notification header that contains the issuer app icon and name.
     *
     * @param statusBarNotification the notification to be bound.
     * @param primaryColor the foreground color used for the small icon.
     *                     Passing {@code null} will use the default colors.
     */
    public void bind(StatusBarNotification statusBarNotification, @Nullable Integer primaryColor) {
        setVisibility(View.VISIBLE);

        mIconView.setVisibility(View.VISIBLE);
        Drawable drawable =
                statusBarNotification.getNotification().getSmallIcon().loadDrawable(getContext());
        mIconView.setImageDrawable(drawable);
        if (primaryColor != null) {
            mIconView.setColorFilter(primaryColor);
        }
    }

    /**
     * Resets the notification header empty for recycling.
     */
    public void reset() {
        setVisibility(View.GONE);

        mIconView.setVisibility(View.GONE);
        mIconView.setImageDrawable(null);
    }
}
