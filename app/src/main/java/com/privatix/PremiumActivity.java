package com.privatix;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.privatix.api.ApiClient;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.UserPaymentWrapper;
import com.privatix.api.models.request.UserPayment;
import com.privatix.fragments.dialogs.SimpleDialog;
import com.privatix.model.ProfileTable;
import com.privatix.model.PurchaseTable;
import com.privatix.utils.AnalyticsUtils;
import com.privatix.utils.Constants;
import com.privatix.utils.DatabaseUtils;
import com.privatix.utils.Helper;
import com.privatix.utils.Utils;
import com.privatix.utils.purchase.Base64;
import com.privatix.utils.purchase.IabBroadcastReceiver;
import com.privatix.utils.purchase.IabHelper;
import com.privatix.utils.purchase.IabResult;
import com.privatix.utils.purchase.Inventory;
import com.privatix.utils.purchase.Purchase;
import com.privatix.utils.purchase.SkuDetails;
import com.scottyab.aescrypt.AESCrypt;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class PremiumActivity extends BaseAnalyticsActivity implements DialogInterface.OnClickListener, IabBroadcastReceiver.IabBroadcastListener {
    static final int RC_REQUEST = 10001;
    private static final String TAG = PremiumActivity.class.getSimpleName();
    String[] premium_title_green, premium_title_black, premium_messages;
    TypedArray imgs;
    Button btn_buy_monthly, btn_buy_yearly;
    ImageView iv_first_dot, iv_second_dot, iv_third_dot;
    IabHelper mHelper;
    String base64EncodedPublicKey;
    Boolean isBillingSet = false;
    String mPremiumSku = "", mFirstChoiceSku = "", mSecondChoiceSku = "";
    // Will the subscription auto-renew?
    boolean mAutoRenewEnabled = false;
    // Does the user have an active subscription to the premium plan?
    boolean mSubscribedToPremium = false;
    //If was purchase
    boolean ifWasPurchase = false;
    // Used to select between purchasing premium on a monthly or yearly basis
    String mSelectedSubscriptionPeriod = "";
    boolean isAuthorized = false;
    String email, sid;
    ProgressDialog progressDialog;
    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;
    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {

            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(Constants.SKU_PREMIUM_MONTHLY)
                    || purchase.getSku().equals(Constants.SKU_PREMIUM_YEARLY)) {
                showProgressDialog();
                // bought the premium subscription
                //Log.d("Purchase", purchase.getOriginalJson());
                //Log.d("Signature", purchase.getSignature());
                Log.d(TAG, "Premium subscription purchased.");

                ifWasPurchase = true;

                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }


                //alert("Thank you for subscribing to premium!");
                mSubscribedToPremium = true;
                mAutoRenewEnabled = purchase.isAutoRenewing();
                Log.d(TAG, "User purchase mAutoRenewEnabled " + mAutoRenewEnabled);
                mPremiumSku = purchase.getSku();

                JSONObject jsonObject = new JSONObject();
                try {
                    JSONObject jsonObjectPurchaseDetails = new JSONObject();
                    jsonObjectPurchaseDetails.put("orderId", purchase.getOrderId());
                    jsonObjectPurchaseDetails.put("packageName", purchase.getPackageName());
                    jsonObjectPurchaseDetails.put("productId", purchase.getSku());
                    jsonObjectPurchaseDetails.put("purchaseTime", purchase.getPurchaseTime());
                    jsonObjectPurchaseDetails.put("purchaseState", purchase.getPurchaseState());
                    jsonObjectPurchaseDetails.put("developerPayload", purchase.getDeveloperPayload());
                    jsonObjectPurchaseDetails.put("purchaseToken", purchase.getToken());
                    jsonObjectPurchaseDetails.put("autoRenewing", purchase.isAutoRenewing());
                    jsonObject.put("purchaseDetails", jsonObjectPurchaseDetails);
                    jsonObject.put("Signature", purchase.getSignature());
                    //Log.d("Json purchase", jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                byte[] data = new byte[0];
                try {
                    data = jsonObject.toString().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String base64 = Base64.encodeWebSafe(data, false);
                PurchaseTable purchaseTable = new PurchaseTable(base64, sid);
                purchaseTable.save();
                sendPurchaseData(base64);
            }

        }
    };
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private String mSlideName = "premium_slide_1";
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */


            // First find out which subscription is auto renewing
            Purchase premiumMonthly = inventory.getPurchase(Constants.SKU_PREMIUM_MONTHLY);
            Purchase premiumYearly = inventory.getPurchase(Constants.SKU_PREMIUM_YEARLY);
            if (premiumMonthly != null && premiumMonthly.isAutoRenewing()) {
                mPremiumSku = Constants.SKU_PREMIUM_MONTHLY;
                mAutoRenewEnabled = true;
            } else if (premiumYearly != null && premiumYearly.isAutoRenewing()) {
                mPremiumSku = Constants.SKU_PREMIUM_YEARLY;
                mAutoRenewEnabled = true;
            } else {
                mPremiumSku = "";
                mAutoRenewEnabled = false;
            }

            SkuDetails skuDetailsMonth = inventory.getSkuDetails(Constants.SKU_PREMIUM_MONTHLY);
            SkuDetails skuDetailsYearly = inventory.getSkuDetails(Constants.SKU_PREMIUM_YEARLY);
            if (skuDetailsMonth != null && skuDetailsYearly != null) {
                String priceMonthly = skuDetailsMonth.getPrice();
                String priceYearly = skuDetailsYearly.getPrice();
                btn_buy_monthly.setText(priceMonthly);
                btn_buy_yearly.setText(priceYearly);
            } else {
                btn_buy_monthly.setText(R.string.price_month);
                btn_buy_yearly.setText(R.string.price_year);
            }

            // The user is subscribed if either subscription exists, even if neither is auto
            // renewing
            mSubscribedToPremium = (premiumMonthly != null && verifyDeveloperPayload(premiumMonthly))
                    || (premiumYearly != null && verifyDeveloperPayload(premiumYearly));

            Log.d(TAG, "User " + (mSubscribedToPremium ? "HAS" : "DOES NOT HAVE")
                    + " premium subscription.");
            Log.d(TAG, "User mAutoRenewEnabled" + mAutoRenewEnabled);
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");


            if (ifWasPurchase)
                AnalyticsUtils.sendEventSuccessPurchaseDetails(PremiumActivity.this, mTracker, mSlideName, inventory);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Resources res = getResources();
        premium_title_green = res.getStringArray(R.array.premium_title_green);
        premium_title_black = res.getStringArray(R.array.premium_title_black);
        premium_messages = res.getStringArray(R.array.premium_messages);
        imgs = res.obtainTypedArray(R.array.imgs_premium);


        initView();

        sid = DatabaseUtils.getSid();

        if (TextUtils.isEmpty(sid)) {
            Toast.makeText(this, "Something wrong. Try to re-login", Toast.LENGTH_LONG).show();
            finish();
        }


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        /*
      The {@link android.support.v4.view.PagerAdapter} that will provide
      fragments for each of the sections. We use a
      {@link FragmentPagerAdapter} derivative, which will keep every
      loaded fragment in memory. If this becomes too memory intensive, it
      may be best to switch to a
      {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.

        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int pos) {
                changeActiveDot(pos);

                //GA - current opened slide
                pos++;
                mSlideName = "premium_slide_" + pos;
                AnalyticsUtils.sendViewScreenEventGoogleAnalytics(PremiumActivity.this, mTracker, mSlideName);

                HashMap<String, String> parameters = new HashMap<>();
                parameters.put("slide", String.valueOf(pos));

//                if (Utils.isTabletMoreThanSevenInches(PremiumActivity.this)) {
//                    AnalyticsUtils.sendFlurryEvent(PremiumActivity.this, "premium_android_tab", parameters);
//                } else {
//                    AnalyticsUtils.sendFlurryEvent(PremiumActivity.this, "premium_android_mob", parameters);
//                }
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfAuthorized();
    }

    @Override
    protected void onStart() {
        super.onStart();

        //GA - opened slide
        HashMap<String, String> parameters = new HashMap<>();
        if (mViewPager != null) {
            int currentSlide = mViewPager.getCurrentItem();
            currentSlide++;
            String slideName = "premium_slide_" + currentSlide;
            AnalyticsUtils.sendViewScreenEventGoogleAnalytics(this, mTracker, slideName);

            parameters.put("slide", String.valueOf(currentSlide));

            AnalyticsUtils.sendEventPremium(this, mTracker, parameters);

        } else {
            String slideName = "premium_slide_1";
            AnalyticsUtils.sendViewScreenEventGoogleAnalytics(this, mTracker, slideName);

            parameters.put("slide", "1");

            AnalyticsUtils.sendEventPremium(this, mTracker, parameters);

        }
    }

    public void checkIfAuthorized() {
        List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
        if (profileTable.size() > 0) {
            ProfileTable currentProfile = profileTable.get(0);
            isAuthorized = currentProfile.isAuthorized();
            email = currentProfile.getUserEmail();
        }

        //if (isAuthorized) {
        billingInit();
        //}
    }

    public void initView() {
        mViewPager = (ViewPager) findViewById(R.id.container);
        iv_first_dot = (ImageView) findViewById(R.id.iv_first_dot);
        iv_second_dot = (ImageView) findViewById(R.id.iv_second_dot);
        iv_third_dot = (ImageView) findViewById(R.id.iv_third_dot);
        btn_buy_monthly = (Button) findViewById(R.id.btn_buy_monthly);
        btn_buy_yearly = (Button) findViewById(R.id.btn_buy_yearly);

        btn_buy_monthly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAuthorized)
                    onPremiumButtonClicked(Constants.SKU_PREMIUM_MONTHLY);
                else
                    startActivity(new Intent(PremiumActivity.this, LoginOrSignUpActivity.class));
            }
        });

        btn_buy_yearly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAuthorized)
                    onPremiumButtonClicked(Constants.SKU_PREMIUM_YEARLY);
                else
                    startActivity(new Intent(PremiumActivity.this, LoginOrSignUpActivity.class));
            }
        });
    }

    public void changeActiveDot(int pos) {
        switch (pos) {
            case 0:
                iv_first_dot.setImageResource(R.drawable.dot_active);
                iv_second_dot.setImageResource(R.drawable.dot_normal);
                iv_third_dot.setImageResource(R.drawable.dot_normal);
                break;
            case 1:
                iv_first_dot.setImageResource(R.drawable.dot_normal);
                iv_second_dot.setImageResource(R.drawable.dot_active);
                iv_third_dot.setImageResource(R.drawable.dot_normal);
                break;
            case 2:
                iv_first_dot.setImageResource(R.drawable.dot_normal);
                iv_second_dot.setImageResource(R.drawable.dot_normal);
                iv_third_dot.setImageResource(R.drawable.dot_active);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (imgs != null)
            imgs.recycle();
        // very important:
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void billingInit() {
        String part1 = getString(R.string.token_part1);
        String part2 = getString(R.string.token_part3);
        String part3 = getString(R.string.token_part2);
        base64EncodedPublicKey = part1 + (new StringBuilder(part2).reverse().toString()) + part3;

        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // включаем дебагинг (в релизной версии ОБЯЗАТЕЛЬНО выставьте в false)
        mHelper.enableDebugLogging(false);

        // инициализируем; запрос асинхронен
        // будет вызван, когда инициализация завершится
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.e("Purchase setup", "Something going wrong");
                    return;
                }
                isBillingSet = true;
                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

//                mBroadcastReceiver = new IabBroadcastReceiver(PremiumActivity.this);
//                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
//                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    List<String> additionalSkuList = new ArrayList<>();
                    additionalSkuList.add(Constants.SKU_PREMIUM_MONTHLY);
                    additionalSkuList.add(Constants.SKU_PREMIUM_YEARLY);
                    mHelper.queryInventoryAsync(true, null, additionalSkuList, mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }


            }
        });
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        String messageAfterDecrypt = "not_empty_line";
        String password = getString(R.string.check_token);
        ;
        try {
            messageAfterDecrypt = AESCrypt.decrypt(password, payload);
            //Log.e(TAG, "messageAfterDecrypt " + messageAfterDecrypt);
            //Log.e(TAG, "email " + email);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            //handle error - could be due to incorrect password or tampered encryptedMsg
        }
//            if (messageAfterDecrypt.equals(email)){
//                return  true;
//            }
        if (Utils.isEmailValid(messageAfterDecrypt)) {
            return true;
        }

        return false;
    }

    void complain(String message) {
        //Log.e(TAG, "**** Purchase Error: " + message);
    }

    public void sendPurchaseData(String token) {
        String softwareName = getString(R.string.software_name) + Utils.getAppVersion(this);
        UserPayment userPayment = new UserPayment(token);
        ApiClient.getClient(this, mTracker).userPayment(softwareName, sid, userPayment, new CancelableCallback<UserPaymentWrapper>(new Callback<UserPaymentWrapper>() {
            @Override
            public void success(UserPaymentWrapper userPaymentWrapper, Response response) {
                PurchaseTable.deleteAll(PurchaseTable.class);
                List<ProfileTable> profileTable = ProfileTable.listAll(ProfileTable.class);
                ProfileTable currentProfile = profileTable.get(0);
                currentProfile.setPlan("Premium");
                currentProfile.save();
                dismissProgressDialog();
                Helper.startMainActivity(PremiumActivity.this, false);
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                error.printStackTrace();
                dismissProgressDialog();
                String message = "Error details " + error.getMessage();
                SimpleDialog errorDialog = SimpleDialog.newInstance(getString(R.string.purchase_error), message);
                try {
                    errorDialog.show(getSupportFragmentManager(), SimpleDialog.class.getSimpleName());
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

            }
        }));
    }

    private void showProgressDialog() {
        progressDialog = ProgressDialog.show(this, null,
                "Save data...");
        progressDialog.setCancelable(false);
    }

    private void dismissProgressDialog() {
        if (!isFinishing() && progressDialog != null)
            progressDialog.dismiss();
    }

    // "Subscribe to premium" button clicked. Explain to user, then start purchase
    // flow for subscription.
    public void onPremiumButtonClicked(String sku) {
        if (!mHelper.subscriptionsSupported()) {
            complain("Subscriptions not supported on your device yet. Sorry!");
            Toast.makeText(this, R.string.subscription_not_supported, Toast.LENGTH_LONG).show();
            return;
        }
        startPurchase(sku);
    }

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int id) {
        if (id == 0 /* First choice item */) {
            mSelectedSubscriptionPeriod = mFirstChoiceSku;
        } else if (id == 1 /* Second choice item */) {
            mSelectedSubscriptionPeriod = mSecondChoiceSku;
        } else if (id == DialogInterface.BUTTON_POSITIVE /* continue button */) {
            startPurchase("");
        } else if (id != DialogInterface.BUTTON_NEGATIVE) {
            // There are only four buttons, this should not happen
            Log.e(TAG, "Unknown button clicked in subscription dialog: " + id);
        }
    }

    public void startPurchase(String sku) {
        if (email == null) {
            Toast.makeText(this, "You should be authorized", Toast.LENGTH_LONG).show();
            return;
        }
        mSelectedSubscriptionPeriod = sku;
        //Log.e(TAG, "startPurchase " + email);
        String encryptedMsg = "";
           /*   for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. */
        String password = getString(R.string.check_token);
        try {
            encryptedMsg = AESCrypt.encrypt(password, email);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        //Log.e(TAG, "startPurchase " + encryptedMsg);
        String payload = encryptedMsg;

        if (TextUtils.isEmpty(mSelectedSubscriptionPeriod)) {
            // The user has not changed from the default selection
            mSelectedSubscriptionPeriod = mFirstChoiceSku;
        }

        List<String> oldSkus = null;
        if (!TextUtils.isEmpty(mPremiumSku)
                && !mPremiumSku.equals(mSelectedSubscriptionPeriod)) {
            // The user currently has a valid subscription, any purchase action is going to
            // replace that subscription
            oldSkus = new ArrayList<>();
            oldSkus.add(mPremiumSku);
        }


        Log.d(TAG, "Launching purchase flow for premium subscription.");
        try {
            mHelper.launchPurchaseFlow(this, mSelectedSubscriptionPeriod, IabHelper.ITEM_TYPE_SUBS,
                    oldSkus, RC_REQUEST, mPurchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
        }
        // Reset the dialog options
        mSelectedSubscriptionPeriod = "";
        mFirstChoiceSku = "";
        mSecondChoiceSku = "";
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (mTracker != null) mTracker = null;
//    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String ARG_SECTION_TITLE_GREEN = "section_title_green";
        private static final String ARG_SECTION_TITLE_BLACK = "section_title_black";
        private static final String ARG_SECTION_MESSAGE = "section_message";
        private static final String ARG_SECTION_IMAGE = "section_image";
        TextView tv_title_green, tv_title_black, tv_message;
        ImageView iv_premium_images;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, String title_green, String title_black, String message, int imageId) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putString(ARG_SECTION_TITLE_GREEN, title_green);
            args.putString(ARG_SECTION_TITLE_BLACK, title_black);
            args.putString(ARG_SECTION_MESSAGE, message);
            args.putInt(ARG_SECTION_IMAGE, imageId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_premium, container, false);
            initView(rootView);
            int numberOfPage = getArguments().getInt(ARG_SECTION_NUMBER);
            String title_green = getArguments().getString(ARG_SECTION_TITLE_GREEN);
            String title_black = getArguments().getString(ARG_SECTION_TITLE_BLACK);
            String message = getArguments().getString(ARG_SECTION_MESSAGE);
            int imageId = getArguments().getInt(ARG_SECTION_IMAGE);

            tv_title_green.setText(title_green);
            tv_title_black.setText(title_black);
            tv_message.setText(message);
            iv_premium_images.setImageResource(imageId);
            return rootView;
        }


        public void initView(View rootView) {
            tv_title_green = (TextView) rootView.findViewById(R.id.tv_title_green);
            tv_title_black = (TextView) rootView.findViewById(R.id.tv_title_black);
            tv_message = (TextView) rootView.findViewById(R.id.tv_message);
            iv_premium_images = (ImageView) rootView.findViewById(R.id.iv_premium_images);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            //get resourceid by index
            int imageId = imgs.getResourceId(position, -1);
            return PlaceholderFragment.newInstance(position + 1, premium_title_green[position], premium_title_black[position], premium_messages[position], imageId);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }
}
