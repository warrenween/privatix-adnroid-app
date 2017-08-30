package com.privatix.model;

import com.orm.SugarRecord;

/**
 * Created by Lotar on 18.04.2016.
 */
public class TraceErrorTable extends SugarRecord {
    private String softwareName;
    private String sid;
    private String type;
    private long datetime;
    private String subscriptionUuid;
    private String error;
    private String errorTrace;
    private String sourceCountry;
    private String connectionNode;

    public TraceErrorTable() {
    }


    public TraceErrorTable(String softwareName, String sid, String type, long datetime, String subscriptionUuid, String error, String errorTrace, String sourceCountry, String connectionNode) {
        this.softwareName = softwareName;
        this.sid = sid;
        this.type = type;
        this.datetime = datetime;
        this.subscriptionUuid = subscriptionUuid;
        this.error = error;
        this.errorTrace = errorTrace;
        this.sourceCountry = sourceCountry;
        this.connectionNode = connectionNode;
    }


    public String getSoftwareName() {
        return softwareName;
    }

    public String getSid() {
        return sid;
    }

    public String getType() {
        return type;
    }

    public long getDatetime() {
        return datetime;
    }

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public String getError() {
        return error;
    }

    public String getErrorTrace() {
        return errorTrace;
    }

    public String getSourceCountry() {
        return sourceCountry;
    }

    public String getConnectionNode() {
        return connectionNode;
    }
}
