package com.example.locationapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import static com.example.locationapi.BuildConfig.APPLICATION_ID;

public class MainActivity extends AppCompatActivity {

    private static final int CHECK_SETTINGS_CODE = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 400;

    private Button startLocationUpdateButton, stopLocationUpdateButton;
    private TextView locationTextView;
    private TextView locationUpdateTimeTextView;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean isLocationUpdateActive;
    private String locationUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.location_text_view);
        locationUpdateTimeTextView = findViewById(R.id.location_update_time);
        startLocationUpdateButton = findViewById(R.id.start_location_button);
        stopLocationUpdateButton = findViewById(R.id.stop_location_button);

        fusedLocationProviderClient = LocationServices
                .getFusedLocationProviderClient(this);

        settingsClient = LocationServices.getSettingsClient(this);

        startLocationUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocationUpdate();
            }
        });

        stopLocationUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationUpdate();
            }
        });

        buildLocationRequest();
        buildLocationCallBack();
        buildLocationResponse();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdate();
    }

    private void startLocationUpdate() {
        isLocationUpdateActive = true;
        startLocationUpdateButton.setEnabled(false);
        stopLocationUpdateButton.setEnabled(true);

        settingsClient
                .checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this,
                        new OnSuccessListener<LocationSettingsResponse>() {
                            @Override
                            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                                fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                                        locationCallback,
                                        Looper.myLooper());
                                updateLocationUI();
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();

                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException resolvableApiException =
                                            ((ResolvableApiException) e);
                                    resolvableApiException
                                            .startResolutionForResult(
                                                    MainActivity.this,
                                                    CHECK_SETTINGS_CODE);
                                } catch (IntentSender.SendIntentException ex) {
                                    ex.printStackTrace();
                                }
                                break;
                            case LocationSettingsStatusCodes
                                    .SETTINGS_CHANGE_UNAVAILABLE:
                                String messageExc = "Setup location setting on device";
                                Toast.makeText(MainActivity.this,
                                        messageExc,
                                        Toast.LENGTH_SHORT).show();
                                isLocationUpdateActive = false;
                                startLocationUpdateButton.setEnabled(true);
                                stopLocationUpdateButton.setEnabled(false);
                        }
                        updateLocationUI();
                    }
                });
    }

    private void stopLocationUpdate() {

        if(!isLocationUpdateActive){
            return;
        }
        fusedLocationProviderClient.removeLocationUpdates(locationCallback).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                isLocationUpdateActive = false;
                startLocationUpdateButton.setEnabled(true);
                stopLocationUpdateButton.setEnabled(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHECK_SETTINGS_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Log.d("MainActivity",
                            "User has agreed to change location settings");
                    startLocationUpdate();
                    break;
                case Activity.RESULT_CANCELED:
                    Log.d("MainActivity",
                            "User has don't agreed to change location settings");
                    isLocationUpdateActive = false;
                    startLocationUpdateButton.setEnabled(true);
                    stopLocationUpdateButton.setEnabled(false);
                    updateLocationUI();
                    break;
            }
        }
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();

                updateLocationUI();
            }
        };
    }

    private void updateLocationUI() {
        if (currentLocation != null) {

            locationTextView.setText(String.format(Locale.ENGLISH,
                    "%.2f\u2103 %.2f\u2103",
                    currentLocation.getLatitude(), currentLocation.getLongitude()));

            locationUpdateTimeTextView.setText(DateFormat.getTimeInstance()
                    .format(new Date()));
        }
    }

    private void buildLocationResponse() {
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addAllLocationRequests(Collections.singleton(locationRequest));
        locationSettingsRequest = builder.build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdateActive && checkLocationPermission()) {
            startLocationUpdate();
        } else if (!checkLocationPermission()) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        boolean shouldProvideRationale = ActivityCompat
                .shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        String mainText = "Location permission is needed for app functionality";
        String action = "OK";
        if (shouldProvideRationale) {
            showSnackBar(mainText, action, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_LOCATION_PERMISSION);
                }
            });
        } else {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private void showSnackBar(final String mainText,
                              final String action,
                              View.OnClickListener listener) {

        Snackbar.make(findViewById(android.R.id.content), mainText,
                Snackbar.LENGTH_INDEFINITE).setAction(action, listener).show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length <= 0) {
                Log.d("onRequestPermRes", "Request was cancelled");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdateActive) {
                    startLocationUpdate();
                }
            } else {
                String mainText = "Turn on location on settings";
                String action = "Settings";
                showSnackBar(mainText, action, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts(
                                "package",
                                APPLICATION_ID,
                                null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
            }
        }

    }

    private boolean checkLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}
