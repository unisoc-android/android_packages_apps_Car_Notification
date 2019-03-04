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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationStats;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import androidx.annotation.VisibleForTesting;

import com.android.car.notification.template.BasicNotificationViewHolder;
import com.android.car.notification.template.EmergencyNotificationViewHolder;
import com.android.car.notification.template.InboxNotificationViewHolder;
import com.android.car.notification.template.MessageNotificationViewHolder;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;

import java.util.HashMap;
import java.util.Map;

/**
 * Notification Manager for heads-up notifications in car.
 */
public class CarHeadsUpNotificationManager
        implements CarUxRestrictionsManager.OnUxRestrictionsChangedListener {
    private static CarHeadsUpNotificationManager sManager;
    private final Context mContext;
    private final IStatusBarService mBarService;
    private final boolean mEnableNavigationHeadsup;
    private final long mDuration;
    private final long mMinDisplayDuration;
    private final long mEnterAnimationDuration;
    private final long mAlphaEnterAnimationDuration;
    private final long mExitAnimationDuration;
    private final int mScrimHeightBelowNotification;
    private final KeyguardManager mKeyguardManager;
    private final PreprocessingManager mPreprocessingManager;
    private final WindowManager mWindowManager;
    private final LayoutInflater mInflater;

    private boolean mShouldRestrictMessagePreview;
    private NotificationClickHandlerFactory mClickHandlerFactory;

    // key for the map is the statusbarnotification key
    private final Map<String, HeadsUpEntry> mActiveHeadsUpNotifications;

    @VisibleForTesting
    CarHeadsUpNotificationManager(Context context) {
        mContext = context.getApplicationContext();
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        mEnableNavigationHeadsup =
                context.getResources().getBoolean(R.bool.config_showNavigationHeadsup);
        mDuration = mContext.getResources().getInteger(R.integer.headsup_notification_duration_ms);
        mMinDisplayDuration = mContext.getResources().getInteger(
                R.integer.heads_up_notification_minimum_time);
        mEnterAnimationDuration =
                mContext.getResources().getInteger(R.integer.headsup_total_enter_duration_ms);
        mAlphaEnterAnimationDuration =
                mContext.getResources().getInteger(R.integer.headsup_alpha_enter_duration_ms);
        mExitAnimationDuration =
                mContext.getResources().getInteger(R.integer.headsup_exit_duration_ms);
        mScrimHeightBelowNotification = mContext.getResources().getDimensionPixelOffset(
                R.dimen.headsup_scrim_height_below_notification);
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        mPreprocessingManager = PreprocessingManager.getInstance(context);
        mWindowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mInflater = LayoutInflater.from(mContext);


        mActiveHeadsUpNotifications = new HashMap<>();
    }

    private FrameLayout addNewLayoutForHeadsUp(HeadsUpEntry currentNotification) {
        WindowManager.LayoutParams wrapperParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                // This type allows covering status bar and receiving touch input
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        wrapperParams.gravity = Gravity.TOP;
        int topMargin = mContext.getResources().getDimensionPixelOffset(
                R.dimen.headsup_notification_top_margin);

        FrameLayout wrapper = new FrameLayout(mContext);
        wrapper.setPadding(0, topMargin, 0, 0);
        mWindowManager.addView(wrapper, wrapperParams);

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
        mWindowManager.addView(currentNotification.getScrimView(), scrimParams);
        return wrapper;
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
            HeadsUpEntry currentActiveHeadsUpNotification = mActiveHeadsUpNotifications.get(
                    statusBarNotification.getKey());
            if (currentActiveHeadsUpNotification == null) {
                return;
            }
            if (CarNotificationDiff.sameNotificationKey(
                    currentActiveHeadsUpNotification.getStatusBarNotification(),
                    statusBarNotification)
                    && currentActiveHeadsUpNotification.getHandler().hasMessagesOrCallbacks()) {
                clearViews(statusBarNotification);
            }
            return;
        }
        if (isNew(statusBarNotification) || canUpdate(statusBarNotification)) {
            showHeadsUp(mPreprocessingManager.optimizeForDriving(statusBarNotification));
        }
    }

    /**
     * This method gets called when an app wants to cancel or withdraw its notification.
     */
    public void maybeRemoveHeadsUp(StatusBarNotification statusBarNotification) {
        HeadsUpEntry currentActiveHeadsUpNotification = mActiveHeadsUpNotifications.get(
                statusBarNotification.getKey());
        // if the heads up notification is already removed do nothing.
        if (currentActiveHeadsUpNotification == null) {
            return;
        }

        long totalDisplayDuration =
                System.currentTimeMillis() - currentActiveHeadsUpNotification.getPostTime();
        // ongoing notification that has passed the minimum threshold display time.
        if (totalDisplayDuration >= mMinDisplayDuration) {
            clearViews(statusBarNotification);
            return;
        }

        long earliestRemovalTime = mMinDisplayDuration - totalDisplayDuration;

        currentActiveHeadsUpNotification.getHandler().postDelayed(() ->
                clearViews(statusBarNotification), earliestRemovalTime);
    }

    /**
     * Returns true if the notification's flag is not set to
     * {@link Notification#FLAG_ONLY_ALERT_ONCE}
     */
    private boolean alertAgain(Notification newNotification) {
        return (newNotification.flags & Notification.FLAG_ONLY_ALERT_ONCE) == 0;
    }

    /**
     * Checks if the notification is a new notification or not.
     */
    private boolean isNew(StatusBarNotification statusBarNotification) {
        return mActiveHeadsUpNotifications.get(statusBarNotification.getKey()) == null
                || mActiveHeadsUpNotifications.get(statusBarNotification.getKey()).isNew;
    }

    /**
     * Return true if the currently displaying notification have the same key as the new added
     * notification. In that case it will be considered as an update to the currently displayed
     * notification.
     */
    private boolean isUpdate(StatusBarNotification statusBarNotification) {
        HeadsUpEntry currentActiveHeadsUpNotification = mActiveHeadsUpNotifications.get(
                statusBarNotification.getKey());
        if (currentActiveHeadsUpNotification == null) {
            return false;
        }
        return CarNotificationDiff.sameNotificationKey(
                currentActiveHeadsUpNotification.getStatusBarNotification(),
                statusBarNotification);
    }

    /**
     * Updates only when the notification is being displayed.
     */
    private boolean canUpdate(StatusBarNotification statusBarNotification) {
        HeadsUpEntry currentActiveHeadsUpNotification = mActiveHeadsUpNotifications.get(
                statusBarNotification.getKey());
        return currentActiveHeadsUpNotification != null && System.currentTimeMillis() -
                currentActiveHeadsUpNotification.getPostTime() < mDuration;
    }

    /**
     * Returns the active HeadsUpEntry or creates a new one while adding it to the list of
     * mActiveHeadsUpNotifications.
     */
    private HeadsUpEntry addNewHeadsUpEntry(StatusBarNotification statusBarNotification) {
        HeadsUpEntry currentActiveHeadsUpNotification = mActiveHeadsUpNotifications.get(
                statusBarNotification.getKey());
        if (currentActiveHeadsUpNotification == null) {
            currentActiveHeadsUpNotification = new HeadsUpEntry(statusBarNotification, mContext);
            currentActiveHeadsUpNotification.isNew = true;
            mActiveHeadsUpNotifications.put(statusBarNotification.getKey(),
                    currentActiveHeadsUpNotification);
            return currentActiveHeadsUpNotification;
        }
        // This is a ongoing notification which needs to be alerted again to the user. This
        // requires for the post time to be updated.
        currentActiveHeadsUpNotification.updatePostTime();
        currentActiveHeadsUpNotification.isNew = false;
        currentActiveHeadsUpNotification.isAlertingAgain = true;
        return currentActiveHeadsUpNotification;
    }

    /**
     * Controls three major conditions while showing heads up notification.
     * <p>
     * <ol>
     * <li> When a new HUN comes in it will be displayed with animations
     * <li> If an update to existing HUN comes in which enforces to alert the HUN again to user,
     * then the post time will be updated to current time. This will only be done if {@link
     * Notification#FLAG_ONLY_ALERT_ONCE} flag is not set.
     * <li> If an update to existing HUN comes in which just updates the data and does not want to
     * alert itself again, then the animations will not be shown and the data will get updated. This
     * will only be done if {@link Notification#FLAG_ONLY_ALERT_ONCE} flag is not set.
     * </ol>
     */
    private void showHeadsUp(StatusBarNotification statusBarNotification) {
        if (getWrapper(statusBarNotification) != null) {
            getWrapper(statusBarNotification).removeAllViews();
        }
        // Show animations only when there is no active HUN and notification is new. This check
        // needs to be done here because after this the new notification will be added to the map
        // holding ongoing notifications.
        boolean shouldShowAnimation = !isUpdate(statusBarNotification);
        HeadsUpEntry currentNotification = addNewHeadsUpEntry(statusBarNotification);
        if (isNew(statusBarNotification)) {
            Log.d("testing", "NEW: " + statusBarNotification.getKey());
            FrameLayout currentWrapper = addNewLayoutForHeadsUp(currentNotification);
            currentNotification.setFrameLayout(currentWrapper);
            setAutoDismissViews(currentNotification, statusBarNotification);
        } else if (alertAgain(statusBarNotification.getNotification())) {
            setAutoDismissViews(currentNotification, statusBarNotification);
        }

        currentNotification.setNotificationView(null);
        @NotificationViewType int viewType = getNotificationViewType(statusBarNotification);
        mClickHandlerFactory.setHeadsUpNotificationCallBack(
                new CarHeadsUpNotificationManager.Callback() {
                    @Override
                    public void clearHeadsUpNotification() {
                        clearViews(statusBarNotification);
                    }
                });
        switch (viewType) {
            case NotificationViewType.CAR_EMERGENCY_HEADSUP: {
                currentNotification.setNotificationView(mInflater.inflate(
                        R.layout.car_emergency_headsup_notification_template,
                        getWrapper(statusBarNotification)));
                EmergencyNotificationViewHolder holder =
                        new EmergencyNotificationViewHolder(
                                currentNotification.getNotificationView(),
                                mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
            case NotificationViewType.CAR_WARNING_HEADSUP: {
                currentNotification.setNotificationView(mInflater.inflate(
                        R.layout.car_warning_headsup_notification_template,
                        getWrapper(statusBarNotification)));
                // Using the basic view holder because they share the same view binding logic
                // OEMs should create view holders if needed
                BasicNotificationViewHolder holder =
                        new BasicNotificationViewHolder(currentNotification.getNotificationView(),
                                mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
            case NotificationViewType.CAR_INFORMATION_HEADSUP: {
                currentNotification.setNotificationView(mInflater.inflate(
                        R.layout.car_information_headsup_notification_template,
                        getWrapper(statusBarNotification)));
                // Using the basic view holder because they share the same view binding logic
                // OEMs should create view holders if needed
                BasicNotificationViewHolder holder =
                        new BasicNotificationViewHolder(currentNotification.getNotificationView(),
                                mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
            case NotificationViewType.MESSAGE_HEADSUP: {
                currentNotification.setNotificationView(mInflater.inflate(
                        R.layout.message_headsup_notification_template,
                        getWrapper(statusBarNotification)));
                MessageNotificationViewHolder holder =
                        new MessageNotificationViewHolder(currentNotification.getNotificationView(),
                                mClickHandlerFactory);
                if (mShouldRestrictMessagePreview) {
                    holder.bindRestricted(statusBarNotification, /* isInGroup= */ false);
                } else {
                    holder.bind(statusBarNotification, /* isInGroup= */ false);
                }
                break;
            }
            case NotificationViewType.INBOX_HEADSUP: {
                currentNotification.setNotificationView(mInflater.inflate(
                        R.layout.inbox_headsup_notification_template,
                        getWrapper(statusBarNotification)));
                InboxNotificationViewHolder holder =
                        new InboxNotificationViewHolder(currentNotification.getNotificationView(),
                                mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
            case NotificationViewType.BASIC_HEADSUP:
            default: {
                currentNotification.setNotificationView(mInflater.inflate(
                        R.layout.basic_headsup_notification_template,
                        getWrapper(statusBarNotification)));
                BasicNotificationViewHolder holder =
                        new BasicNotificationViewHolder(currentNotification.getNotificationView(),
                                mClickHandlerFactory);
                holder.bind(statusBarNotification, /* isInGroup= */ false);
                break;
            }
        }

        // Get the height of the notification view after onLayout()
        // in order to set the height of the scrim view and do animations
        currentNotification.getNotificationView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int notificationHeight =
                                currentNotification.getNotificationView().getHeight();
                        WindowManager.LayoutParams scrimParams =
                                (WindowManager.LayoutParams) currentNotification.getScrimView().getLayoutParams();
                        scrimParams.height = notificationHeight + mScrimHeightBelowNotification;
                        mWindowManager.updateViewLayout(currentNotification.getScrimView(),
                                scrimParams);

                        if (shouldShowAnimation) {
                            currentNotification.getScrimView().setY(
                                    0 - notificationHeight - mScrimHeightBelowNotification);
                            currentNotification.getNotificationView().setY(0 - notificationHeight);
                            currentNotification.getNotificationView().setAlpha(0f);

                            Interpolator yPositionInterpolator = AnimationUtils.loadInterpolator(
                                    mContext,
                                    R.interpolator.heads_up_entry_direction_interpolator);
                            Interpolator alphaInterpolator = AnimationUtils.loadInterpolator(
                                    mContext,
                                    R.interpolator.heads_up_entry_alpha_interpolator);

                            ObjectAnimator moveY = ObjectAnimator.ofFloat(
                                    currentNotification.getNotificationView(), "y", 0f);
                            moveY.setDuration(mEnterAnimationDuration);
                            moveY.setInterpolator(yPositionInterpolator);

                            ObjectAnimator alpha = ObjectAnimator.ofFloat(
                                    currentNotification.getNotificationView(), "alpha", 1f);
                            alpha.setDuration(mAlphaEnterAnimationDuration);
                            alpha.setInterpolator(alphaInterpolator);

                            AnimatorSet animatorSet = new AnimatorSet();
                            animatorSet.playTogether(moveY, alpha);
                            animatorSet.start();

                            currentNotification.getScrimView().setVisibility(View.VISIBLE);
                            currentNotification.getScrimView().animate()
                                    .y(0f)
                                    .setDuration(mEnterAnimationDuration);
                        }
                        currentNotification.getNotificationView().getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                    }
                });

        // Add swipe gesture
        View columnCardView = currentNotification.getNotificationView().findViewById(
                R.id.column_card_view);
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
                                        notificationVisibility
                                );

                            } catch (RemoteException e) {
                                throw e.rethrowFromSystemServer();
                            }
                            clearViews(statusBarNotification);
                        }));
    }

    @VisibleForTesting
    View getNotificationView(HeadsUpEntry currentNotification) {
        return currentNotification == null ? null : currentNotification.getNotificationView();
    }

    @VisibleForTesting
    protected Map<String, HeadsUpEntry> getActiveHeadsUpNotifications() {
        return mActiveHeadsUpNotifications;
    }

    private void setAutoDismissViews(HeadsUpEntry currentNotification,
            StatusBarNotification statusBarNotification) {
        currentNotification.getHandler().removeCallbacksAndMessages(null);
        currentNotification.getHandler().postDelayed(() -> clearViews(statusBarNotification),
                mDuration);
    }

    /**
     * Returns the {@link FrameLayout} related to the currently displaying Heads Up Notification.
     */
    private FrameLayout getWrapper(StatusBarNotification statusBarNotification) {
        HeadsUpEntry currentHeadsUpNotification = mActiveHeadsUpNotifications.get(
                statusBarNotification.getKey());
        if (currentHeadsUpNotification != null) {
            return currentHeadsUpNotification.getFrameLayout();
        }
        return null;
    }

    private void clearViews(StatusBarNotification statusBarNotification) {
        HeadsUpEntry currentHeadsUpNotification = mActiveHeadsUpNotifications.get(
                statusBarNotification.getKey());
        Interpolator exitInterpolator = AnimationUtils.loadInterpolator(mContext,
                R.interpolator.heads_up_exit_direction_interpolator);
        Interpolator alphaInterpolator = AnimationUtils.loadInterpolator(mContext,
                R.interpolator.heads_up_exit_alpha_interpolator);

        ObjectAnimator moveY = ObjectAnimator.ofFloat(
                currentHeadsUpNotification.getNotificationView(), "y",
                -1 * currentHeadsUpNotification.getNotificationView().getHeight());
        moveY.setDuration(mExitAnimationDuration);
        moveY.setInterpolator(exitInterpolator);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(
                currentHeadsUpNotification.getNotificationView(), "alpha", 1f);
        alpha.setDuration(mExitAnimationDuration);
        alpha.setInterpolator(alphaInterpolator);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(moveY, alpha);
        animatorSet.start();

        currentHeadsUpNotification.getScrimView().animate()
                .y((currentHeadsUpNotification.getNotificationView().getHeight()
                        - mScrimHeightBelowNotification) * -1)
                .setDuration(mExitAnimationDuration)
                .withEndAction(() -> {
                    currentHeadsUpNotification.getScrimView().setVisibility(View.GONE);
                    currentHeadsUpNotification.getHandler().removeCallbacksAndMessages(
                            null);
                    getWrapper(statusBarNotification).removeAllViews();
                    mWindowManager.removeView(currentHeadsUpNotification.getFrameLayout());
                    mActiveHeadsUpNotifications.remove(statusBarNotification.getKey());
                });
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
     * <li> OEMs configured CATEGORY_NAVIGATION should not be shown
     * </ul>
     *
     * <p> A notification will be shown as a heads-up if:
     * <ul>
     * <li> Importance >= HIGH
     * </ul>
     *
     * <p> Group alert behavior still follows API documentation.
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

        // Navigation notification configured by OEM
        if (!mEnableNavigationHeadsup && Notification.CATEGORY_NAVIGATION.equals(
                statusBarNotification.getNotification().category)) {
            return false;
        }
        // Group alert behavior
        if (notification.suppressAlertingDueToGrouping()) {
            return false;
        }
        // Show if importance >= HIGH
        NotificationListenerService.Ranking ranking = getRanking();
        if (rankingMap.getRanking(statusBarNotification.getKey(), ranking)) {
            if (ranking.getImportance() >= NotificationManager.IMPORTANCE_HIGH) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    protected NotificationListenerService.Ranking getRanking() {
        return new NotificationListenerService.Ranking();
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
    @VisibleForTesting
    protected void setClickHandlerFactory(NotificationClickHandlerFactory clickHandlerFactory) {
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
