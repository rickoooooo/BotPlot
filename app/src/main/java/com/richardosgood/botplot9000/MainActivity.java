package com.richardosgood.botplot9000;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

//import org.alternativevision.gpx.GPXParser;
//import org.alternativevision.gpx.beans.GPX;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Route;
import io.ticofab.androidgpxparser.parser.domain.RoutePoint;

import static android.provider.AlarmClock.EXTRA_MESSAGE;


public class MainActivity extends AppCompatActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private ArrayList<Waypoint> waypointList = new ArrayList<>();
    private RecyclerView recyclerView;
    private WaypointAdapter mAdapter;
    private CoordinatorLayout coordinatorLayout;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;
    private EditText fileName;
    private Button saveButton;
    //private String fileName;
    //private GPXParser gpxParser;
    //private GPX gpx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null && savedInstanceState.containsKey("waypoints")){
            waypointList = savedInstanceState.getParcelableArrayList("waypoints");
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        coordinatorLayout = findViewById(R.id.coordinator_layout);

        fileName = findViewById(R.id.gpxName);

        mAdapter = new WaypointAdapter(waypointList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration dividerItemDecoration= new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.sk_line_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(mAdapter);

        // row click listener
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        // adding item touch helper
        // only ItemTouchHelper.LEFT added to detect Right to Left swipe
        // if you want both Right -> Left and Left -> Right
        // add pass ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT as param
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this, mAdapter);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        // Load test data
        //prepareWaypointData();

        // Start location service stuff
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // If we can't write to storage, then disable the save button
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            saveButton = findViewById(R.id.button_save);
            saveButton.setEnabled(false);
        }

    }

    // Saves instance state when the display rotates, or otherwise the app is killed
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putParcelableArrayList("waypoints", waypointList);
    }

    // Load a previously saved instance state
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        waypointList = savedInstanceState.getParcelableArrayList("waypoints");
        mAdapter.notifyDataSetChanged();
    }

    // This gets called right after we choose a GPX file to load.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==123 && resultCode==RESULT_OK) {
            Uri gpxURI = data.getData(); //The uri with the location of the file

            Gpx parsedGpx = null;
            GPXParser mParser = new GPXParser();
            try {
                InputStream in = getContentResolver().openInputStream(gpxURI);
                parsedGpx = mParser.parse(in);
            } catch (Exception e) {
                Log.i("TEST", "EXCEPTION: " + e);
            }

            if (parsedGpx != null) {
                // Clear out current waypoint data
                mAdapter.clear();

                // log stuff
                Log.i("TEST", "NOT NULL");
                List<Route> routes = parsedGpx.getRoutes();
                for (int i = 0; i < routes.size(); i++) {
                    Route route = routes.get(i);
                    Log.i("TEST", "track " + i + ":");
                    fileName.setText(route.getRouteName());
                    List<RoutePoint> routePoints = route.getRoutePoints();
                    for (int j = 0; j < routePoints.size(); j++) {
                        RoutePoint routePoint = routePoints.get(j);
                        Waypoint waypoint = new Waypoint(routePoint.getLatitude(), routePoint.getLongitude(), routePoint.getType(), routePoint.getDescription());
                        Log.i("TEST", "  routePoint " + j + ":");
                        Log.i("TEST", "    point: lat " + routePoint.getLatitude() + ", lon " + routePoint.getLongitude() + ", type " + routePoint.getType() + ", handle " + routePoint.getDescription());
                        waypointList.add(waypoint);
                        //routePoint.get
                        mAdapter.notifyDataSetChanged();
                    }
                }
            } else {
                Toast.makeText(this, "Unable to parse GPX data!", Toast.LENGTH_SHORT).show();
                Log.i("TEST", "Error parsing gpx track!");
            }
        }
    }

    private void prepareWaypointData() {
        Waypoint waypoint = new Waypoint(47.6204333333333,-122.351483333333, 0);
        waypointList.add(waypoint);

        new Waypoint(47.6204333333333, -122.351483333333, 1);
        waypointList.add(waypoint);

        new Waypoint(47.6204333333333, -122.351483333333, 1);
        waypointList.add(waypoint);

        new Waypoint(47.6204333333333, -122.351483333333, 1);
        waypointList.add(waypoint);

        new Waypoint(47.6204333333333, -122.351483333333, 1);
        waypointList.add(waypoint);

        mAdapter.notifyDataSetChanged();
    }

    // ----------------------------
    // --- Swipe functions
    // ----------------------------

    /**
     * callback when recycler view is swiped
     * item will be removed on swiped
     * undo option will be provided in snackbar to restore the item
     */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof WaypointAdapter.MyViewHolder) {
            // get the removed item name to display it in snack bar
            double name = waypointList.get(viewHolder.getAdapterPosition()).getLongitude();

            // backup of removed item for undo purpose
            final Waypoint deletedItem = waypointList.get(viewHolder.getAdapterPosition());
            final int deletedIndex = viewHolder.getAdapterPosition();

            // remove the item from recycler view
            mAdapter.removeItem(viewHolder.getAdapterPosition());

            // showing snack bar with Undo option
            Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, "Waypoint removed from list!", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // undo is selected, restore the deleted item
                    mAdapter.restoreItem(deletedItem, deletedIndex);
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds cartList to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // -------------------------------
    // -- GPS STUFF
    // -------------------------------

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        } else {
            // We have permission!
            startLocationUpdates();
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLocation == null) {
                startLocationUpdates();
            }
            if (mLocation != null) {
                double latitude = mLocation.getLatitude();
                double longitude = mLocation.getLongitude();
            } else {
                Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5)
                .setFastestInterval(5);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("TEST", "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("TEST", "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }


    // Get view's position in the list of waypoint
    // Example: User clicks a button, use this to figure out which row in the list the button belongs to
    //  So we know which data set to update
    /*
    int getViewPosition(View view){
        View parentRow = (View) view.getParent().getParent();
        RecyclerView recyclerView = (RecyclerView) parentRow.getParent();
        return recyclerView.getChildLayoutPosition(parentRow);
    }
    */

    // ------------------------------------
    // ---- Button click functions
    // ------------------------------------

    public void onAddWaypoint(View view) {
        Waypoint waypoint = new Waypoint(0.0,0.0, 1);
        waypointList.add(waypoint);
        mAdapter.notifyDataSetChanged();
    }

    public void onGpsClick(View view) {
        updateGPS(view, false);
    }

    public void onAccuracyClick(View view) {
        updateGPS(view, true);
    }

    public void updateGPS(View view, boolean checkAccuracy) {
        float accuracy = 9999;
        // Check to see if app has GPS permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Ask for permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Get current GPS accuracy
        if (mLocation != null) {
            TextView textView = findViewById(R.id.gpsAccuracy);
            accuracy = mLocation.getAccuracy();
            textView.setText(Float.toString(accuracy));
        } else {
            Toast.makeText(this, "No GPX fix yet!", Toast.LENGTH_SHORT).show();

        }

        // Get GPS position
        int position = mAdapter.getViewPosition(view);
        Waypoint waypoint = waypointList.get(position);
        if (mLocation == null) {
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkAccuracy) { // Only update if accuracy is better now than it was before
            if (accuracy >= waypoint.getAccuracy()) {
                Toast.makeText(this, "Update failed: GPS accuracy worse or equal than before", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Update GPS data point
        waypoint.updateGps(mLocation);
        mAdapter.notifyDataSetChanged();
    }

    public void getGpsAccuracy(View view){
        // Check to see if app has GPS permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Ask for permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Get current GPS accuracy
        if (mLocation != null) {
            TextView textView = findViewById(R.id.gpsAccuracy);
            float accuracy = mLocation.getAccuracy();
            textView.setText(Float.toString(accuracy));
        } else {
            Toast.makeText(this, "No GPX fix yet!", Toast.LENGTH_SHORT).show();

        }

    }

    /** Load waypoint detail activity  */
    public void onMapClick(View view) {
        Intent intent = new Intent(this, MapsActivity.class);

        // Get the waypoint of the clicked list item
        int position = mAdapter.getViewPosition(view);
        Waypoint waypoint = waypointList.get(position);

        // MapsActivity excepts an ArrayList<Waypoint>, so I make one with just one item.
        ArrayList<Waypoint> points = new ArrayList<Waypoint>();
        points.add(waypoint);

        // Create intend and start activity
        intent.putParcelableArrayListExtra("waypointList", points);
        startActivity(intent);
    }

    public void onMapAll(View view) {
        Intent intent = new Intent(this, MapsActivity.class);

        // Create intend and start activity
        intent.putParcelableArrayListExtra("waypointList", waypointList);
        startActivity(intent);
    }

    public void onLoad(View view) {
        // Open file chooser
        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);
        // Intent intent = new Intent(Intent.ACTION_PICK,
        //         MediaStore.Images.Media.INTERNAL_CONTENT_URI);

        startActivityForResult(Intent.createChooser(intent, "Choose GPX file"), 123);
    }

    public void onSave(View view) {
        // Build filename from user entry
        String filename = fileName.getText() + ".gpx";
        String filepath = "MyGPXFiles";
        String myGpxData = buildGpxFile();
        File myGpxFile = null;

        myGpxFile = new File(getExternalFilesDir(filepath), filename);
        try {

            FileOutputStream fos = new FileOutputStream(myGpxFile);
            fos.write(myGpxData.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, filename + " saved to external storage.", Toast.LENGTH_SHORT).show();

    }

    public String buildGpxFile(){
        String output = "";
        output += "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
        output += "<gpx xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" version=\"1.1\" creator=\"BotPlot9000\">\n";
        output += "<metadata>\n";
        output += "\t<name>" + fileName.getText() + "</name>\n";
        output += "</metadata>";

        //Loop through routes
        if (waypointList != null) {

            output += "\t<rte>";
            output += "\t\t<name>" + fileName.getText() + "</name>";
            output += "\t\t<desc />";

            for (int j = 0; j < waypointList.size(); j++) {

                output += "\t\t<rtept " +
                        "lat=\"" + waypointList.get(j).getLatitude() + "\" " +
                        "lon=\"" + waypointList.get(j).getLongitude() + "\" " +
                        "type=\"" + waypointList.get(j).typeToString() + "\" " +
                        "handle=\"" + waypointList.get(j).getDescription() + "\"" +
                        " />";
            }
            output += "\t</rte>";
            output += "</gpx>";
        } else {
            Toast.makeText(this, "Waypoint list is null!", Toast.LENGTH_SHORT).show();
        }

        return output;
    }

    // Next two functions help us check if storage is ready for writing
    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

}
