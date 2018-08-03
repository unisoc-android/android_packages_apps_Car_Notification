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

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({NotificationViewType.COLLAPSED_GROUP_NOTIFICATION_VIEW_TYPE,
        NotificationViewType.EXPANDED_GROUP_NOTIFICATION_VIEW_TYPE,
        NotificationViewType.BASIC_NOTIFICATION_VIEW_TYPE,
        NotificationViewType.BASIC_NOTIFICATION_IN_GROUP_VIEW_TYPE,
        NotificationViewType.HEADS_UP_BASIC_NOTIFICATION_VIEW_TYPE,
        NotificationViewType.MESSAGING_NOTIFICATION_VIEW_TYPE,
        NotificationViewType.MESSAGE_NOTIFICATION_IN_GROUP_VIEW_TYPE,
        NotificationViewType.HEADS_UP_MESSAGING_NOTIFICATION_VIEW_TYPE,
        NotificationViewType.PROGRESS_NOTIFICATION_VIEW_TYPE,
        NotificationViewType.PROGRESS_NOTIFICATION_IN_GROUP_VIEW_TYPE})
@Retention(RetentionPolicy.SOURCE)
@interface NotificationViewType {
    int BASIC_NOTIFICATION_VIEW_TYPE = 0;
    int HEADS_UP_BASIC_NOTIFICATION_VIEW_TYPE = 1;
    int MESSAGING_NOTIFICATION_VIEW_TYPE = 2;
    int HEADS_UP_MESSAGING_NOTIFICATION_VIEW_TYPE = 3;
    int PROGRESS_NOTIFICATION_VIEW_TYPE = 4;
    int COLLAPSED_GROUP_NOTIFICATION_VIEW_TYPE = 5;
    int EXPANDED_GROUP_NOTIFICATION_VIEW_TYPE = 6;
    int MESSAGE_NOTIFICATION_IN_GROUP_VIEW_TYPE = 7;
    int BASIC_NOTIFICATION_IN_GROUP_VIEW_TYPE = 8;
    int PROGRESS_NOTIFICATION_IN_GROUP_VIEW_TYPE = 9;
}
