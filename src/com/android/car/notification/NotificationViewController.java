package com.android.car.notification;

import android.car.CarNotConnectedException;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class is a bridge to collect signals from the notification and ux restriction services and
 * trigger the correct UI updates.
 */
public class NotificationViewController {

    private static final String TAG = "NotificationViewControl";
    private CarNotificationView mCarNotificationView;
    private PreprocessingManager mPreprocessingManager;
    private CarNotificationListener mCarNotificationListener;
    private CarUxRestrictionsManager mCarUxRestrictionsManager;
    private NotificationUpdateHandler mNotificationUpdateHandler = new NotificationUpdateHandler();

    public NotificationViewController(CarNotificationView carNotificationView,
            PreprocessingManager preprocessingManager,
            CarNotificationListener carNotificationListener,
            CarUxRestrictionsManager carUxRestrictionsManager) {
        mCarNotificationView = carNotificationView;
        mPreprocessingManager = preprocessingManager;
        mCarNotificationListener = carNotificationListener;
        mCarUxRestrictionsManager = carUxRestrictionsManager;
    }

    /**
     * Set the Ux restriction manager. This is needed if it was not ready at the time of creation.
     */
    public void setCarUxRestrictionsManager(
            CarUxRestrictionsManager carUxRestrictionsManager) {
        mCarUxRestrictionsManager = carUxRestrictionsManager;
    }

    /**
     * Updates UI and registers required listeners
     */
    public void enable() {
        mCarNotificationListener.setHandler(mNotificationUpdateHandler);
        try {
            if (mCarUxRestrictionsManager != null) {
                mCarUxRestrictionsManager.registerListener(restrictionInfo ->
                        mCarNotificationView.setCarUxRestrictions(restrictionInfo));
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car not connected", e);
        }
        updateNotifications();
    }

    /**
     * Removes listeners
     */
    public void disable() {
        mCarNotificationListener.setHandler(null);
        try {
            if (mCarUxRestrictionsManager != null) {
                mCarUxRestrictionsManager.unregisterListener();
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car not connected", e);
        }
    }


    /**
     * Update all notifications and ranking
     */
    private void updateNotifications() {
        mCarNotificationView.setNotifications(
                mPreprocessingManager.process(
                        mCarNotificationListener.getNotifications(),
                        mCarNotificationListener.getCurrentRanking()));
    }

    private class NotificationUpdateHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            if (message.what == CarNotificationListener.NOTIFY_NOTIFICATIONS_CHANGED) {
                updateNotifications();

            } else if (message.what == CarNotificationListener.NOTIFY_NOTIFICATION_ADDED) {
                updateNotifications();
            }
        }
    }
}
