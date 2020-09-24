package com.example.map1application;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.icu.util.Calendar;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.internal.IMapFragmentDelegate;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.internal.ui.AutocompleteImplFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    double currentLatitude, currentLongitude;
    SupportMapFragment mapFragment;
    boolean trackingFlag = false;
    ListView lv;
    PlacesClient placesClient;

    public static final int MAX = 10;
    String[] placename, placeAddress;
    LatLng[] placeLatlng;

    String myKey = "AIzaSyDxc2-MPR9koFmTfEUVplIt9tOXMo-khHI";
    AutocompleteSupportFragment autocompleteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        autocompleteFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        lv = findViewById(R.id.lv);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (trackingFlag) {
                    new LocationTask().execute(locationResult.getLastLocation());
                }
            }
        };
        checkLocationPermission();

        if (!Places.isInitialized()) {
            Places.initialize(this, myKey);
        }
        placesClient = Places.createClient(this);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {

            @Override
            public void onPlaceSelected(@NonNull final Place place) {
                if (place != null) {
                    /*mMap.clear();
                    LatLng latLng = place.getLatLng();
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(place.getName());
                    mMap.addMarker(markerOptions);*/

                    String placeId = place.getId();
                    List<Place.Field> placeField = Arrays.asList(Place.Field.PHOTO_METADATAS);
                    final FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeField).build();
                    placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                        @Override
                        public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                            Place thisPlace = fetchPlaceResponse.getPlace();
                            List<PhotoMetadata> photoMetadataList = thisPlace.getPhotoMetadatas();
                            if (photoMetadataList == null || photoMetadataList.isEmpty()) {
                                Toast.makeText(MapsActivity.this, "Place photo not available", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            PhotoMetadata thisPhotometadata = photoMetadataList.get(0);

                            final FetchPhotoRequest fetchPhotoRequest = FetchPhotoRequest.builder(thisPhotometadata).build();

                            placesClient.fetchPhoto(fetchPhotoRequest).addOnSuccessListener(new OnSuccessListener<FetchPhotoResponse>() {
                                @Override
                                public void onSuccess(FetchPhotoResponse fetchPhotoResponse) {
                                    Bitmap bp = fetchPhotoResponse.getBitmap();
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MapsActivity.this, status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void startTrackingLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_LONG).show();
            trackingFlag = true;

            fusedLocationProviderClient.requestLocationUpdates(getLocationRequest(), locationCallback, null);
        }
    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        return locationRequest;
    }

    private void stopTrackingLocation() {
        if (trackingFlag)
            trackingFlag = false;
    }

    public void checkLocationPermission() {
        if (!trackingFlag)
            startTrackingLocation();
        else
            stopTrackingLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTrackingLocation();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showPlaces(View view) {
        List<Place.Field> placeField = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeField).build();
        if (trackingFlag) {
            @SuppressLint("MissingPermission")
            Task<FindCurrentPlaceResponse> responseTask = placesClient.findCurrentPlace(request);

            responseTask.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
                @Override
                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                    if (task.isSuccessful()) {
                        FindCurrentPlaceResponse placeResponse = task.getResult();
                        int count;
                        if (placeResponse.getPlaceLikelihoods().size() < MAX) {
                            count = placeResponse.getPlaceLikelihoods().size();
                        } else {
                            count = MAX;
                        }
                        placename = new String[count];
                        placeAddress = new String[count];
                        placeLatlng = new LatLng[count];

                        int i = 0;
                        for (PlaceLikelihood placeLikelihood : placeResponse.getPlaceLikelihoods()) {
                            placename[i] = placeLikelihood.getPlace().getName();
                            placeAddress[i] = placeLikelihood.getPlace().getAddress();
                            placeLatlng[i] = placeLikelihood.getPlace().getLatLng();
                            i++;

                            if (i == count) {
                                break;
                            }
                        }
                        fillPlaceInformation();
                    } else {
                        Toast.makeText(MapsActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void fillPlaceInformation() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, placename);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                mMap.clear();
                LatLng latLng = placeLatlng[index];
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                MarkerOptions marker = new MarkerOptions();
                marker.position(latLng);
                marker.title("Ypu selected place as" + placename[index] + placeAddress[index]);
                mMap.addMarker(marker);
            }
        });
        lv.setAdapter(adapter);
    }

    class LocationTask extends AsyncTask<Location, Void, String> {

        @Override
        protected String doInBackground(Location... locations) {
            String msg = "";
            Location currentLocation = locations[0];
            if (currentLocation != null) {
                currentLatitude = currentLocation.getLatitude();
                currentLongitude = currentLocation.getLongitude();
                msg = "Current Location";
            }
            return msg;
        }



        @Override
        protected void onPostExecute(String s) {
            if (s.equals("Current Location")) {
                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        mMap = googleMap;
                        mMap.clear();
                        LatLng mylatlng = new LatLng(currentLatitude, currentLongitude);
                        String pos1= Double.toString(currentLatitude);
                        String pos2= Double.toString(currentLongitude);
                        String pos3= "Lat Pos: "+pos1+"\n"+"Lon Pos: "+pos2;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mylatlng, 20.0f));
                        mMap.setMyLocationEnabled(true);

                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(mylatlng);
                        markerOptions.title("You are Here");
                        //markerOptions.snippet(pos3);
                        mMap.addMarker(markerOptions);
                        //
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm:ss z");
                        String date = dateFormat.format(new Date());
                        //dateTimeDisplay.setText(date);
                        Toast.makeText(getApplicationContext(),pos3+"\n"+date,Toast.LENGTH_LONG).show();
                    }

                });
            }
        }
    }
}
