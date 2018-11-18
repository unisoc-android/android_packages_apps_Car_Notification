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

/**
 * The base view holder class that all template view holders should extend.
 */
public abstract class CarNotificationBaseViewHolder extends RecyclerView.ViewHolder {
    private boolean mIsAnimating;

    CarNotificationBaseViewHolder(View itemView) {
        super(itemView);
    }

    /**
     * Resets the notification view empty for recycling. Child classes should call super to ensure
     * view being reset.
     */
    @CallSuper
    void reset() {
        itemView.setTranslationX(0);
        itemView.setAlpha(1f);
    }

    /**
     * Binds a {@link StatusBarNotification} to a notification template.
     *
     * @param statusBarNotification the notification to be bound.
     * @param isInGroup whether this notification is part of a grouped notification.
     */
    public abstract void bind(StatusBarNotification statusBarNotification, boolean isInGroup);

    /**
     * Returns true if the notification contained in this view holder can be swiped away.
     */
    public boolean isDismissible() {
        if (getStatusBarNotification() == null) {
            return false;
        }

        StatusBarNotification statusBarNotification = getStatusBarNotification();

        return (statusBarNotification.getNotification().flags
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

    /**
     * Abstract method that the child class should implement that returns the current
     * {@link StatusBarNotification} that it is holding.
     */
    @Nullable
    public abstract StatusBarNotification getStatusBarNotification();

}
