package com.privatix.model;

import com.google.gson.annotations.Expose;
import com.orm.SugarRecord;

/**
 * Created by Lotar on 12.11.2015.
 */
public class NotificationTable extends SugarRecord<NotificationTable> {

    @Expose
    private
    String type;
    @Expose
    private
    String period;
    @Expose
    private
    String format;
    @Expose
    private
    String target;
    @Expose
    private
    String text;
    @Expose
    private
    String link;
    private
    String url;
    @Expose
    private String ttl;


    public NotificationTable(String type, String period, String target, String format, String text, String link, String ttl, String url) {
        this.type = type;
        this.period = period;
        this.target = target;
        this.text = text;
        this.link = link;
        this.ttl = ttl;
        this.format = format;
        this.url = url;
    }


    public NotificationTable() {
    }

    public String getUrl() {
        return url;
    }

    public String getFormat() {
        return format;
    }

    public String getPeriod() {
        return period;
    }

    public String getTarget() {
        return target;
    }

    public String getText() {
        return text;
    }

    public String getLink() {
        return link;
    }

    public String getType() {
        return type;
    }

    public String getTtl() {
        return ttl;
    }
}
