package com.privatix.utils;

import android.text.TextUtils;

import com.privatix.api.models.answer.Notification;
import com.privatix.api.models.answer.subscription.Nodes;
import com.privatix.model.Hosts;
import com.privatix.model.NotificationTable;
import com.privatix.model.ProfileTable;
import com.privatix.model.SubscriptionTable;

import java.util.List;

/**
 * Created by Lotar on 03.07.2016.
 */
public class DatabaseUtils {
    public static String getSid() {
        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        ProfileTable currentProfile = profileTable.get(0);
        return currentProfile.getSid();
    }

    public static void saveProfile(ProfileTable currentProfile, Boolean isAuthorized, Boolean isVerified, String plan,
                                   String country, String vpnLogin, String vpnPassword, String expiresAt, String sid, String subscriptionId) {
        currentProfile.setIs_authorized(isAuthorized);
        if (!isAuthorized)
            currentProfile.setPlan("free");
        else
            currentProfile.setPlan(plan);
        currentProfile.setSid(sid);
        currentProfile.setIs_verified(isVerified);
        currentProfile.setSubscriptionId(subscriptionId);
        currentProfile.setOriginalCountry(country);
        currentProfile.setCurrentVpnLogin(vpnLogin);
        currentProfile.setCurrentVpnPassword(vpnPassword);
        currentProfile.setPremiumExpiresAt(expiresAt);
        currentProfile.save();
    }

    public static void saveNewSubscriptions(List<Nodes> nodesList) {
        SubscriptionTable.deleteAll(SubscriptionTable.class);
        for (Nodes node : nodesList) {
            //Nodes node = nodesList.get(i);
            String country = node.getDisplayName();
            if (TextUtils.isEmpty(country))
                country = node.getCountry();
            SubscriptionTable subscriptionTable = new SubscriptionTable(country, node.getCountryCode(), node.getFree());
            subscriptionTable.save();
            for (int j = 0; j < node.getHosts().size(); j++) {
                Hosts host = new Hosts(node.getHosts().get(j).getHost(), subscriptionTable);
                host.save();
            }
        }

    }

    public static void saveNewNotifications(List<Notification> notifications) {
        NotificationTable.deleteAll(NotificationTable.class);
        if (notifications != null) {
            for (Notification notification : notifications) {
                NotificationTable notificationTable = new NotificationTable(notification.getType(), notification.getPeriod(), notification.getTarget(), notification.getFormat(),
                        notification.getText(), notification.getLink(), notification.getTtl(), notification.getUrl());
                notificationTable.save();
            }
        }
    }
}
