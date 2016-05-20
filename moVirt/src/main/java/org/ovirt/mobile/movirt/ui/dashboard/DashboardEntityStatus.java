package org.ovirt.mobile.movirt.ui.dashboard;

/**
 * Created by suomiy on 5/16/16.
 */
public class DashboardEntityStatus {
    private int count = 0;
    private int iconResourceId;

    public void incrementCount() {
        count++;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public void setIconResourceId(int iconResourceId) {
        this.iconResourceId = iconResourceId;
    }
}
