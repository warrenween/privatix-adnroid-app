package com.privatix.model;

import com.orm.SugarRecord;

/**
 * Created by ross on 06.05.16.
 */
public class SpeedCheckerCountyTable extends SugarRecord<SpeedCheckerCountyTable> {
    String countryCode;

    public SpeedCheckerCountyTable() {
    }

    public SpeedCheckerCountyTable(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
