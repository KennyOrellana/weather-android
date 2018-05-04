package com.clovinn.weather.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.clovinn.weather.R;
import com.clovinn.weather.data.models.Forecast;
import com.clovinn.weather.data.models.Weather;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    //UI
    private ImageView ivRefresh;
    private TextView tvLocation;
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvRain;
    private TextView tvSummary;

    //Animation
    Animation animation;

    //Location
    public static final int REQUEST_CHECK_SETTINGS = 1002;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 12;
    private LocationManager locationManager;
    private LocationListener locationListener;

    //API REST
    private OkHttpClient client = new OkHttpClient();
    private Weather weather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupView();
        setupLocation();
    }

    private void showToast(String message){
        if(message==null || message.isEmpty()) return;

        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * UI
     * **/
    private void setupView(){
        ivRefresh = findViewById(R.id.iv_refresh);
        tvLocation = findViewById(R.id.tv_location);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvHumidity = findViewById(R.id.tv_humidity);
        tvRain = findViewById(R.id.tv_rain);
        tvSummary = findViewById(R.id.tv_summary);

        ivRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAnimation();
                checkPermissions();
            }
        });

        animation = AnimationUtils.loadAnimation(this, R.anim.rotation_circular);
        startAnimation();
    }

    private void refreshView(){
        stopAnimation();

        if(weather==null || weather.getCurrently()==null) return;

        tvLocation.setText(weather.getTimezone());
        tvTemperature.setText(weather.getCurrently().getTemperature());
        tvHumidity.setText(weather.getCurrently().getHumidity());
        tvRain.setText(weather.getCurrently().getPrecipProbability());
        tvSummary.setText(weather.getCurrently().getSummary());
    }

    private void startAnimation(){
        if(ivRefresh!=null){
            ivRefresh.setClickable(false);
            animation.reset();
            ivRefresh.startAnimation(animation);
        }
    }

    private void stopAnimation(){
        if(!isDestroyed() && !isFinishing() && ivRefresh!=null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    animation.cancel();
                    ivRefresh.clearAnimation();
                    ivRefresh.setClickable(true);
                }
            });
        }
    }

    /**
     * LOCATION
     * **/
    private void setupLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                requestWeatherData(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        checkPermissions();
    }

    private void checkPermissions() {
        boolean isGpsPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean isNetworkPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!isGpsPermissionGranted && !isNetworkPermissionGranted) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                checkGPS();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
        } else {
            checkGPS();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkGPS();
                } else {
                    showToast("Necesitas aprobar los permios.");
                }
            }
        }
    }

    private void checkGPS() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    startLocation();
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        startLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        showToast("Debes Activar el GPS.");
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    private void startLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    /** API REST **/
    private void requestWeatherData(Location location){
        if(location==null) return;

        String url = "https://api.darksky.net/forecast/60b03cb458d0d7343f6c1fefa4050213/" + location.getLatitude() + "," + location.getLongitude();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                stopAnimation();
                showToast("Ocurrio un error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    String json = response.body().string();
                    weather = new Gson().fromJson(json, Weather.class);

                    if(!isDestroyed() && !isFinishing()){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshView();
                            }
                        });
                    }
                } else {
                    stopAnimation();
                    showToast("Ocurrio un error");
                }
            }
        });
    }
}
