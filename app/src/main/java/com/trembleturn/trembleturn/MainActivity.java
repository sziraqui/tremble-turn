package com.trembleturn.trembleturn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;



import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.trembleturn.trembleturn.POJO.Routes;
import com.trembleturn.trembleturn.webservice.ErrorType;
import com.trembleturn.trembleturn.webservice.OnResponseListener;
import com.trembleturn.trembleturn.webservice.ResponsePacket;
import com.trembleturn.trembleturn.webservice.ApiRouter;
import com.trembleturn.trembleturn.webservice.ApiRoutes;

import org.json.JSONObject;

public class MainActivity extends BaseActivity implements OnResponseListener {

    public static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       
    }


    public void OnClick(View v)
    {
        switch (v.getId()) {
            case R.id.main_page_button:
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
                break;
            case R.id.call_api:
                getAtoBSteps(new LatLng(19.0269, 72.8553), new LatLng(19.44769, 73.015137));
                break;
            case R.id.left_band_btn:
                Log.d(TAG, "vibrate left");
                vibrateLeft();
                break;
            case R.id.right_band_btn:
                Log.d(TAG, "vibrate right");
                vibrateRight();
                break;
            case R.id.next_activity:
                Intent i = new Intent(this, MapsSearchActivity.class);
                startActivity(i);
                break;
        }

    }

    public void getAtoBSteps(LatLng source, LatLng dest) {
        try {
            new ApiRouter(this, this, ApiRoutes.RC_A2B_STEPS, TAG)
                    .makeStringGetRequest(ApiRoutes.getA2BRequestUrl(source, dest));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void vibrateLeft() {
        try {
            new ApiRouter(this, this, ApiRoutes.RC_BAND_LEFT_HALF, TAG)
                    .makeStringGetRequest(ApiRoutes.BAND_LEFT_HALF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void vibrateRight() {
        try {
            new ApiRouter(this, this, ApiRoutes.RC_BAND_RIGHT_HALF, TAG)
                    .makeStringGetRequest(ApiRoutes.BAND_RIGHT_HALF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSuccess(int requestCode, JSONObject response) {
        switch (requestCode) {
            case ApiRoutes.RC_A2B_STEPS:
                try {
                    Routes routes = new Gson().fromJson(response.getJSONArray("routes").get(0).toString(), Routes.class);
                    Log.i(TAG, routes.legs.get(0).start_location.lat + " " + routes.legs.get(0).steps.get(0).end_location.lat);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case ApiRoutes.RC_BAND_LEFT_HALF:
                Log.i(TAG, "Vibrate left band successful");
                break;
            case ApiRoutes.RC_BAND_RIGHT_HALF:
                Log.i(TAG, "Vibrate right band successful");
                break;
        }
    }

    @Override
    public void onError(int requestCode, ErrorType errorType, JSONObject response) {

    }

    //The back press exit
    boolean doubleBackToExitPressedOnce = false;
    @Override
    public void onBackPressed() {

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            finishAffinity();
            return;
        }
    }
}
