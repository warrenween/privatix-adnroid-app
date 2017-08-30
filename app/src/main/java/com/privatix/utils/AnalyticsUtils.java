package com.privatix.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.privatix.R;
import com.privatix.model.GoogleAnalyticsEventsTable;
import com.privatix.utils.purchase.Inventory;

import java.util.HashMap;
import java.util.List;

import static com.privatix.model.GoogleAnalyticsEventsTable.TYPE_EVENT;
import static com.privatix.model.GoogleAnalyticsEventsTable.TYPE_VIEW_PAGE;

/**
 * Created by Lotar on 01.07.2016.
 */
public class AnalyticsUtils {

    private static void sendEventGoogleAnalytics(Context context, Tracker tracker, String screenName, String category,
                                                 String action, String label, Long value) {

        if (tracker == null)
            return;

        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder();

        tracker.setScreenName(screenName);

        eventBuilder.setCategory(category)
                .setAction(action);

        if (label != null && !label.equals("")) eventBuilder.setLabel(label);
        if (value != null && value != -1) eventBuilder.setValue(value);

        if (Utils.isNetworkAvailable(context)) {
            tracker.send(eventBuilder.build());
            Log.d(Constants.GOOGLE_ANALYTICS_DEBUG_TAG, "triggered event: " + category);
        } else {
            String tableLabel;
            Long tableValue;

            if (label == null) tableLabel = "";
            else tableLabel = label;

            if (value == null) tableValue = -1L;
            else tableValue = value;

            GoogleAnalyticsEventsTable newEvent = new GoogleAnalyticsEventsTable(
                    TYPE_EVENT,
                    screenName,
                    category, action, tableLabel, tableValue);
            newEvent.save();
            Log.d(Constants.GOOGLE_ANALYTICS_DEBUG_TAG, "triggered event: unsuccess - " + category);
        }
    }

    public static void checkForNotSentEvents(Context context, Tracker tracker) {
        //List<GoogleAnalyticsEventsTable> eventsList = listAll(GoogleAnalyticsEventsTable.class);
        List<GoogleAnalyticsEventsTable> eventsList = GoogleAnalyticsEventsTable.listAll(GoogleAnalyticsEventsTable.class);
        Log.d(Constants.GOOGLE_ANALYTICS_DEBUG_TAG, "number of not sent: " + eventsList.size());

        if (eventsList.size() != 0) {
            for (GoogleAnalyticsEventsTable event : eventsList) {
                switch (event.getType()) {
                    case TYPE_EVENT:
                        sendEventGoogleAnalytics(context, tracker, event.getScreenName(),
                                event.getCategory(), event.getAction(), event.getLabel(), event.getValue());
                        event.delete();
                        break;
                    case TYPE_VIEW_PAGE:
                        sendViewScreenEventGoogleAnalytics(context, tracker, event.getScreenName());
                        event.delete();
                        break;
                }
            }
        }

    }

//    public static void sendFlurryEvent(Context context, String eventID, Map<String, String> parameters) {
//        if (Utils.isNetworkAvailable(context)) {
//            if (parameters == null) FlurryAgent.logEvent(eventID);
//            else FlurryAgent.logEvent(eventID, parameters);
//            Log.d(Constants.FLURRY_DEBUG_TAG, "triggered event: " + eventID);
//        } else {
//            Log.d(Constants.FLURRY_DEBUG_TAG, "triggered event unsuccess: " + eventID);
//            FlurryEventsTable newEvent = new FlurryEventsTable(
//                    eventID, parameters);
//            newEvent.save();
//        }
//    }

    public static void sendViewScreenEventGoogleAnalytics(Context context, Tracker tracker,
                                                          String screenName) {
        tracker.setScreenName(screenName);

        if (Utils.isNetworkAvailable(context)) {
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
            Log.d(Constants.GOOGLE_ANALYTICS_DEBUG_TAG, "view_screen: " + screenName);
        } else {
            GoogleAnalyticsEventsTable newScreenView = new GoogleAnalyticsEventsTable(
                    TYPE_VIEW_PAGE,
                    screenName,
                    "", "", "", -1);
            newScreenView.save();

            Log.d(Constants.GOOGLE_ANALYTICS_DEBUG_TAG, "view_screen: unsuccess viewing - " + screenName);
        }

    }

//    public static void checkForNotSentFlurryEvents(Context context) {
//        List<FlurryEventsTable> eventsList = listAll(FlurryEventsTable.class);
//        if (eventsList.size() != 0) {
//            for (FlurryEventsTable flurryEvent : eventsList) {
//                sendFlurryEvent(context, flurryEvent.getEventID(),
//                        flurryEvent.getParameters());
//                flurryEvent.delete();
//            }
//        }
//
//    }

