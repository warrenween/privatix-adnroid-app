package com.privatix.model;

import com.orm.SugarRecord;

/**
 * Created by ross on 06.05.16.
 */
public class OriginalCountrySpeedCheckerTable extends SugarRecord<OriginalCountrySpeedCheckerTable> {

    String originalCountry;

    public OriginalCountrySpeedCheckerTable() {
    }

    public OriginalCountrySpeedCheckerTable(String originalCountry) {
        this.originalCountry = originalCountry;
    }

    public String getOriginalCountry() {
        return originalCountry;
    }

    public void setOriginalCountry(String originalCountry) {
        this.originalCountry = originalCountry;
    }
}
