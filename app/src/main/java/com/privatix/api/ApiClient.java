package com.privatix.api;


import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.analytics.Tracker;
import com.jakewharton.retrofit.Ok3Client;
import com.privatix.BuildConfig;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.helper.CustomErrorHandler;
import com.privatix.api.helper.NoActivityCustomErrorHandler;
import com.privatix.api.models.answer.GetSubscriptionWrapper;
import com.privatix.api.models.answer.MetricWrapper;
import com.privatix.api.models.answer.UserActivationWrapper;
import com.privatix.api.models.answer.UserAuthorizationWrapper;
import com.privatix.api.models.answer.UserCheckMailWrapper;
import com.privatix.api.models.answer.UserPaymentWrapper;
import com.privatix.api.models.answer.UserRecoverWrapper;
import com.privatix.api.models.answer.UserRegistrationWrapper;
import com.privatix.api.models.answer.UserSessionWrapper;
import com.privatix.api.models.answer.subscription.TraceErrorWrapper;
import com.privatix.api.models.request.Metric;
import com.privatix.api.models.request.TraceError;
import com.privatix.api.models.request.UserActivation;
import com.privatix.api.models.request.UserAuthorization;
import com.privatix.api.models.request.UserCheckMail;
import com.privatix.api.models.request.UserOAuth;
import com.privatix.api.models.request.UserPayment;
import com.privatix.api.models.request.UserRecover;
import com.privatix.api.models.request.UserRegistration;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit.RestAdapter;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;

public class ApiClient {
    public static final String ENDPOINT = BuildConfig.SERVER_URL;
    private static final int X_API_VERSION = 3;
    private static final String API_VERSION_HEADER = "X-API-Version:" + X_API_VERSION;
    private static OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();
    private static RestApiClient apiClient;

    public static RestApiClient getClient(AppCompatActivity appCompatActivity) {
//        if(!Utils.isNetworkAvailable(activity)) {
//            Utils.showToast(R.string.check_your_network);
//        }
//        fragmentManager = null;
//        okHttpClient.interceptors().add(new Interceptor() {
//            @Override
//            public Response intercept(Chain chain) throws IOException {
//                return onOnIntercept(chain);
//            }
//        });
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(ENDPOINT)
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .setErrorHandler(new CustomErrorHandler(appCompatActivity))
                .setClient(new Ok3Client(okHttpClient))
                .build();
        //apiClient = restAdapter.create(RestApiClient.class);
        return restAdapter.create(RestApiClient.class);
    }


    public static RestApiClient getClient(Context context, Tracker tracker) {
//        okHttpClient.interceptors().add(new Interceptor() {
//            @Override
//            public Response intercept(Chain chain) throws IOException {
//                return onOnIntercept(chain);
//            }
//        });
        if (apiClient == null) {
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(ENDPOINT)
                    .setLogLevel(RestAdapter.LogLevel.NONE)
                    .setErrorHandler(new NoActivityCustomErrorHandler(context, tracker))
                    .setClient(new Ok3Client(okHttpClient))
                    .build();
            apiClient = restAdapter.create(RestApiClient.class);
        }
        return apiClient;
    }


    public interface RestApiClient {

        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER,
                })
        @POST("/user/activation")
        void userActivation(@Header("X-Software") String softwareName, @Body UserActivation userActivation, CancelableCallback<UserActivationWrapper> callback);


        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @GET("/subscription")
        void getSubscription(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, CancelableCallback<GetSubscriptionWrapper> callback);


        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @GET("/user/session")
        void getUserSession(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, CancelableCallback<UserSessionWrapper> callback);


        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @POST("/user/recover")
        void userRecover(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, @Body UserRecover body, CancelableCallback<UserRecoverWrapper> callback);


        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @POST("/user/authorization")
        void userAuthorization(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, @Body UserAuthorization body, CancelableCallback<UserAuthorizationWrapper> callback);


        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @POST("/user/registration")
        void userRegistration(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, @Body UserRegistration body, CancelableCallback<UserRegistrationWrapper> callback);


        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @POST("/user/oauth")
        void userAuthorizationSocial(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, @Body UserOAuth body, CancelableCallback<UserRegistrationWrapper> callback);


        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @POST("/user/check_mail")
        void userCheckMail(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, @Body UserCheckMail body, CancelableCallback<UserCheckMailWrapper> callback);


        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @POST("/metrics")
        void sendMetrics(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, @Body Metric body, CancelableCallback<MetricWrapper> callback);

        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @POST("/user/payment")
        void userPayment(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, @Body UserPayment body, CancelableCallback<UserPaymentWrapper> callback);


        @Headers
                ({
                        "Content-Type:application/json",
                        API_VERSION_HEADER
                })
        @POST("/error")
        void traceError(@Header("X-Software") String softwareName, @Header("X-Session-ID") String sid, @Body TraceError body, CancelableCallback<TraceErrorWrapper> callback);


    }


    public interface OnConnectionTimeoutListener {
        void onConnectionTimeout();
    }


//    private Response onOnIntercept(Interceptor.Chain chain) throws IOException {
//        try {
//            Response response = chain.proceed(chain.request());
//            String content = UtilityMethods.convertResponseToString(response);
//            Log.d(TAG, lastCalledMethodName + " - " + content);
//            return response.newBuilder().body(ResponseBody.create(response.body().contentType(), content)).build();
//        }
//        catch (SocketTimeoutException exception) {
//            exception.printStackTrace();
//            if(listener != null)
//                listener.onConnectionTimeout();
//        }
//
//        return chain.proceed(chain.request());
//    }

}