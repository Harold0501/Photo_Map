package com.simulation1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.Manifest;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private MapView mapView;
    private AMap aMap;
    private LatLng photoLocation;
    private TextView locationTextView;
    private final Map<Marker, String> markerTextMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final int REQUEST_LOCATION_PERMISSION = 1;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Set the corresponding XML layout file
        locationTextView = findViewById(R.id.location_text);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();

        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW); // Single positioning and moving to the center of the map
        myLocationStyle.interval(2000); // Locate every 2 seconds
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setMyLocationEnabled(true);

        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);
        AMapLocationClient locationClient;
        // Set up location monitoring
        try {
            locationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            Log.e("LocationError", "Positioning startup failed", e);
            throw new RuntimeException(e);
        }
        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy); // High precision mode
        locationOption.setOnceLocation(false);
        locationOption.setInterval(2000);
        locationClient.setLocationOption(locationOption);
        // Set location callback listening
        locationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation == null) {
                    Log.e("LocationError", "aMapLocation is null");
                } else if (aMapLocation.getErrorCode() != 0) {
                    Log.e("LocationError", "Positioning failed. Error Code：" + aMapLocation.getErrorCode() +
                            "，Error Message：" + aMapLocation.getErrorInfo() +
                            "Location Error Details：" + aMapLocation.getLocationDetail());
                }
                if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
                    double latitude = aMapLocation.getLatitude();  // Get latitude
                    double longitude = aMapLocation.getLongitude(); // Get longitude
                    photoLocation = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                    Log.d("LocationInfo", "Device Current location: Latitude = " + latitude + " Longitude = " + longitude);
                    locationTextView.setText(String.format(Locale.US, "(%.6f, %.6f)", latitude, longitude));
                }
            }
        });
        // Start location
        locationClient.startLocation();

        aMap.setOnMarkerClickListener(marker -> {
            String currentText = markerTextMap.get(marker);
            if (currentText == null || currentText.isEmpty()) {
                showTextInputDialog(marker);
            } else {
                marker.setTitle(currentText);
                marker.showInfoWindow();
            }
            return true;
        });

        aMap.setOnInfoWindowClickListener(this::showTextInputDialog);

        Button takePictureButton = findViewById(R.id.take_picture_button);
        takePictureButton.setOnClickListener(v -> openCamera());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK && data != null) {
            Bitmap photoBitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");

            if (photoBitmap != null) {
                ImageView photoImageView = findViewById(R.id.photo_image_view);
                photoImageView.setImageBitmap(photoBitmap);

                savePhotoToGallery(photoBitmap);

                if (photoLocation != null) {
                    addPhotoMarker(photoLocation, photoBitmap);
                } else {
                    Toast.makeText(this, "Unable to obtain location information, unable to tag photo", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void savePhotoToGallery(Bitmap bitmap) {
        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                "MyPhoto_" + System.currentTimeMillis(),
                "Photo taken in simulation app"
        );

        if (savedImageURL != null) {
            Toast.makeText(this, "Photo saved to the album", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Photo save failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void addPhotoMarker(LatLng position, Bitmap bitmap) {
        Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, 100, 100, true);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(thumbnail))
                .title("");

        Marker marker = aMap.addMarker(markerOptions);
        markerTextMap.put(marker, "");
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }
    }

    private void showTextInputDialog(Marker marker) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enter marker text");

        final EditText input = new EditText(this);
        input.setHint("Please enter text...");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String text = input.getText().toString();
            if (!text.isEmpty()) {
                saveTextToMarker(marker, text);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveTextToMarker(Marker marker, String text) {
        if (marker != null) {
            markerTextMap.put(marker, text);
            marker.setTitle(text);
            marker.showInfoWindow();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}