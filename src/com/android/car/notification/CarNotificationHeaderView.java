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
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Notification header view that contains the issuer app icon and name, and extra information.
 */
public class CarNotificationHeaderView extends LinearLayout {

    private static final String TAG = "car_notification_header";

    private PackageManager mPackageManager;
    private ImageView mIconView;
    private TextView mHeaderTextView;
    private int mDefaultTextColor;
    private String mSeparatorText;

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
        mPackageManager = context.getPackageManager();
        mDefaultTextColor = context.getColor(R.color.header_text_color);
        mSeparatorText = context.getString(R.string.header_text_separator);
        inflate(context, R.layout.car_notification_header_view, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIconView = findViewById(R.id.app_icon);
        mHeaderTextView = findViewById(R.id.header_text);
    }

    /**
     * Binds the notification header that contains the issuer app icon and name.
     *
     * @param statusBarNotification the notification to be bound.
     * @param primaryColor          the foreground color used for the small icon.
     *                              Passing {@code null} will use the default colors.
     */
    public void bind(StatusBarNotification statusBarNotification, @Nullable Integer primaryColor) {
        reset();
        setVisibility(View.VISIBLE);
        Notification notification = statusBarNotification.getNotification();
        Context packageContext = statusBarNotification.getPackageContext(getContext());

        // app icon
        mIconView.setVisibility(View.VISIBLE);
        Drawable drawable = notification.getSmallIcon().loadDrawable(packageContext);
        mIconView.setImageDrawable(drawable);

        StringBuilder stringBuilder = new StringBuilder();

        // app name
        mHeaderTextView.setVisibility(View.VISIBLE);
        stringBuilder.append(loadHeaderAppName(statusBarNotification.getPackageName()));

        Bundle extras = notification.extras;

        // optional field: sub text
        if (!TextUtils.isEmpty(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))) {
            stringBuilder.append(mSeparatorText);
            stringBuilder.append(extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        }

        // optional field: content info
        if (!TextUtils.isEmpty(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))) {
            stringBuilder.append(mSeparatorText);
            stringBuilder.append(extras.getCharSequence(Notification.EXTRA_INFO_TEXT));
        }

        // optional field: time
        if (extras.getBoolean(Notification.EXTRA_SHOW_WHEN, false)) {
            stringBuilder.append(mSeparatorText);
            CharSequence dateString = DateUtils.getRelativeDateTimeString(
                    getContext(),
                    statusBarNotification.getPostTime(),
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL);
            stringBuilder.append(dateString);
        }

        // optional color
        if (primaryColor != null) {
            mIconView.setColorFilter(primaryColor);
            mHeaderTextView.setTextColor(primaryColor);
        }

        mHeaderTextView.setText(stringBuilder);
    }

    /**
     * Resets the notification header empty for recycling.
     */
    private void reset() {
        setVisibility(View.GONE);

        mIconView.setVisibility(View.GONE);
        mIconView.setImageDrawable(null);
        mIconView.setColorFilter(mDefaultTextColor);

        mHeaderTextView.setVisibility(View.GONE);
        mHeaderTextView.setText(null);
        mHeaderTextView.setTextColor(mDefaultTextColor);
    }

    /**
     * Fetches the application label given the package name.
     *
     * @param packageName The package name of the application.
     * @return application label. Returns {@code null} when application name is not found.
     */
    @Nullable
    private String loadHeaderAppName(String packageName) {
        ApplicationInfo info;
        try {
            info = mPackageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error fetching app name in car notification header");
            return null;
        }
        return String.valueOf(mPackageManager.getApplicationLabel(info));
    }
}
