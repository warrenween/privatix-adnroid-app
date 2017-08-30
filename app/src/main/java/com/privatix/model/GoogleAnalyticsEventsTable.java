package com.privatix.model;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

/**
 * Created by ross on 12.05.16.
 */
public class GoogleAnalyticsEventsTable extends SugarRecord {
    @Ignore
    public static final int TYPE_VIEW_PAGE = 0;
    @Ignore
    public static final int TYPE_EVENT = 1;
    private int type;
    private String screenName;
    private String category;
    private String action;
    private String label;
    private long value;

    public GoogleAnalyticsEventsTable() {
    }

    public GoogleAnalyticsEventsTable(int type, String screenName, String category, String action, String label, long value) {
        this.type = type;
        this.screenName = screenName;
        this.category = category;
        this.action = action;
        this.label = label;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
