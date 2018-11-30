package com.android.car.notification;

import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.List;


/**
 * Layout that contains Car Notifications.
 *
 * It does some extra setup in the onFinishInflate method because it may not get used from an
 * activity where one would normally attach RecyclerViews
 */
public class CarNotificationView extends RelativeLayout
        implements CarUxRestrictionsManager.OnUxRestrictionsChangedListener {

    private CarNotificationViewAdapter mAdapter;
    private Context mContext;
    public CarNotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    /**
     * Attaches the CarNotificationViewAdapter and CarNotificationItemTouchListener to the
     * notification list.
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        PagedListView listView = findViewById(R.id.notifications);
        mAdapter = new CarNotificationViewAdapter(mContext,/* isGroupNotificationAdapter= */ false);
        listView.setAdapter(mAdapter);
        ((SimpleItemAnimator) listView.getRecyclerView().getItemAnimator())
                .setSupportsChangeAnimations(false);
        listView.getRecyclerView().addOnItemTouchListener(
                new CarNotificationItemTouchListener(mContext, mAdapter));
        // TODO: Remove this line after PagedListView supports lowering the scroll bar elevation.
        // Elevate the PagedListView so that cards can go on top of the scroll bar when swiping.
        // Setting a large number because the z position of the scroll bar is unknown.
        listView.getRecyclerView().bringToFront();
    }

    /**
     * Updates notifications and update views.
     */
    public void setNotifications(List<NotificationGroup> notifications) {
        mAdapter.setNotifications(notifications);
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
        mAdapter.setCarUxRestrictions(restrictionInfo);
    }
}
