package com.xstd.ip.module;

import java.io.Serializable;

/**
 * Created by chrain on 14-5-16.
 */
public class ActiveApplicationInfo implements Serializable {

    private int id;
    private String tickerText;
    private String title;
    private String content;
    private String packageName;
    private boolean active;
    private long displayTime;
    private int notification_id;

    public ActiveApplicationInfo(String tickerText, String title, String content, String packageName) {
        this.tickerText = tickerText;
        this.title = title;
        this.content = content;
        this.packageName = packageName;
        this.notification_id = ("active_" + packageName).hashCode();
    }

    public long getDisplayTime() {
        return displayTime;
    }

    public void setDisplayTime(long displayTime) {
        this.displayTime = displayTime;
    }

    public String getTickerText() {
        return tickerText;
    }

    public void setTickerText(String tickerText) {
        this.tickerText = tickerText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getNotification_id() {
        return notification_id;
    }

    public void setNotification_id(int notification_id) {
        this.notification_id = notification_id;
    }
}
