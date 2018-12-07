package com.android.car.notification;

import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.car.widget.PagedListView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.List;


/**
 * Layout that contains Car Notifications.
 *
 * It does some extra setup in the onFinishInflate method because it may not get used from an
 * activity where one would normally attach RecyclerViews
 */
public class CarNotificationView extends ConstraintLayout
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
        // TODO: replace adding margin to the bottom code with the API call to
        // PagedListView once it's implemented.
        ViewGroup.MarginLayoutParams marginLayoutParams =
                (ViewGroup.MarginLayoutParams) listView.getRecyclerView().getLayoutParams();
        marginLayoutParams.setMargins(0, 0, 0,
                mContext.getResources().getDimensionPixelOffset(R.dimen.item_spacing));
        listView.getRecyclerView().setLayoutParams(marginLayoutParams);
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

    /**
     * Sets the NotificationClickHandlerFactory that allows for a hook to run a block off code
     * when  the notification is clicked. This is useful to dismiss a screen after
     * a notification list clicked.
     */
    public void setClickHandlerFactory(NotificationClickHandlerFactory clickHandlerFactory) {
        mAdapter.setClickHandlerFactory(clickHandlerFactory);
    }
}
