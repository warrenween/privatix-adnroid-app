package com.privatix;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.InterstitialCallbacks;
import com.appodeal.ads.SkippableVideoCallbacks;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.privatix.adapters.SpinnerAdapter;
import com.privatix.api.helper.ApiErrorListener;
import com.privatix.async.DownloadImageTask;
import com.privatix.async.LoadCertificatesTask;
import com.privatix.fragments.dialogs.ServersOverloadedDialog;
import com.privatix.fragments.dialogs.SimpleDialog;
import com.privatix.fragments.dialogs.UpdateAppVersionDialog;
import com.privatix.fragments.dialogs.VpnNotSupportedError;
import com.privatix.model.NotificationTable;
import com.privatix.model.ProfileTable;
import com.privatix.model.SubscriptionTable;
import com.privatix.services.AdNotificationService;
import com.privatix.services.MyVpnListenerService;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.Constants;
import com.privatix.utils.DialogsHelper;
import com.privatix.utils.PrefKeys;
import com.privatix.utils.Utils;
import com.privatix.utils.interfaces.MyVpnStateListener;
import com.privatix.utils.ui.MarqueeViewLeft;
import com.privatix.utils.ui.MarqueeViewRight;

import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.CharonVpnService;
import org.strongswan.android.logic.VpnStateService;
import org.strongswan.android.logic.imc.ImcState;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseMainActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener, ApiErrorListener, MyVpnStateListener, InterstitialCallbacks {
    public static final int PAUSE_BETWEEN_ANIMATION_MARQUEE = 200;
    public static final int SPEED_MARQUEE = 10;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PREPARE_VPN_SERVICE = 0;

    VpnProfile vpnProfile;

    List<SubscriptionTable> sortedSubscriptionList = new ArrayList<>();
    List<String> countries = new ArrayList<>();

    Spinner spinnerCountry;
    public SwitchCompat switchConnect;
    String userLoginVpn, userPasswordVpn, currentHostVpn = "", currentCountry = "", currentCountryCode = "", lastCountry = "";
    TextView tv_first_line, tv_second_line, tv_third_line, tv_fourth_line, tv_fifth_line,
            tv_sixth_line, tv_seventh_line, tv_eighth_line, tv_protection_status, tv_protection_tip,
            tv_account_status;
    RelativeLayout rel_spinner;
    LinearLayout ll_account_status, ll_video_ad_text;
    MarqueeViewLeft mv1, mv3, mv5, mv7;
    MarqueeViewRight mv2, mv4, mv6, mv8;
    ImageView iv_shield, iv_account_status, iv_url_banner;
    String protection_status;
    String protection_status_off;
    String protection_status_on;
    String protection_status_connecting;
    Boolean isVerified, isAuthorized, temp;
    String plan;

    int spinnerCurrentPosition = 0;
    SpinnerAdapter spinnerAdapter;
    boolean isFirstStart = true;
    boolean isWasConnected = true, isConnected = false;

    boolean firstOpen = true;
    SpannableString spannableString;
    boolean bootStart = false;
    SharedPreferences sp;
    int connectAdPeriod = -1, disconnectAdPeriod = -1, startAdPeriod = -1;
    int bannerAdNetworkPeriod = -1, bannerVideoTextPeriod = -1, bannerUrlPeriod = -1;
    int lockAdPeriod = -1, ttl = -1;
    String urlBannerLink, urlBannerImageLink;
    boolean startAdChecked = false;
    private Bundle mProfileInfo;
    private VpnProfileDataSource mDataSource;
    private long mErrorConnectionID = 0;
    private long mDismissedConnectionID = 0;
    private AdNotificationService mAdService;
    private final ServiceConnection mAdServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAdService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onAdServiceConnected");
            AdNotificationService.LocalBinder binder = (AdNotificationService.LocalBinder) service;
            mAdService = binder.getService();
        }
    };
    private String screenName;
    private BroadcastReceiver mNetworkStateCheckListener;
    private MyVpnListenerService myVpnListenerService;
    private final ServiceConnection myVpnListenerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            myVpnListenerService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "myVpnListenerServiceConnection connected");
            myVpnListenerService = ((MyVpnListenerService.LocalBinder) service).getService();
            myVpnListenerService.registerListener(MainActivity.this);
            handleChangeState();
            showNoConnectionDialog();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        screenName = "protection_off";

        sp = getSharedPreferences(PrefKeys.KEY_SHARED_PRIVATIX, MODE_PRIVATE);
        getProfileData();
        getSubscription();

        bindService(new Intent(this, MyVpnListenerService.class),
                myVpnListenerServiceConnection, Service.BIND_AUTO_CREATE);
        bindService(new Intent(this, AdNotificationService.class),
                mAdServiceConnection, Service.BIND_AUTO_CREATE);


        setContentView(R.layout.activity_main);

        setProtectionText();
        initView();

        mDataSource = new VpnProfileDataSource(this);
        mDataSource.open();

        /* load CA certificates in a background task */
        new LoadCertificatesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        setSpinnerCountry();
        startMarqueText();
        temp = isFreeAccount();

        checkSpeedNoVpn(currentProfile);

        Intent intent = getIntent();
        if (intent != null) {
            bootStart = intent.getBooleanExtra(Constants.BOOT_START_EXTRA, false);
        }

        getInterstitialData();
        getBannerData();
        getLockAdData();
        initializeAds();
        showStartAd();
        showVideoAdText();
        showAdNetworkBanner();
        showUrlBanner();

        AnalyticsUtils.checkForNotSentEvents(this, mTracker);

        showUpdateDialog();
    }


    public void showUpdateDialog() {
        Float currentAppVersion = Float.valueOf(Utils.getAppVersion(this));
        float appVersionMarket = sp.getFloat(PrefKeys.PLAY_MARKET_VERSION, 0.0f);
        int updateDialogCount = sp.getInt(PrefKeys.UPDATE_DIALOG_COUNT, 0);
        if (updateDialogCount <= 3 && currentAppVersion < appVersionMarket) {
            UpdateAppVersionDialog updateAppVersionDialog = UpdateAppVersionDialog.newInstance();
            updateAppVersionDialog.show(getSupportFragmentManager(), SimpleDialog.class.getSimpleName());
        }
    }


    public void initializeAds() {
        Appodeal.confirm(Appodeal.SKIPPABLE_VIDEO);
        Appodeal.disableLocationPermissionCheck();
        Appodeal.setTesting(false);
        Appodeal.setAutoCache(Appodeal.INTERSTITIAL, true);
        Appodeal.setOnLoadedTriggerBoth(Appodeal.INTERSTITIAL, false);
        Appodeal.initialize(this, getString(R.string.appodeal_key), Appodeal.INTERSTITIAL | Appodeal.BANNER_BOTTOM | Appodeal.SKIPPABLE_VIDEO);
        Appodeal.setInterstitialCallbacks(this);
    }


    public void getInterstitialData() {
        List<NotificationTable> notificationTableList = Select.from(NotificationTable.class)
                .where(Condition.prop("type").eq(getString(R.string.ad_type_adnetwork))).list();
        for (NotificationTable notificationTable : notificationTableList) {
            if (getString(R.string.ad_target_connect).equals(notificationTable.getTarget())) {
                connectAdPeriod = Integer.valueOf(notificationTable.getPeriod());
            } else if (getString(R.string.ad_target_disconnect).equals(notificationTable.getTarget())) {
                disconnectAdPeriod = Integer.valueOf(notificationTable.getPeriod());
            } else if (getString(R.string.ad_target_start).equals(notificationTable.getTarget())) {
                startAdPeriod = Integer.valueOf(notificationTable.getPeriod());
            }
        }
    }


    public void getBannerData() {
        List<NotificationTable> notificationTableList = Select.from(NotificationTable.class)
                .where(Condition.prop("type").eq(getString(R.string.ad_type_banner))).list();
        for (NotificationTable notificationTable : notificationTableList) {
            Log.e(TAG, "Format " + notificationTable.getFormat());
            if (getString(R.string.ad_format_adnetwork).equals(notificationTable.getFormat())) {
                bannerAdNetworkPeriod = Integer.valueOf(notificationTable.getPeriod());
            } else if (getString(R.string.ad_format_videotext).equals(notificationTable.getFormat())) {
                bannerVideoTextPeriod = Integer.valueOf(notificationTable.getPeriod());
            } else if (getString(R.string.ad_format_url).equals((notificationTable.getFormat()))) {
                bannerUrlPeriod = Integer.valueOf(notificationTable.getPeriod());
                urlBannerLink = notificationTable.getLink();
                urlBannerImageLink = notificationTable.getUrl();
            }
        }
    }


    public void getLockAdData() {
        List<NotificationTable> notificationTableList = Select.from(NotificationTable.class)
                .where(Condition.prop("type").eq(getString(R.string.ad_type_lock))).list();
        for (NotificationTable notificationTable : notificationTableList) {
            Log.e(TAG, "Target " + notificationTable.getTarget());
            if (getString(R.string.ad_target_connect).equals(notificationTable.getTarget())) {
                lockAdPeriod = Integer.valueOf(notificationTable.getPeriod());
                ttl = Integer.valueOf(notificationTable.getTtl());
            }
        }
    }


    public boolean showLockAd() {
        boolean result = true;
        int countLockAd = sp.getInt(PrefKeys.LOCK_AD_COUNT, -1);
        Log.d(TAG, "countLockAd=" + countLockAd + " lockAdPeriod =" + lockAdPeriod);
        Log.d(TAG, "countLockAd % lockAdPeriod=" + countLockAd % lockAdPeriod);
        if (lockAdPeriod != -1 && lockAdPeriod != 0) {
            if (countLockAd % lockAdPeriod == 0) {
                showOverLoadedDialog();
                result = false;
            }
            sp.edit().putInt(PrefKeys.LOCK_AD_COUNT, ++countLockAd).apply();
        }
        return result;
    }


    public void showAdNetworkBanner() {
        int countAdNetworkTextAd = sp.getInt(PrefKeys.ADNETWORK_AD_COUNT, -1);
        Log.d(TAG, "countAdNetworkTextAd=" + countAdNetworkTextAd + " bannerAdNetworkPeriod =" + bannerAdNetworkPeriod);
        Log.d(TAG, "countAdNetworkTextAd% bannerAdNetworkPeriod=" + countAdNetworkTextAd % bannerAdNetworkPeriod);
        if (bannerAdNetworkPeriod != -1 && bannerAdNetworkPeriod != 0) {
            if (countAdNetworkTextAd % bannerAdNetworkPeriod == 0) {
                Log.d(TAG, "show AdNetwork Ad");
                Appodeal.show(this, Appodeal.BANNER_BOTTOM);
            }
            sp.edit().putInt(PrefKeys.ADNETWORK_AD_COUNT, ++countAdNetworkTextAd).apply();
        }
    }


    public void showVideoAdText() {
        int countVideoTextAd = sp.getInt(PrefKeys.VIDEO_TEXT_AD_COUNT, -1);
        Log.d(TAG, "countVideoTextAd=" + countVideoTextAd + " bannerVideoTextPeriod =" + bannerVideoTextPeriod);
        Log.d(TAG, "countVideoTextAd% bannerVideoTextPeriod=" + countVideoTextAd % bannerVideoTextPeriod);
        if (bannerVideoTextPeriod != -1) {
            if (countVideoTextAd % bannerVideoTextPeriod == 0) {
                Log.d(TAG, "show VideoText Ad");
                ll_video_ad_text.setVisibility(View.VISIBLE);
            }
            sp.edit().putInt(PrefKeys.VIDEO_TEXT_AD_COUNT, ++countVideoTextAd).apply();
        }
    }


    public void showUrlBanner() {
        int countUrlBanner = sp.getInt(PrefKeys.URL_AD_COUNT, -1);
        Log.d(TAG, "countUrlBanner=" + countUrlBanner + " bannerUrlPeriod =" + bannerUrlPeriod);
        Log.d(TAG, "countUrlBanner% bannerUrlPeriod=" + countUrlBanner % bannerUrlPeriod);
        if (bannerUrlPeriod != -1 && bannerUrlPeriod != 0) {
            if (countUrlBanner % bannerUrlPeriod == 0) {
                Log.d(TAG, "show url banner");
                new DownloadImageTask(MainActivity.this, iv_url_banner, urlBannerLink)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, urlBannerImageLink);
            }
            sp.edit().putInt(PrefKeys.URL_AD_COUNT, ++countUrlBanner).apply();
        }
    }


    public void showStartAd() {
        startAdChecked = true;
        int countStartAd = sp.getInt(PrefKeys.START_AD_COUNT, -1);
        Log.d(TAG, "countStartAd=" + countStartAd + " startAdPeriod =" + startAdPeriod);
        Log.d(TAG, "countStartAd% startAdPeriod=" + countStartAd % startAdPeriod);
        if (startAdPeriod != -1) {
            if (countStartAd % startAdPeriod == 0) {
                Log.d(TAG, "show Start Ad");
                Appodeal.show(this, Appodeal.INTERSTITIAL);
            }
            sp.edit().putInt(PrefKeys.START_AD_COUNT, ++countStartAd).apply();
        }
    }


    public void showConnectAd() {
        int countConnectAd = sp.getInt(PrefKeys.CONNECT_AD_COUNT, -1);
        Log.d(TAG, "countConnectAd=" + countConnectAd + " connectAdPeriod =" + connectAdPeriod);
        Log.d(TAG, "countConnectAd% connectAdPeriod=" + countConnectAd % connectAdPeriod);
        if (connectAdPeriod != -1) {
            if (countConnectAd % connectAdPeriod == 0) {
                Log.d(TAG, "show ConnectAd Ad");
                Appodeal.show(this, Appodeal.INTERSTITIAL);
            }
            sp.edit().putInt(PrefKeys.CONNECT_AD_COUNT, ++countConnectAd).apply();
        }
    }


    public void showDisconnectAd() {
        int countDisconnectAd = sp.getInt(PrefKeys.DISCONNECT_AD_COUNT, -1);
        Log.d(TAG, "countDisconnectAd=" + countDisconnectAd + " disconnectAdPeriod =" + disconnectAdPeriod);
        Log.d(TAG, "countDisconnectAd% disconnectAdPeriod=" + countDisconnectAd % disconnectAdPeriod);
        if (disconnectAdPeriod != -1) {
            if (countDisconnectAd % disconnectAdPeriod == 0) {
                Log.d(TAG, "show DisconnectAd Ad");
                Appodeal.show(this, Appodeal.INTERSTITIAL);
            }
            sp.edit().putInt(PrefKeys.DISCONNECT_AD_COUNT, ++countDisconnectAd).apply();
        }
    }


    public void showNoConnectionDialog() {
        Intent intent = getIntent();
        if (intent != null) {
            boolean isConnectionActive = intent.getBooleanExtra(Constants.CONNECTION_STATE, true);
            boolean wasConnected = myVpnListenerService.getState() != null && myVpnListenerService.getState() == VpnStateService.State.CONNECTED;
            Log.d(TAG, "wasConnected " + wasConnected);
            if (!wasConnected && !isConnectionActive)
                showNoConnectionCloseAppDialog(R.string.error_internet_connection);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        getProfileData();
        handleAccountStatus(isVerified, isAuthorized);
//        Log.d(TAG, "onResume call");
//        Log.d(TAG, "onResume plan " + plan);
//        Log.d(TAG, "onResume isFreeAccount " + isFreeAccount() + "");
//        Log.d(TAG, "onResume temp" + temp);
        if (temp != isFreeAccount()) {
            spinnerAdapter.accountTypeChanged(isFreeAccount());
        }
        int currentSpinnerPosition = spinnerCountry.getSelectedItemPosition() + 1;
        int freeCountryCount = Select.from(SubscriptionTable.class)
                .where(Condition.prop("is_free").eq("1")).list().size();
        if (isFreeAccount() && currentSpinnerPosition > freeCountryCount) {
            Log.e(TAG, "onResume change to 0");
            spinnerCountry.setSelection(0);
        }
        isFirstStart = false;
        checkIfHasNotSavedPurchase();
    }


    public void setProtectionText() {
        protection_status = getString(R.string.protection_status_text_view);
        protection_status_off = getString(R.string.protection_status_off_text_view);
        protection_status_on = getString(R.string.protection_status_on_text_view);
        protection_status_connecting = getString(R.string.protection_status_connecting_text_view);
    }


    public void getProfileData() {
        List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
        if (profileTables.size() > 0) {
            ProfileTable currentProfile = profileTables.get(0);
            isAuthorized = currentProfile.isAuthorized();
            isVerified = currentProfile.isVerified();
            plan = currentProfile.getPlan();
            userLoginVpn = currentProfile.getCurrentVpnLogin();
            userPasswordVpn = currentProfile.getCurrentVpnPassword();
        } else {
            startActivity(new Intent(this, GetStartedActivity.class));
            finish();
        }
    }

    public void getSubscription() {
        sortedSubscriptionList.clear();
        countries.clear();

        //first take free
        sortedSubscriptionList.addAll(Select.from(SubscriptionTable.class)
                .where(Condition.prop("is_free").eq("1")).list());

        sortedSubscriptionList.addAll(Select.from(SubscriptionTable.class)
                .where(Condition.prop("is_free").eq("0")).list());


        String lastConnectedCountry = sp.getString(PrefKeys.LAST_CONNECTION_COUNTRY, "");
        for (int i = 0; i < sortedSubscriptionList.size(); i++) {
            SubscriptionTable currentSubscription = sortedSubscriptionList.get(i);
            String country = currentSubscription.getCountry();
            if (!TextUtils.isEmpty(lastConnectedCountry) && country.equals(lastConnectedCountry)) {
                if (currentSubscription.getIsFree() || !isFreeAccount()) {
                    int index = sortedSubscriptionList.indexOf(currentSubscription);
                    sortedSubscriptionList.remove(index);
                    sortedSubscriptionList.add(0, currentSubscription);
                    break;
                }
            }
        }
    }

    public void changeToProtected(boolean isAnimate, boolean isFromShield) {
        tv_first_line.setText(R.string.text_protected_first);
        tv_second_line.setText(R.string.text_protected_second);
        tv_third_line.setText(R.string.text_protected_third);
        tv_fourth_line.setText(R.string.text_protected_fourth);
        tv_fifth_line.setText(R.string.text_protected_fifth);
        tv_sixth_line.setText(R.string.text_protected_sixth);
        tv_seventh_line.setText(R.string.text_protected_seventh);
        tv_eighth_line.setText(R.string.text_protected_eighth);

        if (isFromShield) {
            switchConnect.setOnCheckedChangeListener(null);
            switchConnect.setChecked(true);
            switchConnect.setOnCheckedChangeListener(this);
        }
        switchConnect.setText(getString(R.string.switch_text_on));
        if (isAnimate) {
            Animation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
            fadeOut.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    iv_shield.setImageResource(R.drawable.shield_active);
                    Animation fadeIn = new AlphaAnimation(0, 1);
                    fadeIn.setInterpolator(new AccelerateInterpolator()); //and this
                    fadeIn.setStartOffset(getResources().getInteger(android.R.integer.config_shortAnimTime));
                    fadeIn.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
                    fadeIn.setFillAfter(true);
                    iv_shield.setAnimation(fadeIn);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            iv_shield.setAnimation(fadeOut);
        } else
            iv_shield.setImageResource(R.drawable.shield_active);
    }


    public void changeToNotProtected() {
        tv_first_line.setText(R.string.text_not_protected_first);
        tv_second_line.setText(R.string.text_not_protected_second);
        tv_third_line.setText(R.string.text_not_protected_third);
        tv_fourth_line.setText(R.string.text_not_protected_fourth);
        tv_fifth_line.setText(R.string.text_not_protected_fifth);
        tv_sixth_line.setText(R.string.text_not_protected_sixth);
        tv_seventh_line.setText(R.string.text_not_protected_seventh);
        tv_eighth_line.setText(R.string.text_not_protected_eighth);
        try {
            iv_shield.setImageResource(R.drawable.shield_broken);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        switchConnect.setChecked(false);
        switchConnect.setText(getString(R.string.switch_text_off));
        switchConnect.setOnCheckedChangeListener(this);
    }

    public boolean isFreeAccount() {
        return "free".equals(plan);
    }


    public void handleAccountStatus(boolean isVerified, boolean isAuthorized) {
        if (isFreeAccount()) {
            ll_account_status.setVisibility(View.VISIBLE);
            if (!isVerified && !isAuthorized) {
                tv_account_status.setText(R.string.status_sing_up);
                iv_account_status.setImageResource(R.drawable.sign_up_icon);
            } else if (isAuthorized && !isVerified) {
                tv_account_status.setText(R.string.status_verify_email);
                iv_account_status.setImageResource(R.drawable.mail_checked);
            } else if (isAuthorized) {
                tv_account_status.setText(R.string.status_upgrage);
                iv_account_status.setImageResource(R.drawable.premium_star_big);
            } else {
                tv_account_status.setText(R.string.status_sing_up);
                iv_account_status.setImageResource(R.drawable.sign_up_icon);
            }
        } else {
            if (isAuthorized && !isVerified) {
                ll_account_status.setVisibility(View.VISIBLE);
                tv_account_status.setText(R.string.status_verify_email);
                iv_account_status.setImageResource(R.drawable.mail_checked);
            } else {
                ll_account_status.setVisibility(View.GONE);
            }
        }
    }


    public void startMarqueText() {
        mv1 = (MarqueeViewLeft) findViewById(R.id.marqueeView100);
        mv1.setPauseBetweenAnimations(PAUSE_BETWEEN_ANIMATION_MARQUEE);
        mv1.setSpeed(SPEED_MARQUEE);

        mv2 = (MarqueeViewRight) findViewById(R.id.marqueeView200);
        mv2.setPauseBetweenAnimations(PAUSE_BETWEEN_ANIMATION_MARQUEE);
        mv2.setSpeed(SPEED_MARQUEE);

        mv3 = (MarqueeViewLeft) findViewById(R.id.marqueeView300);
        mv3.setPauseBetweenAnimations(PAUSE_BETWEEN_ANIMATION_MARQUEE);
        mv3.setSpeed(SPEED_MARQUEE);

        mv4 = (MarqueeViewRight) findViewById(R.id.marqueeView400);
        mv4.setPauseBetweenAnimations(PAUSE_BETWEEN_ANIMATION_MARQUEE);
        mv4.setSpeed(SPEED_MARQUEE);

        mv5 = (MarqueeViewLeft) findViewById(R.id.marqueeView500);
        mv5.setPauseBetweenAnimations(PAUSE_BETWEEN_ANIMATION_MARQUEE);
        mv5.setSpeed(SPEED_MARQUEE);

        mv6 = (MarqueeViewRight) findViewById(R.id.marqueeView600);
        mv6.setPauseBetweenAnimations(PAUSE_BETWEEN_ANIMATION_MARQUEE);
        mv6.setSpeed(SPEED_MARQUEE);

        mv7 = (MarqueeViewLeft) findViewById(R.id.marqueeView700);
        mv7.setPauseBetweenAnimations(PAUSE_BETWEEN_ANIMATION_MARQUEE);
        mv7.setSpeed(SPEED_MARQUEE);

        mv8 = (MarqueeViewRight) findViewById(R.id.marqueeView800);
        mv8.setPauseBetweenAnimations(PAUSE_BETWEEN_ANIMATION_MARQUEE);
        mv8.setSpeed(SPEED_MARQUEE);

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mv1.startMarquee();
            }
        });
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mv2.startMarquee();
            }
        });
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mv3.startMarquee();
            }
        });
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mv4.startMarquee();
            }
        });
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mv5.startMarquee();
            }
        });
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mv6.startMarquee();
            }
        });
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mv7.startMarquee();
            }
        });
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mv8.startMarquee();
            }
        });
    }


    public void showDisabledStatus() {
        changeControllersStatus(true);
        changeToNotProtected();
        spinnerCountry.setOnItemSelectedListener(this);
        tv_protection_tip.setText(spannableString);
        tv_protection_status.setText(Html.fromHtml(protection_status + " " + protection_status_off));
    }


    public void showConnectedStatus() {
        changeControllersStatus(true);
        currentHostVpn = myVpnListenerService.getProfile().getGateway();
        currentCountry = myVpnListenerService.getProfile().getName();
        currentCountryCode = myVpnListenerService.getProfile().getCountryCode();
        lastCountry = currentCountry;
        int positionOfSpinner = countries.indexOf(currentCountry);
        spinnerCountry.setSelection(positionOfSpinner);
        spinnerCountry.setOnItemSelectedListener(this);
        changeToProtected(false, true);
        tv_protection_tip.setText(R.string.protection_tip_on);
        tv_protection_status.setText(Html.fromHtml(protection_status + " " + protection_status_on));

    }


    public void changeControllersStatus(boolean isEnabled) {
        switchConnect.setEnabled(isEnabled);
        iv_shield.setEnabled(isEnabled);
    }


    public void startVideoServersOverloaded() {
        Appodeal.setSkippableVideoCallbacks(new SkippableVideoCallbacks() {
            @Override
            public void onSkippableVideoLoaded() {
                Log.e(TAG, "onSkippableVideoLoaded");
            }

            @Override
            public void onSkippableVideoFailedToLoad() {
                Log.e(TAG, "onSkippableVideoFailedToLoad");
            }

            @Override
            public void onSkippableVideoShown() {
                Log.e(TAG, "onSkippableVideoShown");
            }

            @Override
            public void onSkippableVideoFinished() {
                Log.e(TAG, "onSkippableVideoFinished");
                startVpnAfterAd();
            }

            @Override
            public void onSkippableVideoClosed(boolean b) {
                Log.e(TAG, "onSkippableVideoClosed");
                startVpnAfterAd();
            }
        });
        Appodeal.show(this, Appodeal.SKIPPABLE_VIDEO);
    }


    public void startVpnAfterAd() {
        setAndStartVpn();
        changeToProtected(true, true);
    }

    public void showOverLoadedDialog() {
        ServersOverloadedDialog serversOverloadedDialog = ServersOverloadedDialog.newInstance(ttl);
        serversOverloadedDialog.setCancelable(false);
        try {
            serversOverloadedDialog.show(getSupportFragmentManager(), ServersOverloadedDialog.class.getSimpleName());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }


    public void handleChangeState() {
        Log.e(TAG, "handleChangeState");
        long connectionID = myVpnListenerService.getConnectionID();
        //Log.d(TAG, "connectionID: " + connectionID);
        VpnStateService.State state = myVpnListenerService.getState();
        VpnStateService.ErrorState error = myVpnListenerService.getErrorState();
        ImcState imcState = myVpnListenerService.getImcState();

        if (reportError(connectionID, error, imcState)) {
            return;
        }

        switch (state) {
            case DISABLED:
                showDisabledStatus();
                if (isConnected)
                    showDisconnectAd();
                isConnected = false;
                isWasConnected = false;
                stopCheckReceiver();
                if (mAdService != null)
                    mAdService.startNotificationTimers();

                screenName = "protection_off";

                //sp.edit().putBoolean(PrefKeys.VPN_STATUS, false).apply();

                int countryId = spinnerCountry.getSelectedItemPosition();
                currentHostVpn = sortedSubscriptionList.get(countryId).getHosts().get(0).getHost();
                //Log.e(TAG, "Disabled boot start "+ bootStart);
                if (bootStart) {
                    switchVpnOn();
                    bootStart = false;
                }
                break;
            case CONNECTING:
                tv_protection_status.setText(Html.fromHtml(protection_status_connecting));
                changeControllersStatus(false);
                break;
            case SERVER_DOWN:
                tv_protection_status.setText(Html.fromHtml(protection_status_connecting));
                changeControllersStatus(true);
                break;
            case CONNECTED:
                Log.d(TAG, "connected");
                showConnectedStatus();

                if (!isWasConnected) {
                    Log.d("ratingDialog", "need");
                    checkIfNeedReviewDialog();
                    showConnectAd();
                }
                if (mAdService != null) {
                    mAdService.startNotificationTimers();
                }
                isWasConnected = true;
                isConnected = true;

                DialogsHelper.showFirstSuccessfulConnectionDialog(sp, MainActivity.this, getSupportFragmentManager());
                screenName = "protection_on";
                startCheckReceiver();
                //Appodeal.show(this, Appodeal.SKIPPABLE_VIDEO);
                break;
            case DISCONNECTING:
                changeControllersStatus(false);
                Log.d(TAG, "handleChangeState DISCONNECTING");
                break;
        }
    }


    public void initView() {
        initSpannableString();
        spinnerCountry = (Spinner) findViewById(R.id.spinner_country);
        tv_first_line = (TextView) findViewById(R.id.tv_first_line);
        tv_second_line = (TextView) findViewById(R.id.tv_second_line);
        tv_third_line = (TextView) findViewById(R.id.tv_third_line);
        tv_fourth_line = (TextView) findViewById(R.id.tv_fourth_line);
        tv_fifth_line = (TextView) findViewById(R.id.tv_fifth_line);
        tv_sixth_line = (TextView) findViewById(R.id.tv_sixth_line);
        tv_seventh_line = (TextView) findViewById(R.id.tv_seventh_line);
        tv_eighth_line = (TextView) findViewById(R.id.tv_eighth_line);
        tv_protection_status = (TextView) findViewById(R.id.tv_protection_status);
        tv_protection_tip = (TextView) findViewById(R.id.tv_protection_tip);
        tv_account_status = (TextView) findViewById(R.id.tv_account_status);
        iv_account_status = (ImageView) findViewById(R.id.iv_account_status);
        ll_account_status = (LinearLayout) findViewById(R.id.ll_account_status);
        iv_shield = (ImageView) findViewById(R.id.iv_shield);
        switchConnect = (SwitchCompat) findViewById(R.id.switch_connect);
        rel_spinner = (RelativeLayout) findViewById(R.id.rel_spinner);
        iv_url_banner = (ImageView) findViewById(R.id.iv_url_banner);
        ll_video_ad_text = (LinearLayout) findViewById(R.id.ll_video_ad_text);

        tv_protection_status.setText(Html.fromHtml(protection_status + " " + protection_status_off));
        switchConnect.setOnCheckedChangeListener(this);
        iv_shield.setOnClickListener(this);
        ll_account_status.setOnClickListener(this);
        ll_video_ad_text.setOnClickListener(this);

        tv_protection_tip.setMovementMethod(LinkMovementMethod.getInstance());
        spannableString.setSpan(new ForegroundColorSpan(tv_protection_tip.getCurrentTextColor()), 84, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv_protection_tip.setText(spannableString);
        tv_protection_tip.setHighlightColor(Color.TRANSPARENT);

    }


    public void initSpannableString() {
        String protectionTipOff = getString(R.string.protection_tip_off);
        spannableString = new SpannableString(protectionTipOff);
        ClickableSpan clickableSpanSignUp = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                if (myVpnListenerService != null && myVpnListenerService.getState() != VpnStateService.State.CONNECTING &&
                        myVpnListenerService.getState() != VpnStateService.State.DISCONNECTING) {
                    boolean shouldContinue = showLockAd();
                    if (shouldContinue) {
                        setAndStartVpn();
                        changeToProtected(true, true);
                    }
                }
            }
        };
        spannableString.setSpan(clickableSpanSignUp, 84, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }


    public void setAndStartVpn() {
        vpnProfile = mDataSource.getVpnProfile(1);
        if (currentCountry.isEmpty() || currentHostVpn.isEmpty()) {
            if (sortedSubscriptionList.size() > 0) {
                currentCountry = sortedSubscriptionList.get(0).getCountry();
                currentHostVpn = sortedSubscriptionList.get(0).getHosts().get(0).getHost();
                currentCountryCode = sortedSubscriptionList.get(0).getCountryCode();
            }
        }
        lastCountry = currentCountry;
        if (vpnProfile == null) {
            vpnProfile = new VpnProfile();
            vpnProfile.setName(currentCountry);
            vpnProfile.setVpnType(VpnType.IKEV2_EAP);
            vpnProfile.setGateway(currentHostVpn);
            vpnProfile.setUsername(userLoginVpn);
            vpnProfile.setPassword(userPasswordVpn);
            vpnProfile.setCountryCode(currentCountryCode);
            mDataSource.insertProfile(vpnProfile);
        } else {
            vpnProfile.setName(currentCountry);
            vpnProfile.setVpnType(VpnType.IKEV2_EAP);
            vpnProfile.setGateway(currentHostVpn);
            vpnProfile.setUsername(userLoginVpn);
            vpnProfile.setPassword(userPasswordVpn);
            vpnProfile.setCountryCode(currentCountryCode);
            mDataSource.updateVpnProfile(vpnProfile);
        }

        Log.d(TAG, "vpn profile id "+ vpnProfile.getId() + "");
        Log.d(TAG,"vpn profile name" + vpnProfile.getName() + "name");
        Log.d(TAG,"vpn profile userName "+ vpnProfile.getUsername() + " my");
        Log.d(TAG,"vpn profile password "+ vpnProfile.getPassword() + " pass");
        Log.d(TAG, "vpn profile gateway" + vpnProfile.getGateway() + " gateway");

        Bundle profileInfo = new Bundle();
        profileInfo.putLong(VpnProfileDataSource.KEY_ID, vpnProfile.getId());
        profileInfo.putString(VpnProfileDataSource.KEY_USERNAME, vpnProfile.getUsername());
        profileInfo.putString(VpnProfileDataSource.KEY_PASSWORD, vpnProfile.getPassword());
        startVpnProfile(profileInfo);
    }


    public void setSpinnerCountry() {
        String[] countryNames = new String[sortedSubscriptionList.size()];
        for (int i = 0; i < sortedSubscriptionList.size(); i++) {
            SubscriptionTable currentSubscription = sortedSubscriptionList.get(i);
            String countryName = currentSubscription.getCountry();
            countryNames[i] = countryName;
            countries.add(countryName);
        }

        spinnerAdapter = new SpinnerAdapter(MainActivity.this, R.layout.spinner_item, countryNames,
                sortedSubscriptionList, isFreeAccount());
        spinnerCountry.setAdapter(spinnerAdapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.e("onItemSelected", position + "call");

        if (sortedSubscriptionList.get(position).getIsFree() || !isFreeAccount()) {
            spinnerAdapter.setCurrentSelectedItemPosition(position);
            //GA - selected new country
            if (!firstOpen) {
                AnalyticsUtils.sendEventSelectedCountry(MainActivity.this, mTracker, screenName, sortedSubscriptionList.get(position).getCountryCode());
            } else {
                firstOpen = false;

            }
            spinnerCurrentPosition = position;
            currentHostVpn = sortedSubscriptionList.get(position).getHosts().get(0).getHost();
            currentCountry = sortedSubscriptionList.get(position).getCountry();
            currentCountryCode = sortedSubscriptionList.get(position).getCountryCode();
            Log.e("onItemSelected", "last country " + lastCountry + " ,current country " + currentCountry);
            if (myVpnListenerService != null && (myVpnListenerService.getState() == VpnStateService.State.CONNECTED || myVpnListenerService.getState() == VpnStateService.State.CONNECTING)
                    && !lastCountry.equals(currentCountry)) {
                boolean shouldContinue = showLockAd();
                if (shouldContinue)
                    setAndStartVpn();
                else
                    myVpnListenerService.disconnect();
            }
        } else {
            startPremiumScreen();
            Log.e("onItemSelected", "Premium last country " + lastCountry + " ,current country " + currentCountry);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int index = countries.indexOf(lastCountry);
                    Log.e(TAG, "onItemSelected premium index " + index);
                    if(index!=-1)
                        spinnerCountry.setSelection(index);
                }
            }, 100);

        }
    }


    public void startPremiumScreen() {
        Intent intent = new Intent(MainActivity.this, PremiumActivity.class);
        startActivity(intent);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d("onNothingSelected", "call");
        currentHostVpn = sortedSubscriptionList.get(0).getHosts().get(0).getHost();
    }

    private void startVpnProfile(Bundle profileInfo) {
        prepareVpnService(profileInfo);
    }

    /**
     * Prepare the VpnService. If this succeeds the current VPN profile is
     * started.
     *
     * @param profileInfo a bundle containing the information about the profile to be started
     */
    protected void prepareVpnService(Bundle profileInfo) {
        Intent intent;
        //FIX: NOT REINSTALL - ANDROID ASK PERMISSION BUG
        try {
            intent = VpnService.prepare(this);
        } catch (IllegalStateException ex) {
            /* this happens if the always-on VPN feature (Android 4.2+) is activated */
            VpnNotSupportedError.showWithMessage(this, R.string.vpn_not_supported_during_lockdown);
            return;
        } catch (NullPointerException e) {
            Utils.showToast("Something wrong. Try to reload app");
            return;
        }
        /* store profile info until the user grants us permission */
        mProfileInfo = profileInfo;
        if (intent != null) {
            try {
                startActivityForResult(intent, PREPARE_VPN_SERVICE);
            } catch (ActivityNotFoundException ex) {
                /* it seems some devices, even though they come with Android 4,
                 * don't have the VPN components built into the system image.
				 * com.android.vpndialogs/com.android.vpndialogs.ConfirmDialog
				 * will not be found then */
                VpnNotSupportedError.showWithMessage(this, R.string.vpn_not_supported);
            }
        } else {	/* user already granted permission to use VpnService */
            onActivityResult(PREPARE_VPN_SERVICE, RESULT_OK, null);
        }
    }

    private boolean reportError(long connectionID, VpnStateService.ErrorState error, ImcState imcState) {
        if (connectionID > mDismissedConnectionID) {	/* report error if it hasn't been dismissed yet */
            mErrorConnectionID = connectionID;
        } else {	/* ignore all other errors */
            error = VpnStateService.ErrorState.NO_ERROR;
        }
        if (error == VpnStateService.ErrorState.NO_ERROR) {
            hideErrorDialog();
            return false;
        } else if (mErrorDialog != null) {	/* we already show the dialog */
            return true;
        }

        switch (error) {
            case AUTH_FAILED:
                if (imcState == ImcState.BLOCK) {
                    showErrorDialog(R.string.error_assessment_failed);
                } else {
                    showErrorDialog(R.string.error_auth_failed);
                }
                break;
            case PEER_AUTH_FAILED:
                showErrorDialog(R.string.error_peer_auth_failed);
                break;
            case LOOKUP_FAILED:
                showSimpleErrorDialog(R.string.error_internet_connection);
                break;
            case UNREACHABLE:
                showErrorDialog(R.string.error_unreachable);
                break;
            default:
                //MOST LIKELY ANDROID KNOWN BUG. ASK USER TO REBOOT DEVICE
                showErrorDialog(R.string.error_generic);
                break;
        }
        return true;
    }


    public void clearError() {
        if (myVpnListenerService != null) {
            myVpnListenerService.setError(VpnStateService.ErrorState.NO_ERROR);
            myVpnListenerService.disconnect();
        }
        mDismissedConnectionID = mErrorConnectionID;
        handleChangeState();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PREPARE_VPN_SERVICE:
                if (resultCode == RESULT_OK && mProfileInfo != null) {
                    Intent intent = new Intent(this, CharonVpnService.class);
                    intent.putExtras(mProfileInfo);
                    this.startService(intent);
                } else {
                    changeToNotProtected();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void recycleFlagsArray() {
        if (spinnerAdapter != null)
            spinnerAdapter.recycleFlagsArray();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        stopCheckReceiver();
        recycleFlagsArray();
        if (mAdService != null)
            this.unbindService(mAdServiceConnection);

        if (myVpnListenerService != null) {
            this.unbindService(myVpnListenerServiceConnection);
            myVpnListenerService.unregisterListener(this);
        }
        mDataSource.close();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_shield:
                if (myVpnListenerService != null && myVpnListenerService.getState() == VpnStateService.State.DISABLED) {
                    boolean shouldContinue = showLockAd();
                    if (shouldContinue) {
                        setAndStartVpn();
                        changeToProtected(true, true);
                    }
                    //GA - protected from shield
                    AnalyticsUtils.sendEventProtectedFromShield(MainActivity.this, mTracker, screenName);

                } else if (myVpnListenerService != null) {
                    switchConnect.setOnCheckedChangeListener(null);
                    myVpnListenerService.disconnect();
                    changeToNotProtected();

                    //GA - protected off from shield
                    AnalyticsUtils.sendEventProtectionOffFromShield(MainActivity.this, mTracker, screenName);
                }
                break;
            case R.id.ll_account_status:
                if (isFreeAccount()) {
                    if (!isVerified && !isAuthorized) {
                        startActivity(new Intent(MainActivity.this, SignUpActivity.class));
                    } else if (isAuthorized && !isVerified) {
                        //Nothing to do
                    } else if (isAuthorized && isVerified) {
                        startPremiumScreen();
                    } else {
                        startActivity(new Intent(MainActivity.this, SignUpActivity.class));
                    }
                }
                break;
            case R.id.ll_video_ad_text:
                Appodeal.show(this, Appodeal.SKIPPABLE_VIDEO);
                break;
        }
    }

    public void switchVpnOn() {
        boolean shouldContinue = showLockAd();
        if (shouldContinue) {
            setAndStartVpn();
            changeToProtected(true, false);
        }

        //GA - protected on from switch
        AnalyticsUtils.sendEventProtectedFromSwitch(MainActivity.this, mTracker, screenName);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //Log.e(TAG, "onCheckedChanged");
        if (isChecked) {
            if (myVpnListenerService != null && myVpnListenerService.getState() == VpnStateService.State.DISCONNECTING) {
                Utils.showToast("Disconnecting");
            } else if (myVpnListenerService != null && myVpnListenerService.getState() == VpnStateService.State.DISABLED) {
                switchVpnOn();
            }
        } else {
            changeToNotProtected();

            //GA - protected off from switch
            AnalyticsUtils.sendEventProtectionOffFromSwitch(MainActivity.this, mTracker, screenName);

            if (myVpnListenerService != null) {
                myVpnListenerService.disconnect();
            }
        }
    }

    @Override
    public void onError(int errorStatus, int errorCode, String error) {
        Log.e("onError", error);
    }

    @Override
    public void stateChanged() {
        handleChangeState();
    }

    public void startCheckReceiver() {
        stopCheckReceiver();
        mNetworkStateCheckListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Utils.isNetworkAvailable(getApplicationContext()) && isWasConnected) {
                    tv_protection_status.setText(Html.fromHtml(protection_status + " " + protection_status_on));
                } else {
                    tv_protection_status.setText(Html.fromHtml(protection_status_connecting));
                }
            }
        };
        registerReceiver(
                mNetworkStateCheckListener,
                new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void stopCheckReceiver() {
        if (mNetworkStateCheckListener != null) {
            try {
                unregisterReceiver(mNetworkStateCheckListener);
                mNetworkStateCheckListener = null;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onInterstitialLoaded(boolean isPrecache) {
        Log.e(TAG, "onInterstitialLoaded " + isPrecache);
        if (!startAdChecked)
            showStartAd();
    }

    @Override
    public void onInterstitialFailedToLoad() {
        Log.e(TAG, "onInterstitialFailedToLoad");
    }

    @Override
    public void onInterstitialShown() {
        Log.e(TAG, "onInterstitialShown");
    }

    @Override
    public void onInterstitialClicked() {
        Log.e(TAG, "onInterstitialClicked");
    }

    @Override
    public void onInterstitialClosed() {
        Log.e(TAG, "onInterstitialClosed");
    }
}
