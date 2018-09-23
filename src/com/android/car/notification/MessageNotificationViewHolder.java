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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Messaging notification template that displays a messaging notification and a voice reply button.
 */
public class MessageNotificationViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "car_notification_messaging";
    private final Context mContext;
    private final CarNotificationHeaderView mHeaderView;
    private final CarNotificationActionsView mActionsView;
    private final View mParentView;
    private final TextView mSenderNameView;
    private final TextView mTitleTextView;
    private final ImageView mAvatarView;

    public MessageNotificationViewHolder(View view) {
        super(view);
        mContext = view.getContext();
        mParentView = view;
        mHeaderView = view.findViewById(R.id.notification_header);
        mActionsView = view.findViewById(R.id.notification_actions);
        mSenderNameView = view.findViewById(R.id.sender_name);
        mTitleTextView = view.findViewById(R.id.conversation_title);
        mAvatarView = view.findViewById(R.id.sender_avatar);
    }

    /**
     * Binds a {@link StatusBarNotification} to a messaging car notification template.
     *
     * @param statusBarNotification passing {@code null} clears the view.
     * @param isInGroup whether this notification card is part of a group.
     */
    public void bind(@Nullable StatusBarNotification statusBarNotification, boolean isInGroup) {
        reset();
        if (statusBarNotification == null) {
            return;
        }
        mHeaderView.bind(statusBarNotification, /* primaryColor= */ null);
        mActionsView.bind(statusBarNotification, isInGroup);

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

        Bundle extras = notification.extras;
        Parcelable[] messagesData = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (messagesData != null) {
            List<Notification.MessagingStyle.Message> messages =
                    Notification.MessagingStyle.Message.getMessagesFromBundleArray(messagesData);
            if (messages != null && !messages.isEmpty()) {
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
        if (TextUtils.isEmpty(senderName)) {
            senderName = extras.getCharSequence(Notification.EXTRA_TITLE);
        }
        if (TextUtils.isEmpty(messageText)) {
            messageText = extras.getCharSequence(Notification.EXTRA_TEXT);
        }
        if (avatar == null) {
            avatar = notification.getLargeIcon();
        }

        if (!TextUtils.isEmpty(messageText)) {
            mTitleTextView.setVisibility(View.VISIBLE);
            mTitleTextView.setText(messageText);
        }
        if (!TextUtils.isEmpty(senderName)) {
            mSenderNameView.setVisibility(View.VISIBLE);
            mSenderNameView.setText(senderName);
        }
        if (avatar != null) {
            mAvatarView.setImageIcon(avatar);
        }
    }

    /**
     * Resets the messaging notification view empty for recycling.
     */
    private void reset() {
        mParentView.setClickable(false);
        mParentView.setOnClickListener(null);

        mHeaderView.reset();

        mActionsView.reset();

        mSenderNameView.setVisibility(View.GONE);
        mSenderNameView.setText(null);

        mAvatarView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_person));

        mTitleTextView.setVisibility(View.GONE);
        mTitleTextView.setText(null);
    }
}
