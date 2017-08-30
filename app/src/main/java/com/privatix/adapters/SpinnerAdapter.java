package com.privatix.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.privatix.R;
import com.privatix.model.SubscriptionTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Lotar on 26.11.2015.
 */
public class SpinnerAdapter extends ArrayAdapter {
    private Context mContext;
    private List<SubscriptionTable> dataSubscriptionList;
    private Boolean isFreeAccount = true;
    private int currentSelectedItemPosition = 0;
    private TypedArray imgs_flags;
    private Map<String, Integer> countryMap;

    public SpinnerAdapter(Context context, int textViewResourceId, String[] countryName,
                          List<SubscriptionTable> sortedCountriesList, Boolean isFreeAccount) {
        super(context, textViewResourceId, countryName);
        mContext = context;
        this.dataSubscriptionList = sortedCountriesList;
        this.isFreeAccount = isFreeAccount;
        initFlagsArray();
    }


    public void accountTypeChanged(Boolean isFreeAccount) {
        this.isFreeAccount = isFreeAccount;
    }

    private void initFlagsArray() {
        countryMap = new HashMap<>();
        Resources res = getContext().getResources();
        String[] countryCodeName = res.getStringArray(R.array.country_code_name);
        imgs_flags = res.obtainTypedArray(R.array.imgs_flags);
        for (int i = 0; i < countryCodeName.length; i++) {
            countryMap.put(countryCodeName[i], imgs_flags.getResourceId(i, -1));
        }
    }


    @Override
    public Object getItem(int position) {
        return super.getItem(position);
    }

    private View getCustomView(int position, View convertView,
                               ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.spinner_item, parent, false);
        TextView tvCountry = (TextView) layout
                .findViewById(R.id.tvCountry);
        //if(!dataSubscriptionList.get(position).getCountry().equals("Hong Kong")) {
        tvCountry.setText(dataSubscriptionList.get(position).getCountry());
        //}else{
        //    tvCountry.setText("Hong Kong SAR China");
        //}


        String countryCode = dataSubscriptionList.get(position).getCountryCode();
        Integer resId = -1;

        //We can't name resources do cause it is Java keyword
        if (countryCode.equals("do")) {
            resId = R.drawable.do_;
        } else {
            resId = countryMap.get(countryCode);
        }
        ImageView img = (ImageView) layout.findViewById(R.id.imgCountry);
        if (null != resId && resId != -1) {
            img.setImageResource(resId);
        }

        return layout;
    }

    private View getMyDropDownView(int position, View convertView,
                                   ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.spinner_item_dropdown, parent, false);

        TextView tvCountry = (TextView) layout
                .findViewById(R.id.tvCountry);

//        TextView tv_premium = (TextView) layout
//                .findViewById(R.id.tv_premium);

        ImageView imgStar = (ImageView) layout
                .findViewById(R.id.imgStar);

        ImageView imgChecked = (ImageView) layout
                .findViewById(R.id.imgChecked);

        if (position == currentSelectedItemPosition)
            imgChecked.setAlpha(1.0f);
        else
            imgChecked.setAlpha(0f);

        //if(!dataSubscriptionList.get(position).getCountry().equals("Hong Kong")) {
        tvCountry.setText(dataSubscriptionList.get(position).getCountry());
        //}else{
        //    tvCountry.setText("Hong Kong SAR China");
        //}
        //Log.e("countryCode", mCountryCode.get(position));

        String countryCode = dataSubscriptionList.get(position).getCountryCode();
        Integer resId = -1;

        //We can't name resources do cause it is Java keyword
        if (countryCode.equals("do")) {
            resId = R.drawable.do_;
        } else {
            resId = countryMap.get(countryCode);
        }
        ImageView img = (ImageView) layout.findViewById(R.id.imgCountry);
        if (null != resId && resId != -1) {
            img.setImageResource(resId);
        }
        if (isFreeAccount && !dataSubscriptionList.get(position).getIsFree()) {
//            tv_premium.setVisibility(View.VISIBLE);
            imgStar.setVisibility(View.VISIBLE);
            layout.setAlpha(0.5f);
        }

        return layout;
    }

    // It gets a View that displays in the drop down popup the data at the specified position
    @Override
    public View getDropDownView(int position, View convertView,
                                @NonNull ViewGroup parent) {
        return getMyDropDownView(position, convertView, parent);
    }

    // It gets a View that displays the data at the specified position
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public void setCurrentSelectedItemPosition(int position) {
        currentSelectedItemPosition = position;
    }


    public void recycleFlagsArray() {
        imgs_flags.recycle();
    }
}
