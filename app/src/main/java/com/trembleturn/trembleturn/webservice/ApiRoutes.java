package com.trembleturn.trembleturn.webservice;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

public abstract class ApiRoutes {

    public static final String API_URL = "https://maps.googleapis.com/maps/api/directions/json?";
    public static final String API_KEY = "AIzaSyD61OtmkLZi8l_F3sVVe0R3DH5QfaZ3kbc";
    public static final String LEFT_BAND_URL = "http://192.168.43.53";
    public static final String RIGHT_BAND_URL = "http://192.168.43.43";
    public static final String FULL_PWM = "/r";
    public static final String HALF_PWM = "/h";
    public static final String STOP = "/s";


    public static final int RC_A2B_STEPS = 1;

    public static String getA2BRequestUrl(LatLng source, LatLng dest) {

       return ApiRoutes.API_URL +
                "origin=" + String.valueOf(source.latitude) + "," + String.valueOf(source.longitude) + "&" +
                "destination=" + String.valueOf(dest.latitude) + "," + String.valueOf(dest.longitude) + "&" +
                "key=" + ApiRoutes.API_KEY;
    }

    public static final int RC_BAND_LEFT_FULL = 2;
    public static String BAND_LEFT_FULL = LEFT_BAND_URL + FULL_PWM;

    public static final int RC_BAND_LEFT_HALF = 3;
    public static String BAND_LEFT_HALF = LEFT_BAND_URL + HALF_PWM;

    public static final int RC_BAND_LEFT_STOP = 4;
    public static String BAND_LEFT_STOP = LEFT_BAND_URL + STOP;

    public static final int RC_BAND_RIGHT_FULL = 5;
    public static String BAND_RIGHT_FULL = RIGHT_BAND_URL + FULL_PWM;

    public static final int RC_BAND_RIGHT_HALF = 6;
    public static String BAND_RIGHT_HALF = RIGHT_BAND_URL + HALF_PWM;

    public static final int RC_BAND_RIGHT_STOP = 7;
    public static String BAND_RIGHT_STOP = RIGHT_BAND_URL + STOP;

    abstract void makeStringGetRequest(String url);

    abstract void makeJsonPostRequest(String url, JSONObject jsonRequest);

    abstract void makeJsonPutRequest(String url, JSONObject jsonRequest);
}
