package com.trembleturn.trembleturn;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.trembleturn.trembleturn.POJO.Routes;
import com.trembleturn.trembleturn.POJO.Steps;
import com.trembleturn.trembleturn.webservice.ApiRouter;
import com.trembleturn.trembleturn.webservice.ApiRoutes;
import com.trembleturn.trembleturn.webservice.ErrorType;
import com.trembleturn.trembleturn.webservice.OnResponseListener;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsSearchActivity extends BaseActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult>,
        OnResponseListener {

    public static final int REQUEST_LOCATION_CODE = 99;
    public static final int RC_RESOLVE_GPS_PERMISSION = 10001;
    public static final int SIMULATION_DELAY = 3000;
    public static final int VIBRATION_DELAY = 1000;

    private static final String TAG = MapsSearchActivity.class.getSimpleName();
    protected Routes routes;
    private Toolbar toolbar;
    private EditText etDest;
    private ImageView ivSearch;
    private FloatingActionButton fab;
    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Marker currentLocationMarker;

    public static List<LatLng> decode(final String encodedPath) {
        int len = encodedPath.length();

        // For speed we preallocate to an upper bound on the final length, then
        // truncate the array before returning.
        final List<LatLng> path = new ArrayList<LatLng>();
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            path.add(new LatLng(lat * 1e-5, lng * 1e-5));
        }

        return path;
    }

    public void init() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        etDest = findViewById(R.id.et_dest);
        ivSearch = findViewById(R.id.iv_search);
        fab = findViewById(R.id.fab);
    }

    public SupportMapFragment initializeMap() {
        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.maps_container, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
        return mapFragment;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).
                addApi(LocationServices.API).
                build();
        client.connect();
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                .checkLocationSettings(client, builder.build());
        result.setResultCallback(this);
    }

    // result of location settings request
    @Override
    public void onResult(@NonNull LocationSettingsResult result) {
        final Status status = result.getStatus();
        final LocationSettingsStates state = result
                .getLocationSettingsStates();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.d(TAG, "GPS permission granted successfully");
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                try {
                    Log.d(TAG, "Attempting GPS permission resolution");
                    status.startResolutionForResult(this, RC_RESOLVE_GPS_PERMISSION);
                } catch (IntentSender.SendIntentException e) {
                    // Ignore the error.
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.d(TAG, "Impossible to get GPS permission");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_RESOLVE_GPS_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "GPS permission granted through resolution");
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "GPS permission resolution failed");
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }
        Log.d(TAG, "onLocationChanged:" + location.getLatitude() + " " + location.getLongitude());
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Location");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        currentLocationMarker = mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(10));

        if (client != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_search);
        init();
        initializeMap();
        buildGoogleApiClient();
    }

    public LatLng showDestinationOnMap(String location) {
        List<Address> addressList = null;
        MarkerOptions mo = new MarkerOptions();
        Log.d(TAG, "Current location:" +location);
        LatLng latlng = new LatLng(19.022231, 72.856226);
        if (!location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 5);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Address myAddress = addressList.get(0);
            latlng = new LatLng(myAddress.getLatitude(), myAddress.getLongitude());
            mo.position(latlng);
            mo.title("Your Search Result !");
            mo.flat(true);
            mMap.addMarker(mo);
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
        }
        return latlng;
    }

    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.iv_search:
                String dest = etDest.getText().toString();
                LatLng source;
                LatLng destLatLng = showDestinationOnMap(dest);
                try {
                    source = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                } catch (Exception e) {
                    source = new LatLng(19.022231, 72.856226);
                }

                getAtoBSteps(source, destLatLng);
                // show start FAB
                break;
            case R.id.fab:
                // Start vibrations
                int sizeOfRoute = routes.legs.get(0).steps.size();

                int i;
                List<LatLng> latlnglist = new ArrayList<LatLng>();
                List<String> manevuer = new ArrayList<String>();
                List<String> instr = new ArrayList<String>();

                for (i = 0; i < sizeOfRoute; i++) {
                    double lat1 = routes.legs.get(0).steps.get(i).end_location.lat;
                    double lng1 = routes.legs.get(0).steps.get(i).end_location.lng;
                    latlnglist.add(new LatLng(lat1, lng1));

                    String m2 = routes.legs.get(0).steps.get(i).htmlinstructions;
                    instr.add(m2);

                    String m1 = routes.legs.get(0).steps.get(i).maneuver;
                    if (m1 == null)
                        manevuer.add("Keep going");
                    else
                        manevuer.add(m1);

                }

                Log.i(TAG, latlnglist.toString());

                setAnimation(mMap, latlnglist, manevuer, instr);

        }
    }

    private void indicateLeft() {
        vibrateLeft();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopVibrateLeft();
            }
        }, VIBRATION_DELAY);

    }

    private void indicateRampLeft() {
        vibrateLeftHalf();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopVibrateLeft();
            }
        }, VIBRATION_DELAY);

    }

    private void indicateRight() {
        vibrateRight();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopVibrateRight();
            }
        }, VIBRATION_DELAY);
    }

    private void indicateRampRight() {
        vibrateRight();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopVibrateRight();
            }
        }, VIBRATION_DELAY);
    }

    private void vibrateLeft() {
        try {
            new ApiRouter(this, this, ApiRoutes.RC_BAND_LEFT_FULL, TAG)
                    .makeStringGetRequest(ApiRoutes.BAND_LEFT_FULL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void vibrateLeftHalf() {
        try {
            new ApiRouter(this, this, ApiRoutes.RC_BAND_LEFT_HALF, TAG)
                    .makeStringGetRequest(ApiRoutes.BAND_LEFT_HALF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopVibrateLeft() {
        try {
            new ApiRouter(this, this, ApiRoutes.RC_BAND_LEFT_STOP, TAG)
                    .makeStringGetRequest(ApiRoutes.BAND_LEFT_STOP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void vibrateRight() {
        try {
            new ApiRouter(this, this, ApiRoutes.RC_BAND_RIGHT_FULL, TAG)
                    .makeStringGetRequest(ApiRoutes.BAND_RIGHT_FULL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void vibrateRightHalf() {
        try {
            new ApiRouter(this, this, ApiRoutes.RC_BAND_RIGHT_HALF, TAG)
                    .makeStringGetRequest(ApiRoutes.BAND_RIGHT_HALF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopVibrateRight() {
        try {
            new ApiRouter(this, this, ApiRoutes.RC_BAND_RIGHT_STOP, TAG)
                    .makeStringGetRequest(ApiRoutes.BAND_RIGHT_STOP);
        } catch (Exception e) {
            e.printStackTrace();
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

    @Override
    public void onSuccess(int requestCode, JSONObject response) {
        switch (requestCode) {
            case ApiRoutes.RC_A2B_STEPS:
                try {
                    routes = new Gson().fromJson(response.getJSONArray("routes").get(0).toString(), Routes.class);
                    Log.i(TAG, routes.legs.get(0).start_location.lat + " " + routes.legs.get(0).steps.get(0).end_location.lat);
                    String[] pathdisplay = getPaths(routes);
                    directiondisplay(pathdisplay);
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

    public String[] getPaths(Routes route) {

        List<Steps> step = route.legs.get(0).steps;
        String routes[] = new String[step.size()];
        for (int i = 0; i < step.size(); i++) {

            routes[i] = getPath(step.get(i));
        }

        return routes;
    }

    public String getPath(Steps step) {

        String polyline = step.polyline.points;
        return polyline;
    }

    public void directiondisplay(String[] path) {

        for (int i = 0; i < path.length; i++) {

            PolylineOptions options = new PolylineOptions();
            options.color(Color.RED);
            options.width(10);
            options.addAll(decode(path[i]));

            mMap.addPolyline(options);
        }
    }

    @Override
    public void onError(int requestCode, ErrorType errorType, JSONObject response) {

    }

    public void setAnimation(GoogleMap myMap, final List<LatLng> directionPoint, final List<String> man, final List<String> instr) {
        Marker marker = myMap.addMarker(new MarkerOptions()
                .position(directionPoint.get(0))
                .flat(true));

        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(directionPoint.get(0), 10));

        animateMarker(myMap, marker, directionPoint, man, instr, false);
    }

    private void animateMarker(final GoogleMap myMap, final Marker marker, final List<LatLng> directionPoint, final List<String> man, final List<String> instr, final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = myMap.getProjection();
        final long duration = Integer.MAX_VALUE;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            int i = 0;


            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                if (i < directionPoint.size()) {

                    marker.setPosition(directionPoint.get(i));
                    myMap.animateCamera(CameraUpdateFactory.newLatLng(directionPoint.get(i)));

                    if (man.get(i).contains("right")) {
                        if(man.get(i).contains("keep") || man.get(i).contains("ramp")) {
                            indicateRampRight();
                        } else {
                            indicateRight();
                        }
                    } else if (man.get(i).contains("left")) {
                        if(man.get(i).contains("keep") || man.get(i).contains("ramp")) {
                            indicateRampLeft();
                        } else {
                            indicateLeft();
                        }
                    } else {
                        Log.d(TAG, "neither right nor left");
                    }
                    Log.d("maneuver", man.get(i));

                }
                //myMap.animateCamera(CameraUpdateFactory.zoomTo(2.0f));

                i++;

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, SIMULATION_DELAY);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

}