    public static void sendEventAppInstall(Context context, SharedPreferences sp, Tracker tracker) {
        //if (sp.getBoolean(PrefKeys.APP_INSTALL_FIRST_LAUNCH, true)) {
        //GA & Flurry - install (first launch)
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "start_screen", "install", "android_tab",
                    null, null);
            //sendFlurryEvent(context, "install_android_tab", null);

        } else {
            sendEventGoogleAnalytics(context, tracker, "start_screen", "install", "android_mob",
                    null, null);
            //sendFlurryEvent(context, "install_android_mob", null);
        }
        sp.edit().putBoolean(PrefKeys.APP_INSTALL_FIRST_LAUNCH, false).apply();
    }

    public static void sendEventAppOpen(Context context, SharedPreferences sp, Tracker tracker) {
        if (!sp.getBoolean(PrefKeys.APP_INSTALL_FIRST_LAUNCH, true)) {

            if (Utils.isTabletMoreThanSevenInches(context)) {
                sendEventGoogleAnalytics(context, tracker, "start_screen", "open", "android_tab",
                        null, null);
                //sendFlurryEvent(context, "open_android_tab", null);

            } else {
                sendEventGoogleAnalytics(context, tracker, "start_screen", "open", "android_mob",
                        null, null);
                //sendFlurryEvent(context, "open_android_mob", null);
            }
        }
    }


    public static void sendEventCheckForCompetitors(Context context, SharedPreferences sp, Tracker tracker) {
        if (!sp.getBoolean(PrefKeys.CHECK_FOR_COMPETITORS, false)) {
            String[] packageNames = context.getResources().getStringArray(R.array.competitors);

            String competitorsDetected = Utils.isPackageInstalled(packageNames, context);

            if (!competitorsDetected.equals("")) {
                Log.d(Constants.GOOGLE_ANALYTICS_DEBUG_TAG, competitorsDetected);
                if (Utils.isTabletMoreThanSevenInches(context)) {

                    sendEventGoogleAnalytics(context, tracker, "start_screen", "competitor", "android_tab",
                            competitorsDetected, null);

                    HashMap<String, String> parameters = new HashMap<>();
                    parameters.put("competitors", competitorsDetected);
                    //sendFlurryEvent(context, "competitor_android_tab", parameters);

                } else {
                    sendEventGoogleAnalytics(context, tracker, "start_screen", "competitor", "android_mob",
                            competitorsDetected, null);

                    HashMap<String, String> parameters = new HashMap<>();
                    parameters.put("competitors", competitorsDetected);
                    //sendFlurryEvent(context, "competitor_android_mob", parameters);
                }

                sp.edit().putBoolean(PrefKeys.CHECK_FOR_COMPETITORS, true).apply();
            } else {
                Log.d(Constants.GOOGLE_ANALYTICS_DEBUG_TAG, "competitors_detected: none");
            }
        } else {
            Log.d(Constants.GOOGLE_ANALYTICS_DEBUG_TAG, "competitors check already done");
        }
    }

    public static void sendEventProtectionOffFromSwitch(Context context, Tracker tracker, String screenName) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, screenName, "switch_off", "android_tab",
                    null, null);
            //sendFlurryEvent(context, "switch_off_android_tab", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, screenName, "switch_off", "android_mob",
                    null, null);
            //sendFlurryEvent(context, "switch_off_android_mob", null);
        }
    }

    public static void sendEventProtectedFromSwitch(Context context, Tracker tracker, String screenName) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, screenName, "switch_on", "android_tab",
                    null, null);
            //sendFlurryEvent(context, "switch_on_android_tab", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, screenName, "switch_on", "android_mob",
                    null, null);
            //sendFlurryEvent(context, "switch_on_android_mob", null);
        }
    }

    public static void sendEventProtectionOffFromShield(Context context, Tracker tracker, String screenName) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, screenName, "switch_off", "android_tab",
                    "shield", null);
            //sendFlurryEvent(context, "switch_off_android_tab_shield", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, screenName, "switch_off", "android_mob",
                    "shield", null);
            //sendFlurryEvent(context, "switch_off_android_mob_shield", null);
        }
    }

    public static void sendEventProtectedFromShield(Context context, Tracker tracker, String screenName) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, screenName, "switch_on", "android_tab",
                    "shield", null);
            //sendFlurryEvent(context, "switch_on_android_tab_shield", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, screenName, "switch_on", "android_mob",
                    "shield", null);
            //sendFlurryEvent(context, "switch_on_android_mob_shield", null);
        }
    }

    public static void sendEventSelectedCountry(Context context, Tracker tracker, String screenName, String countryCode) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, screenName, "country", "android_tab",
                    countryCode, null);
            //sendFlurryEvent(context, "country_android_tab_" + countryCode,
            //        null);
        } else {
            sendEventGoogleAnalytics(context, tracker, screenName, "country", "android_mob",
                    countryCode, null);
            //sendFlurryEvent(context, "country_android_mob_" + countryCode,
            //       null);
        }
    }

    public static void sendEventSuccessPurchaseDetails(Context context, Tracker tracker, String slideName, Inventory inventory) {
        //GA - success purchase
        if (inventory.getSkuDetails(Constants.SKU_PREMIUM_MONTHLY) != null) {
            String skuMonth = inventory.getSkuDetails(Constants.SKU_PREMIUM_MONTHLY).getSku();
            long mPriceOfMonth = 10;
            if (Utils.isTabletMoreThanSevenInches(context)) {
                sendEventGoogleAnalytics(context, tracker, slideName, "premium_sale",
                        "android_tab", skuMonth, mPriceOfMonth);
            } else {
                sendEventGoogleAnalytics(context, tracker, slideName, "premium_sale",
                        "android_mob", skuMonth, mPriceOfMonth);
            }
        }

        if (inventory.getSkuDetails(Constants.SKU_PREMIUM_YEARLY) != null) {
            long mPriceOfYear = 59;
            String skuYear = inventory.getSkuDetails(Constants.SKU_PREMIUM_YEARLY).getSku();
            if (Utils.isTabletMoreThanSevenInches(context)) {
                sendEventGoogleAnalytics(context, tracker, slideName, "premium_sale",
                        "android_tab", skuYear, mPriceOfYear);
            } else {
                sendEventGoogleAnalytics(context, tracker, slideName, "premium_sale",
                        "android_mob", skuYear, mPriceOfYear);
            }

        }
    }

    public static void sendEventAppExit(Context context, Tracker tracker) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "", "exit", "android_tab",
                    null, null);
            //sendFlurryEvent(context, "exit_android_tab", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "", "exit", "android_mob",
                    null, null);
            //sendFlurryEvent(context, "exit_android_mob", null);
        }

    }

    public static void sendEventEmailLogin(Context context, Tracker tracker) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "login", "login",
                    "android_tab", "email", null);
            // sendFlurryEvent(context, "login_android_tab_email", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "login", "login",
                    "android_mob", "email", null);
            //sendFlurryEvent(context, "login_android_mob_email", null);
        }
    }

    public static void sendEventGooglePlusLogin(Context context, Tracker tracker) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "login", "login",
                    "android_tab", "gplus", null);
            // sendFlurryEvent(context, "login_android_tab_gplus", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "login", "login",
                    "android_mob", "gplus", null);
            //sendFlurryEvent(context, "login_android_mob_gplus", null);
        }
    }

    public static void sendEventFacebookLogin(Context context, Tracker tracker) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "login", "login",
                    "android_tab", "fb", null);
            //sendFlurryEvent(context, "login_android_tab_fb", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "login", "login",
                    "android_mob", "fb", null);
            //sendFlurryEvent(context, "login_android_mob_fb", null);
        }
    }

    public static void sendEventPasswordRecoverySuccess(Context context, Tracker tracker) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "recover", "pass_recovery",
                    "android_tab", null, null);
            //sendFlurryEvent(context, "pass_recovery_android_tab", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "recover", "pass_recovery",
                    "android_mob", null, null);
            //sendFlurryEvent(context, "pass_recovery_android_mob", null);
        }
    }

    public static void sendEventSignUpGooglePLus(Context context, Tracker tracker) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "signup", "signup", "android_tab",
                    "gplus", null);
            //sendFlurryEvent(context, "signup_android_tab_gplus", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "signup", "signup", "android_mob",
                    "gplus", null);
            //sendFlurryEvent(context, "signup_android_mob_gplus", null);
        }
    }

    public static void sendEventSignUpFacebook(Context context, Tracker tracker) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "signup", "signup", "android_tab",
                    "fb", null);
            //sendFlurryEvent(context, "signup_android_tab_fb", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "signup", "signup", "android_mob",
                    "fb", null);
            //sendFlurryEvent(context, "signup_android_mob_fb", null);
        }
    }

    public static void sendEventSignUpEmail(Context context, Tracker tracker) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "signup", "signup", "android_tab",
                    "email", null);
            //sendFlurryEvent(context, "signup_android_tab_email", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "signup", "signup", "android_mob",
                    "email", null);
            //sendFlurryEvent(context, "signup_android_mob_email", null);
        }
    }

    public static void sendEventRating45(Context context, Tracker tracker, String label) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "", "rating_45star", "android_tab",
                    label, null);
            //sendFlurryEvent(context, "rating_45star_android_tab", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "", "rating_45star", "android_mob",
                    label, null);
            //sendFlurryEvent(context, "rating_45star_android_mob", null);
        }
    }

    public static void sendEventRating123(Context context, Tracker tracker, String label) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "", "rating_123star", "android_tab",
                    label, null);
            //sendFlurryEvent(context, "rating_123star_android_tab", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "", "rating_123star", "android_mob",
                    label, null);
            //sendFlurryEvent(context, "rating_123star_android_mob", null);
        }
    }

    public static void sendEventBadRating(Context context, Tracker tracker, String label) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "", "rating_bad", "android_tab",
                    label, null);
            //sendFlurryEvent(context, "rating_bad_android_tab", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "", "rating_bad", "android_mob",
                    label, null);
            //sendFlurryEvent(context, "rating_bad_android_mob", null);
        }
    }

    public static void sendEventGoodRating(Context context, Tracker tracker, String label) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "", "rating_good", "android_tab",
                    label, null);
            //sendFlurryEvent(context, "rating_good_android_tab", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "", "rating_good", "android_mob",
                    label, null);
            //sendFlurryEvent(context, "rating_good_android_mob", null);
        }
    }


    public static void sendEventSettings(Context context, Tracker tracker) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            sendEventGoogleAnalytics(context, tracker, "settings", "settings", "android_tab",
                    null, null);
            //sendFlurryEvent(context, "settings_android_tab", null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "settings", "settings", "android_mob",
                    null, null);
            //sendFlurryEvent(context, "settings_android_mob", null);
        }
    }


    public static void sendEventPremium(Context context, Tracker tracker, HashMap<String, String> parameters) {
        if (Utils.isTabletMoreThanSevenInches(context)) {
            //AnalyticsUtils.sendFlurryEvent(context, "premium_android_tab", parameters);
            sendEventGoogleAnalytics(context, tracker, "premium", "premium", "android_tab",
                    null, null);
        } else {
            //AnalyticsUtils.sendFlurryEvent(context, "premium_android_mob", parameters);
            sendEventGoogleAnalytics(context, tracker, "premium", "premium", "android_mob",
                    null, null);
        }
    }


    public static void sendEventConnectionFailed(Context context, Tracker tracker, String label) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("host", label);
        if (Utils.isTabletMoreThanSevenInches(context)) {
            //AnalyticsUtils.sendFlurryEvent(context, "conn_failed_android_tab", parameters);
            sendEventGoogleAnalytics(context, tracker, "", "conn_failed", "android_tab",
                    label, null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "", "conn_failed", "android_mob",
                    label, null);
            //AnalyticsUtils.sendFlurryEvent(context, "conn_failed_android_mob", parameters);
        }
    }


    public static void sendEventTimedOut(Context context, Tracker tracker, String label) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("host", label);
        if (Utils.isTabletMoreThanSevenInches(context)) {
            //AnalyticsUtils.sendFlurryEvent(context, "timeout_android_tab", parameters);
            sendEventGoogleAnalytics(context, tracker, "", "timeout", "android_tab",
                    label, null);
        } else {
            sendEventGoogleAnalytics(context, tracker, "", "timeout", "android_mob",
                    label, null);
            // AnalyticsUtils.sendFlurryEvent(context, "timeout_android_mob", parameters);
        }
    }
}
