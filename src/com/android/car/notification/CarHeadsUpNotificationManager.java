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

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationStats;
import android.service.notification.StatusBarNotification;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.VisibleForTesting;

import com.android.car.notification.template.BasicNotificationViewHolder;
import com.android.car.notification.template.EmergencyNotificationViewHolder;
import com.android.car.notification.template.InboxNotificationViewHolder;
import com.android.car.notification.template.MessageNotificationViewHolder;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;

/**
 * Notification Manager for heads-up notifications in car.
 */
public class CarHeadsUpNotificationManager
        implements CarUxRestrictionsManager.OnUxRestrictionsChangedListener {
    private static CarHeadsUpNotificationManager sManager;
    private final Context mContext;
    private final IStatusBarService mBarService;
    private final boolean mEnableMediaNotification;
    private final boolean mEnableOngoingNotification;
    private final long mDuration;
    private final long mEnterAnimationDuration;
    private final int mScrimHeightBelowNotification;
    private final KeyguardManager mKeyguardManager;
    private final PreprocessingManager mPreprocessingManager;
    private final WindowManager mWindowManager;
    private final LayoutInflater mInflater;
    private final Handler mHandler;
    private final View mScrimView;
    private final FrameLayout mWrapper;
    private StatusBarNotification mCurrentNotification;
    private boolean mShouldRestrictMessagePreview;
    private NotificationClickHandlerFactory mClickHandlerFactory;
    private long mPostedTimeStampMs;
    private View mNotificationView;

    @VisibleForTesting
    CarHeadsUpNotificationManager(Context context) {
        mContext = context.getApplicationContext();
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        mEnableMediaNotification =
                context.getResources().getBoolean(R.bool.config_showMediaNotification);
        mEnableOngoingNotification =
                context.getResources().getBoolean(R.bool.config_showOngoingNotification);
        mDuration = mContext.getResources().getInteger(R.integer.headsup_notification_duration_ms);
        mEnterAnimationDuration =
                mContext.getResources().getInteger(R.integer.headsup_enter_duration_ms);
        mScrimHeightBelowNotification = mContext.getResources().getDimensionPixelOffset(
                R.dimen.headsup_scrim_height_below_notification);
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        mPreprocessingManager = PreprocessingManager.getInstance(context);
        mWindowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mInflater = LayoutInflater.from(mContext);
        mHandler = new Handler();

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

        int topMargin = mContext.getResources().getDimensionPixelOffset(
                R.dimen.headsup_notification_top_margin);
        WindowManager.LayoutParams wrapperParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                // This type allows covering status bar and receiving touch input
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        wrapperParams.gravity = Gravity.TOP;
        mWrapper = new FrameLayout(mContext);
        mWrapper.setPadding(0, topMargin, 0, 0);
        mWindowManager.addView(mWrapper, wrapperParams);
    }

    /**
     * Show the notification as a heads-up if it meets the criteria.
     */
    public void maybeShowHeadsUp(
            StatusBarNotification statusBarNotification,
            NotificationListenerService.RankingMap rankingMap) {
        if (!shouldShowHeadsUp(statusBarNotification, rankingMap)) {
            // check if this is a update to the existing notification and if it should still show
            // as a heads up or not.
            if (CarNotificationDiff.sameNotificationKey(mCurrentNotification, statusBarNotification)
                    && mHandler.hasMessagesOrCallbacks()) {
                clearViews();
            }
            return;
        }
        boolean alertAgain = alertAgain(statusBarNotification.getNotification());
        if (alertAgain || canUpdate()) {
            showHeadsUp(mPreprocessingManager.optimizeForDriving(statusBarNotification));
        }
    }

    /**
     * Returns true if the notification's flag is not set to
     * {@link Notification#FLAG_ONLY_ALERT_ONCE}
     */
    private boolean alertAgain(Notification newNotification) {
        return (newNotification.flags & Notification.FLAG_ONLY_ALERT_ONCE) == 0;
    }

    /**
     * Return true if the currently displaying notification have the same key as the new added
     * notification. In that case is will be considered as an update to the currently displayed
     * notification.
     */
    private boolean isUpdate(StatusBarNotification statusBarNotification) {
        return CarNotificationDiff.sameNotificationKey(mCurrentNotification,
                statusBarNotification);
    }

    /**
     * Updates only when the notification is being displayed.
     */
    private boolean canUpdate() {
        return System.currentTimeMillis() - mPostedTimeStampMs < mDuration;
    }

    private void showHeadsUp(StatusBarNotification statusBarNotification) {
        // Remove previous heads-up notifications immediately
        mWrapper.removeAllViews();
        mCurrentNotification = statusBarNotification;
        if (alertAgain(statusBarNotification.getNotification())) {
            mPostedTimeStampMs = System.currentTimeMillis();
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(() -> clearViews(), mDuration);
        }
        mNotificationView = null;
        @NotificationViewType int viewType = getNotificationViewType(statusBarNotification);
        mClickHandlerFactory.setHeadsUpNotificationCallBack(
                new CarHeadsUpNotificationManager.Callback() {
                    @Override
                    public void clearHeadsUpNotification() {
                        clearViews();
                    }
                });
        switch (viewType) {
            case NotificationViewType.CAR_EMERGENCY_HEADSUP: {
                mNotificationView = mInflater.inflate(
                        R.layout.car_emergency_headsup_notification_template, mWrapper);
                EmergencyNotificationViewHolder holder =
                        new EmergencyNotificationViewHolder(mNotificationView,
                                mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
            case NotificationViewType.CAR_WARNING_HEADSUP: {
                mNotificationView = mInflater.inflate(
                        R.layout.car_warning_headsup_notification_template, mWrapper);
                // Using the basic view holder because they share the same view binding logic
                // OEMs should create view holders if needed
                BasicNotificationViewHolder holder =
                        new BasicNotificationViewHolder(mNotificationView, mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
            case NotificationViewType.CAR_INFORMATION_HEADSUP: {
                mNotificationView = mInflater.inflate(
                        R.layout.car_information_headsup_notification_template, mWrapper);
                // Using the basic view holder because they share the same view binding logic
                // OEMs should create view holders if needed
                BasicNotificationViewHolder holder =
                        new BasicNotificationViewHolder(mNotificationView, mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
            case NotificationViewType.MESSAGE_HEADSUP: {
                mNotificationView = mInflater.inflate(
                        R.layout.message_headsup_notification_template, mWrapper);
                MessageNotificationViewHolder holder =
                        new MessageNotificationViewHolder(mNotificationView, mClickHandlerFactory);
                if (mShouldRestrictMessagePreview) {
                    holder.bindRestricted(statusBarNotification, /* isInGroup= */ false);
                } else {
                    holder.bind(statusBarNotification, /* isInGroup= */ false);
                }
                break;
            }
            case NotificationViewType.INBOX_HEADSUP: {
                mNotificationView = mInflater.inflate(
                        R.layout.inbox_headsup_notification_template, mWrapper);
                InboxNotificationViewHolder holder =
                        new InboxNotificationViewHolder(mNotificationView, mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
            case NotificationViewType.BASIC_HEADSUP:
            default: {
                mNotificationView = mInflater.inflate(
                        R.layout.basic_headsup_notification_template, mWrapper);
                BasicNotificationViewHolder holder =
                        new BasicNotificationViewHolder(mNotificationView, mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
        }

        // Get the height of the notification view after onLayout()
        // in order to set the height of the scrim view and do animations
        mNotificationView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int notificationHeight = mNotificationView.getHeight();
                        WindowManager.LayoutParams scrimParams =
                                (WindowManager.LayoutParams) mScrimView.getLayoutParams();
                        scrimParams.height = notificationHeight + mScrimHeightBelowNotification;
                        mWindowManager.updateViewLayout(mScrimView, scrimParams);

                        // Show animations only when there is no active HUN and notification is new.
                        boolean shouldShowAnimation = !isUpdate(statusBarNotification);
                        if (shouldShowAnimation) {
                            mScrimView.setY(0 - notificationHeight - mScrimHeightBelowNotification);
                            mNotificationView.setY(0 - notificationHeight);

                            mNotificationView.animate()
                                    .y(0f)
                                    .setDuration(mEnterAnimationDuration);

                            mScrimView.setVisibility(View.VISIBLE);
                            mScrimView.animate()
                                    .y(0f)
                                    .setDuration(mEnterAnimationDuration);
                        }
                        mNotificationView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        // Add swipe gesture
        View columnCardView = mNotificationView.findViewById(R.id.column_card_view);
        columnCardView.setOnTouchListener(
                new HeadsUpNotificationOnTouchListener(columnCardView,
                        () -> {
                            boolean isDismissible = (statusBarNotification.getNotification().flags
                                    & (Notification.FLAG_FOREGROUND_SERVICE
                                    | Notification.FLAG_ONGOING_EVENT)) == 0;
                            if (!isDismissible) {
                                return;
                            }
                            try {
                                // rank and count is used for logging and is not need at this
                                // time thus -1
                                NotificationVisibility notificationVisibility =
                                        NotificationVisibility.obtain(
                                                statusBarNotification.getKey(),
                                                /* rank= */ -1,
                                                /* count= */ -1,
                                                /* visible= */ true);
                                mBarService.onNotificationClear(
                                        statusBarNotification.getPackageName(),
                                        statusBarNotification.getTag(),
                                        statusBarNotification.getId(),
                                        statusBarNotification.getUser().getIdentifier(),
                                        statusBarNotification.getKey(),
                                        NotificationStats.DISMISSAL_SHADE,
                                        NotificationStats.DISMISS_SENTIMENT_NEUTRAL,
                                        notificationVisibility
                                );

                            } catch (RemoteException e) {
                                throw e.rethrowFromSystemServer();
                            }
                            clearViews();
                            mHandler.removeCallbacksAndMessages(null);
                        }));
    }

    @VisibleForTesting
    View getNotificationView() {
        return mNotificationView;
    }

    private void clearViews() {
        mHandler.removeCallbacksAndMessages(null);
        mScrimView.setVisibility(View.GONE);
        mWrapper.removeAllViews();
    }

    /**
     * Choose a correct notification layout for this heads-up notification.
     * Note that the layout chosen can be different for the same notification
     * in the notification center.
     */
    @NotificationViewType
    private static int getNotificationViewType(StatusBarNotification statusBarNotification) {
        String category = statusBarNotification.getNotification().category;
        if (category != null) {
            switch (category) {
                case Notification.CATEGORY_CAR_EMERGENCY:
                    return NotificationViewType.CAR_EMERGENCY_HEADSUP;
                case Notification.CATEGORY_CAR_WARNING:
                    return NotificationViewType.CAR_WARNING_HEADSUP;
                case Notification.CATEGORY_CAR_INFORMATION:
                    return NotificationViewType.CAR_INFORMATION_HEADSUP;
                case Notification.CATEGORY_MESSAGE:
                    return NotificationViewType.MESSAGE_HEADSUP;
                default:
                    break;
            }
        }
        Bundle extras = statusBarNotification.getNotification().extras;
        if (extras.containsKey(Notification.EXTRA_BIG_TEXT)
                && extras.containsKey(Notification.EXTRA_SUMMARY_TEXT)) {
            return NotificationViewType.INBOX_HEADSUP;
        }
        // progress, media, big text, big picture, and basic templates
        return NotificationViewType.BASIC_HEADSUP;
    }

    /**
     * Helper method that determines whether a notification should show as a heads-up.
     *
     * <p> A notification will never be shown as a heads-up if:
     * <ul>
     * <li> Keyguard (lock screen) is showing
     * <li> Is ongoing
     * </ul>
     *
     * <p> A non-ongoing notification will be shown as a heads-up if:
     * <ul>
     * <li> Importance >= HIGH
     * <li> Category in {CAR_EMERGENCY, CAR_WARNING}
     * </ul>
     *
     * @return true if a notification should be shown as a heads-up
     */
    private boolean shouldShowHeadsUp(
            StatusBarNotification statusBarNotification,
            NotificationListenerService.RankingMap rankingMap) {
        if (mKeyguardManager.isKeyguardLocked()) {
            return false;
        }
        Notification notification = statusBarNotification.getNotification();

        // Media notification configured by OEM
        if (!mEnableMediaNotification
                && Notification.CATEGORY_TRANSPORT.equals(
                statusBarNotification.getNotification().category)) {
            return false;
        }
        // Ongoing notification configured by OEM
        if (!mEnableOngoingNotification && statusBarNotification.isOngoing()) {
            return false;
        }
        // Group alert behavior
        if (notification.suppressAlertingDueToGrouping()) {
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
        String category = notification.category;
        if (Notification.CATEGORY_CAR_EMERGENCY.equals(category)
                || Notification.CATEGORY_CAR_WARNING.equals(category)) {
            return true;
        }
        return false;
    }

    /**
     * Gets CarHeadsUpNotificationManager instance.
     *
     * @param context The {@link Context} of the application
     * @param clickHandlerFactory used to generate onClickListeners
     */
    public static CarHeadsUpNotificationManager getInstance(Context context,
            NotificationClickHandlerFactory clickHandlerFactory) {
        if (sManager == null) {
            sManager = new CarHeadsUpNotificationManager(context);
        }
        sManager.setClickHandlerFactory(clickHandlerFactory);
        return sManager;
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictions) {
        mShouldRestrictMessagePreview =
                (restrictions.getActiveRestrictions()
                        & CarUxRestrictions.UX_RESTRICTIONS_NO_TEXT_MESSAGE) != 0;
    }

    /**
     * Sets the source of {@link View.OnClickListener}
     *
     * @param clickHandlerFactory used to generate onClickListeners
     */
    private void setClickHandlerFactory(NotificationClickHandlerFactory clickHandlerFactory) {
        mClickHandlerFactory = clickHandlerFactory;
    }

    /**
     * Callback that will be issued after a Heads up notification is clicked
     */
    public interface Callback {
        /**
         * Clears Heads up notification on click.
         */
        void clearHeadsUpNotification();
    }
}
