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

import android.annotation.CallSuper;
import android.annotation.Nullable;
import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.android.car.notification.NotificationClickHandlerFactory;
import com.android.car.notification.R;

/**
 * The base view holder class that all template view holders should extend.
 */
public abstract class CarNotificationBaseViewHolder extends RecyclerView.ViewHolder {
    private final NotificationClickHandlerFactory mClickHandlerFactory;

    /** Nullable for {@link GroupSummaryNotificationViewHolder}. */
    @Nullable
    private final View mCardView;

    private StatusBarNotification mStatusBarNotification;

    private boolean mIsAnimating;

    CarNotificationBaseViewHolder(
            View itemView, NotificationClickHandlerFactory clickHandlerFactory) {
        super(itemView);
        mClickHandlerFactory = clickHandlerFactory;
        mCardView = itemView.findViewById(R.id.column_card_view);
    }

    /**
     * Binds a {@link StatusBarNotification} to a notification template. Base class sets the
     * clicking event for the card view and calls recycling methods.
     *
     * @param statusBarNotification the notification to be bound.
     * @param isInGroup             whether this notification is part of a grouped notification.
     */
    @CallSuper
    public void bind(StatusBarNotification statusBarNotification, boolean isInGroup) {
        reset();
        mStatusBarNotification = statusBarNotification;

        if (mCardView != null) {
            mCardView.setOnClickListener(
                    mClickHandlerFactory.getClickHandler(mStatusBarNotification));
        }
    }

    /**
     * Child view holders should override and call super to recycle any custom component
     * that's not handled by {@link CarNotificationHeaderView}, {@link CarNotificationBodyView} and
     * {@link CarNotificationActionsView}.
     * Note that any child class that is not calling {@link #bind} has to call this method.
     */
    @CallSuper
    void reset() {
        itemView.setTranslationX(0);
        itemView.setAlpha(1f);

        if (mCardView != null) {
            mCardView.setOnClickListener(null);
        }
    }

    /**
     * Returns the current {@link StatusBarNotification} that this view holder is holding.
     * Note that any child class that is not calling {@link #bind} has to override this method.
     */
    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }

    /**
     * Returns true if the notification contained in this view holder can be swiped away.
     */
    public boolean isDismissible() {
        if (mStatusBarNotification == null) {
            return true;
        }

        return (mStatusBarNotification.getNotification().flags
                & (Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_ONGOING_EVENT)) == 0;
    }

    /**
     * Returns the TranslationX of the ItemView.
     */
    public float getSwipeTranslationX() {
        return itemView.getTranslationX();
    }

    /**
     * Sets the TranslationX of the ItemView.
     */
    public void setSwipeTranslationX(float translationX) {
        itemView.setTranslationX(translationX);
    }

    /**
     * Sets the alpha of the ItemView.
     */
    public void setSwipeAlpha(float alpha) {
        itemView.setAlpha(alpha);
    }

    /**
     * Sets whether this view holder has ongoing animation.
     */
    public void setIsAnimating(boolean animating) {
        mIsAnimating = animating;
    }

    /**
     * Returns true if this view holder has ongoing animation.
     */
    public boolean isAnimating() {
        return mIsAnimating;
    }
}
