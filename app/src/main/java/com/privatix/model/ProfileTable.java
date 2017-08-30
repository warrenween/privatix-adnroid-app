package com.privatix.model;

import com.orm.SugarRecord;

/**
 * Created by Lotar on 12.11.2015.
 */
public class ProfileTable extends SugarRecord<ProfileTable> {

    private String sid;

    private String userEmail;

    private String originalCountry;

    private Boolean is_authorized;

    private Boolean is_verified;

    private String plan;

    private String currentVpnLogin;

    private String currentVpnPassword;

    private String premiumExpiresAt;

    private String subscriptionId;

    public ProfileTable() {
    }

    public ProfileTable(Boolean is_authorized, Boolean is_verified, String plan) {
        this.is_authorized = is_authorized;
        this.is_verified = is_verified;
        this.plan = plan;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getOriginalCountry() {
        return originalCountry;
    }

    public void setOriginalCountry(String original_country) {
        this.originalCountry = original_country;
    }

    public String getCurrentVpnLogin() {
        return currentVpnLogin;
    }

    public void setCurrentVpnLogin(String currentVpnLogin) {
        this.currentVpnLogin = currentVpnLogin;
    }

    public String getCurrentVpnPassword() {
        return currentVpnPassword;
    }

    public void setCurrentVpnPassword(String currentVpnPassword) {
        this.currentVpnPassword = currentVpnPassword;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getPremiumExpiresAt() {
        return premiumExpiresAt;
    }

    public void setPremiumExpiresAt(String premiumExpiresAt) {
        this.premiumExpiresAt = premiumExpiresAt;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setIs_authorized(Boolean is_authorized) {
        this.is_authorized = is_authorized;
    }

    public void setIs_verified(Boolean is_verified) {
        this.is_verified = is_verified;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public Boolean isAuthorized() {
        return is_authorized;
    }

    public Boolean isVerified() {
        return is_verified;
    }
}
