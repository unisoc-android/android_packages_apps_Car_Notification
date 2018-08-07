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
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Notification Manager for heads-up notifications in car.
 */
public class CarHeadsUpNotificationManager {
    private static CarHeadsUpNotificationManager sManager;
    private final Context mContext;
    private final WindowManager mWindowManager;
    private final LayoutInflater mInflater;
    private final Handler mTimer;
    private final View mScrimView;
    private final FrameLayout mWrapper;
    private final long mDuration;
    private final long mEnterAnimationDuration;
    private final int mScrimHeightBelowNotification;

    private CarHeadsUpNotificationManager(Context context) {
        mContext = context.getApplicationContext();
        mWindowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mInflater = LayoutInflater.from(mContext);
        mDuration = mContext.getResources().getInteger(R.integer.headsup_notification_duration_ms);
        mEnterAnimationDuration =
                mContext.getResources().getInteger(R.integer.headsup_enter_duration_ms);
        mScrimHeightBelowNotification = mContext.getResources().getDimensionPixelOffset(
                R.dimen.headsup_scrim_height_below_notification);
        mTimer = new Handler();

        // The reason we are adding the gradient scrim as its own window is because
        // we want the touch events to work for notifications, but not the gradient scrim.
        WindowManager.LayoutParams scrimParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                // This type allows covering status bar but not receiving touch input
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        scrimParams.gravity = Gravity.TOP;
        mScrimView = new View(mContext);
        mScrimView.setBackgroundResource(R.drawable.headsup_scrim);
        mScrimView.setVisibility(View.GONE);
        mWindowManager.addView(mScrimView, scrimParams);

        WindowManager.LayoutParams wrapperParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                // This type allows covering status bar and receiving touch input
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        wrapperParams.gravity = Gravity.TOP;
        mWrapper = new FrameLayout(mContext);
        mWrapper.setPadding(
                0, mContext.getResources().getDimensionPixelOffset(R.dimen.car_padding_1), 0, 0);
        mWindowManager.addView(mWrapper, wrapperParams);
    }

    /**
     * Show the notification as a heads-up if it meets the criteria.
     */
    public void maybeShowHeadsUp(
            boolean isDistractionOptimizationRequired,
            StatusBarNotification statusBarNotification,
            NotificationListenerService.RankingMap rankingMap) {
        if (!shouldShowHeadsUp(statusBarNotification, rankingMap)) {
            return;
        }
        showHeadsUp(statusBarNotification);
    }

    private void showHeadsUp(StatusBarNotification statusBarNotification) {
        // Remove previous heads-up notifications immediately as well as the previous timer
        mWrapper.removeAllViews();
        mTimer.removeCallbacksAndMessages(null);

        View notificationView;
        @NotificationViewType int viewType = getNotificationViewType(statusBarNotification);
        switch (viewType) {
            case NotificationViewType.MESSAGE_HEADSUP: {
                notificationView = mInflater.inflate(
                        R.layout.message_headsup_notification_template, mWrapper);
                MessageNotificationViewHolder holder =
                        new MessageNotificationViewHolder(notificationView);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
            case NotificationViewType.BASIC_HEADSUP:
            default: {
                notificationView = mInflater.inflate(
                        R.layout.basic_headsup_notification_template, mWrapper);
                BasicNotificationViewHolder holder =
                        new BasicNotificationViewHolder(notificationView);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
        }
        // To prevent the default animation from showing
        // setting the notification view as invisible until the animator actually kicks in
        // TODO: Remove the default animation
        notificationView.setVisibility(View.INVISIBLE);

        // Get the height of the notification view after onLayout()
        // in order to set the height of the scrim view and do animations
        notificationView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int notificationHeight = notificationView.getHeight();
                        mScrimView.setY(0 - notificationHeight - mScrimHeightBelowNotification);
                        notificationView.setY(0 - notificationHeight);

                        notificationView.setVisibility(View.VISIBLE);
                        notificationView.animate()
                                .y(0f)
                                .setDuration(mEnterAnimationDuration);

                        WindowManager.LayoutParams scrimParams =
                                (WindowManager.LayoutParams) mScrimView.getLayoutParams();
                        scrimParams.height = notificationHeight + mScrimHeightBelowNotification;
                        mWindowManager.updateViewLayout(mScrimView, scrimParams);

                        mScrimView.setVisibility(View.VISIBLE);
                        mScrimView.animate()
                                .y(0f)
                                .setDuration(mEnterAnimationDuration);

                        notificationView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        // Remove heads-up notifications after a timer
        mTimer.postDelayed(() -> {
            mScrimView.setVisibility(View.GONE);
            mWrapper.removeAllViews();
        }, mDuration);
    }

    /**
     * Choose a correct notification layout for this heads-up notification.
     * Note that the layout chosen can be different for the same notification
     * in the notification center.
     */
    @NotificationViewType
    private static int getNotificationViewType(StatusBarNotification statusBarNotification) {
        String category = statusBarNotification.getNotification().category;
        if (Notification.CATEGORY_MESSAGE.equals(category)) {
            return NotificationViewType.MESSAGE_HEADSUP;
        }
        return NotificationViewType.BASIC_HEADSUP;
    }

    /**
     * Helper method that determines whether a notification should show as a heads-up.
     *
     * <p> A notification will never be shown as a heads-up if:
     * Is ongoing
     *
     * <p> A non-ongoing notification will be shown as a heads-up if:
     * Importance >= HIGH
     * or, Category in {CAR_EMERGENCY, CAR_WARNING}
     *
     * @return true if a notification should be shown as a heads-up
     */
    private static boolean shouldShowHeadsUp(
            StatusBarNotification statusBarNotification,
            NotificationListenerService.RankingMap rankingMap) {
        // Don't show persistent notifications
        if (statusBarNotification.isOngoing()) {
            return false;
        }

        // Don't show group summary notification
        if (statusBarNotification.getNotification().isGroupSummary()) {
            return false;
        }

        // Show if importance >= HIGH
        NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
        if (rankingMap.getRanking(statusBarNotification.getKey(), ranking)) {
            if (ranking.getImportance() >= NotificationManager.IMPORTANCE_HIGH) {
                return true;
            }
        }

        // Show if category in {CAR_EMERGENCY, CAR_WARNING}
        String category = statusBarNotification.getNotification().category;
        if (Notification.CATEGORY_CAR_EMERGENCY.equals(category)
                || Notification.CATEGORY_CAR_WARNING.equals(category)) {
            return true;
        }

        return false;
    }

    /**
     * Get CarHeadsUpNotificationManager instance.
     */
    public static CarHeadsUpNotificationManager getInstance(Context context) {
        if (sManager == null) {
            sManager = new CarHeadsUpNotificationManager(context);
        }
        return sManager;
    }
}
