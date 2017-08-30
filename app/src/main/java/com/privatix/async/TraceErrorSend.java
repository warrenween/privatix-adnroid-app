package com.privatix.async;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.privatix.R;
import com.privatix.api.ApiClient;
import com.privatix.api.helper.CancelableCallback;
import com.privatix.api.models.answer.subscription.TraceErrorWrapper;
import com.privatix.api.models.request.TraceError;
import com.privatix.model.ProfileTable;
import com.privatix.model.TraceErrorTable;
import com.privatix.utils.Utils;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Lotar on 07.10.2016.
 */

public class TraceErrorSend {
    public static final String TAG = TraceErrorSend.class.getSimpleName();
    private Context mContext;
    private String type;
    private String error;
    private String sourceCountry;
    private String connectionNode;
    private String errorTrace;

    public TraceErrorSend(Context context, String type, String error, String sourceCountry, String connectionNode) {
        this.mContext = context;
        this.type = type;
        this.error = error;
        this.sourceCountry = sourceCountry;
        this.connectionNode = connectionNode;
    }

    public void sentError() {
       new GetNetworkConfig().execute();
    }



    private void sentToServer(){
        final long datetime = System.currentTimeMillis();
        List<ProfileTable> profileTables = ProfileTable.listAll(ProfileTable.class);
        if (profileTables.size() == 0)
            return;
        ProfileTable currentProfile = profileTables.get(0);
        final String sid = currentProfile.getSid();
        final String subscriptionUuid = currentProfile.getSubscriptionId();
        final String softwareName = mContext.getString(R.string.software_name) + Utils.getAppVersion(mContext);
        TraceError traceError = new TraceError(type, datetime, subscriptionUuid, error, errorTrace, sourceCountry, connectionNode);
        ApiClient.getClient(mContext, null).traceError(softwareName, sid, traceError, new CancelableCallback<TraceErrorWrapper>(new Callback<TraceErrorWrapper>() {
            @Override
            public void success(TraceErrorWrapper traceErrorWrapper, Response response) {
                Log.d("success", "true");
            }

            @Override
            public void failure(RetrofitError error) {
                error.printStackTrace();
                TraceErrorTable traceErrorTable = new TraceErrorTable(softwareName, sid, type, datetime, subscriptionUuid,
                        TraceErrorSend.this.error, errorTrace, sourceCountry, connectionNode);
                traceErrorTable.save();
            }
        }));
    }




    private class GetNetworkConfig extends AsyncTask<Void, Void, String>{
        String netCfg, route;

        @Override
        protected String doInBackground(Void... params) {
            List<String> netCfgList = Shell.SH.run("netcfg");
            if(netCfgList!=null) {
                for (String s : netCfgList) {
                    Log.e(TAG, "netCfgList: " + s);
                    netCfg+= s;
                }
            }else{
                Log.e(TAG, "netCfgList null");
            }

            List<String> routeList = Shell.SH.run("ip route show");
            if(routeList!=null) {
                for (String s : routeList) {
                    Log.e(TAG, "routeList: " + s);
                    route+= s;
                }
            }else{
                Log.e(TAG, "routeList null");
            }

            return "ifconfig " + netCfg+
                    " \n "+ "route "+ route;
        }

        @Override
        protected void onPostExecute(String errorResult) {
            super.onPostExecute(errorResult);
            errorTrace = errorResult;
            sentToServer();
        }
    }


}
