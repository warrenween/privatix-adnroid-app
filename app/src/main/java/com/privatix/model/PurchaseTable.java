package com.privatix.model;

import com.orm.SugarRecord;

/**
 * Created by Lotar on 05.05.2016.
 */
public class PurchaseTable extends SugarRecord<PurchaseTable> {
    private String token;
    private String sid;

    public PurchaseTable(String token, String sid) {
        this.token = token;
        this.sid = sid;
    }

    public PurchaseTable() {
    }


    public String getSid() {
        return sid;
    }

    public String getToken() {
        return token;
    }
}
