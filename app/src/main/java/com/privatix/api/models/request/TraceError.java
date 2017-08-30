package com.privatix.api.models.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Lotar on 07.10.2016.
 */

public class TraceError {
    private String type;
    private long datetime;
    @SerializedName("subscription_uuid")
    @Expose
    private
    String subscriptionUuid;
    private String error;
    @SerializedName("error_trace")
    @Expose
    private
    String errorTrace;
    @SerializedName("source_country")
    @Expose
    private
    String sourceCountry;
    @SerializedName("connection_node")
    @Expose
    private
    String connectionNode;


    public TraceError(String type, long datetime, String subscriptionUuid, String error, String errorTrace, String sourceCountry, String connectionNode) {
        this.type = type;
        this.datetime = datetime;
        this.subscriptionUuid = subscriptionUuid;
        this.error = error;
        this.errorTrace = errorTrace;
        this.sourceCountry = sourceCountry;
        this.connectionNode = connectionNode;
    }
}
