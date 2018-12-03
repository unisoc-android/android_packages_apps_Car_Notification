#
# Copyright (C) 2018 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

###############################################################
# Car Notification Robolectric test target.                  #
###############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := CarNotificationRoboTests
LOCAL_MODULE_CLASS := JAVA_LIBRARIES

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_RESOURCE_DIRS := config

LOCAL_JAVA_LIBRARIES := \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt  \
    testng \
    android.car

LOCAL_INSTRUMENTATION_FOR := CarNotificationForTesting

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_JAVA_LIBRARY)

###############################################################
# Car Notification runner target to run the previous target. #
###############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunCarNotificationRoboTests

LOCAL_JAVA_LIBRARIES := \
    CarNotificationRoboTests \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt \
    testng \
    android.car


LOCAL_TEST_PACKAGE := CarNotificationForTesting

LOCAL_ROBOTEST_FILES := \
    $(call find-files-in-subdirs,$(LOCAL_PATH)/src,*Test.java,.))

LOCAL_INSTRUMENT_SOURCE_DIRS := $(dir $(LOCAL_PATH))../src


include external/robolectric-shadows/run_robotests.mk