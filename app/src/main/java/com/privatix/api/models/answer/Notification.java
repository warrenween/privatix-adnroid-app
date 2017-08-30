package com.privatix.api.models.answer;

import com.google.gson.annotations.Expose;

/**
 * Created by Lotar on 11.04.2016.
 */
public class Notification {
    @Expose
    String type;
    @Expose
    String period;
    @Expose
    String format;
    @Expose
    String target;
    @Expose
    String text;
    @Expose
    String link;
    @Expose
    String url;
    @Expose
    String ttl;
    @Expose
    String content;


    public String getFormat() {
        return format;
    }


    public String getUrl() {
        return url;
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

    public String getContent() {
        return content;
    }
}
