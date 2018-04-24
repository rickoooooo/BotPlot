package com.richardosgood.botplot9000;

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double latitude;
    private double longitude;
    private ArrayList<Waypoint> waypointList = new ArrayList<>();
    private WaypointAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent i = getIntent();
        waypointList = i.getParcelableArrayListExtra("waypointList");
        mAdapter = new WaypointAdapter(waypointList);
        Log.i("MAPS", "mAdapter: " + mAdapter.getWaypoint(0).getLatitude());

        //latitude = Double.valueOf(getIntent().getExtras().getString("latitude"));
        //longitude = Double.valueOf(getIntent().getExtras().getString("longitude"));
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng marker = new LatLng(0,0);
        ArrayList<LatLng> points = new ArrayList<LatLng>();
        // Instantiating the class PolylineOptions to plot polyline in the map
        PolylineOptions polylineOptions = new PolylineOptions();
        float color = 0;

        // Load all passed markers
        for (int x = 0; x < mAdapter.getItemCount(); x++) {
            Waypoint waypoint = mAdapter.getWaypoint(x);

            if (waypoint.getType() == 0) {
                color = BitmapDescriptorFactory.HUE_GREEN;
            } else if (waypoint.getType() == 1) {
                color = BitmapDescriptorFactory.HUE_RED;
            } else if (waypoint.getType() == 2) {
                color = BitmapDescriptorFactory.HUE_ORANGE;
            }

            marker = new LatLng(waypoint.getLatitude(), waypoint.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(marker)
                    .title(waypoint.getDescription())
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));

            // Setting the color of the polyline
            polylineOptions.color(Color.RED);

            // Setting the width of the polyline
            polylineOptions.width(3);

            // Adding the taped point to the ArrayList
            points.add(marker);

        }

        // Setting points of polyline
        polylineOptions.addAll(points);

        // Adding the polyline to the map
        googleMap.addPolyline(polylineOptions);
        // Move the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 15));
        mMap.setMapType(mMap.MAP_TYPE_HYBRID);
    }
}
