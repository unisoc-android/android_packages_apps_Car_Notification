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

import android.annotation.Nullable;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.car.notification.R;

import java.util.List;

/**
 * Messaging notification template that displays a messaging notification and a voice reply button.
 */
public class MessageNotificationViewHolder extends CarNotificationBaseViewHolder {
    private static final String TAG = "car_notification_messaging";
    private final Context mContext;
    private final CarNotificationHeaderView mHeaderView;
    private final CarNotificationBodyView mBodyView;
    private final CarNotificationActionsView mActionsView;
    private final View mParentView;
    private StatusBarNotification mStatusBarNotification;

    public MessageNotificationViewHolder(View view) {
        super(view);
        mContext = view.getContext();
        mParentView = view;
        mHeaderView = view.findViewById(R.id.notification_header);
        mBodyView = view.findViewById(R.id.notification_body);
        mActionsView = view.findViewById(R.id.notification_actions);
    }

    /**
     * Binds a {@link StatusBarNotification} to a messaging car notification template without
     * UX restriction.
     */
    @Override
    public void bind(StatusBarNotification statusBarNotification, boolean isInGroup) {
        reset();
        bindBody(statusBarNotification, /* isRestricted= */ false);
        mHeaderView.bind(statusBarNotification, isInGroup);
        mActionsView.bind(statusBarNotification, isInGroup);
    }

    /**
     * Binds a {@link StatusBarNotification} to a messaging car notification template with
     * UX restriction.
     */
    public void bindRestricted(StatusBarNotification statusBarNotification, boolean isInGroup) {
        reset();
        bindBody(statusBarNotification, /* isRestricted= */ true);
        mHeaderView.bind(statusBarNotification, isInGroup);
        mActionsView.bind(statusBarNotification, isInGroup);
    }

    /**
     * Private method that binds the data to the view.
     */
    private void bindBody(
            @Nullable StatusBarNotification statusBarNotification, boolean isRestricted) {

        mStatusBarNotification = statusBarNotification;
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

        CharSequence messageText = null;
        CharSequence senderName = null;
        Icon avatar = null;
        Integer messageCount = null;

        Bundle extras = notification.extras;
        Parcelable[] messagesData = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (messagesData != null) {
            List<Notification.MessagingStyle.Message> messages =
                    Notification.MessagingStyle.Message.getMessagesFromBundleArray(messagesData);
            if (messages != null && !messages.isEmpty()) {
                messageCount = messages.size();
                // Use the latest message
                Notification.MessagingStyle.Message message = messages.get(messages.size() - 1);
                messageText = message.getText();
                Person sender = message.getSenderPerson();
                if (sender != null) {
                    senderName = sender.getName();
                    avatar = sender.getIcon();
                } else {
                    senderName = message.getSender();
                }
            }
        }

        // app did not use messaging style, fall back to standard fields
        if (messageCount == null) {
            messageCount = notification.number;
            if (messageCount == 0) {
                messageCount = 1; // a notification should at least represent 1 message
            }
        }

        if (TextUtils.isEmpty(senderName)) {
            senderName = extras.getCharSequence(Notification.EXTRA_TITLE);
        }

        if (isRestricted) {
            messageText = mContext.getResources().getQuantityString(
                    R.plurals.restricted_message_text, messageCount, messageCount);
        } else if (TextUtils.isEmpty(messageText)) {
            messageText = extras.getCharSequence(Notification.EXTRA_TEXT);
        }

        if (avatar == null) {
            avatar = notification.getLargeIcon();
        }

        mBodyView.bind(senderName, messageText, avatar);
    }

    /**
     * Resets the messaging notification view empty for recycling.
     */
    @Override
    void reset() {
        super.reset();
        mParentView.setClickable(false);
        mParentView.setOnClickListener(null);
    }

    @Override
    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }

}
