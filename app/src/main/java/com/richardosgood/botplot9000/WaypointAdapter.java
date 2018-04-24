package com.richardosgood.botplot9000;

/**
 * Created by Rick on 1/17/2018.
 */

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WaypointAdapter extends RecyclerView.Adapter<WaypointAdapter.MyViewHolder> implements AdapterView.OnItemSelectedListener{

    private ArrayList<Waypoint> waypointList;


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView latitude, longitude, accuracy;
        public Button gpsButton;
        public Spinner typeSpinner;
        public Button mapButton;
        public EditText description;
        public RelativeLayout viewBackground, viewForeground;
        public MyCustomTextChangedListener myCustomTextChangedListener;

        public MyViewHolder(View view,  MyCustomTextChangedListener textChangedListener) {
            super(view);
            latitude = (TextView) view.findViewById(R.id.latitude);
            longitude = (TextView) view.findViewById(R.id.longitude);
            accuracy = (TextView) view.findViewById(R.id.waypointAccuracy);
            gpsButton = (Button) view.findViewById(R.id.button_gps);
            mapButton = (Button) view.findViewById(R.id.button_map);
            typeSpinner = (Spinner) view.findViewById(R.id.waypoint_type);
            description = (EditText) view.findViewById(R.id.description);
            myCustomTextChangedListener = textChangedListener;
            description.addTextChangedListener(myCustomTextChangedListener);

            viewBackground = view.findViewById(R.id.view_background);
            viewForeground = view.findViewById(R.id.view_foreground);
        }
    }


    public WaypointAdapter(ArrayList<Waypoint> waypointList) {
        this.waypointList = waypointList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.waypoint_list_row, parent, false);

        MyViewHolder vh = new MyViewHolder(itemView, new MyCustomTextChangedListener());
        return vh;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        Waypoint waypoint = waypointList.get(position);
        holder.latitude.setText(Double.toString(waypoint.getLatitude()));
        holder.longitude.setText(Double.toString(waypoint.getLongitude()));
        holder.accuracy.setText(Float.toString(waypoint.getAccuracy()));
        holder.typeSpinner.setSelection(waypoint.getType(), false);
        holder.typeSpinner.setOnItemSelectedListener(WaypointAdapter.this);
        holder.myCustomTextChangedListener.updatePosition(holder.getAdapterPosition());
        holder.description.setText(waypoint.getDescription());
    }


    @Override
    public int getItemCount() {
        return waypointList.size();
    }

    public Waypoint getWaypoint(int pos ) {
        return waypointList.get(pos);
    }

    //@Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(waypointList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(waypointList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    //@Override
    void onItemDismiss(int position) {}

    public void removeItem(int position) {
        waypointList.remove(position);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position);
    }

    public void restoreItem(Waypoint waypoint, int position) {
        waypointList.add(position, waypoint);
        // notify item added by position
        notifyItemInserted(position);
    }

    // ---------------------------------
    // ---- Spinner functions
    // ---------------------------------
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        int position = getViewPosition(parent);
        Waypoint waypoint = waypointList.get(position);
        waypoint.setType(pos);
    }

    public void onNothingSelected(AdapterView<?> parent) {}

    public void clear(){
        this.waypointList.clear();
        this.notifyDataSetChanged();
    }

    // Get view's position in the list of waypoint
    // Example: User clicks a button, use this to figure out which row in the list the button belongs to
    //  So we know which data set to update
    int getViewPosition(View view){
        View parentRow = (View) view.getParent().getParent();
        RecyclerView recyclerView = (RecyclerView) parentRow.getParent();
        return recyclerView.getChildLayoutPosition(parentRow);
    }

    // This is used to watch for changes to the item descriptions. It will save the descriptions
    // from the EditText views into the waypoint objects after one is modified. Without this,
    // That information is lost as you scroll through the view or change to maps, etc.
    private class MyCustomTextChangedListener implements TextWatcher{
        private int position;

        public void updatePosition(int position) {
            this.position = position;
        }

        // the user's changes are saved here
        public void onTextChanged(CharSequence c, int start, int before, int count) {
            Waypoint wp = waypointList.get(position);
            wp.setDescription(c.toString());
        }

        public void beforeTextChanged(CharSequence c, int start, int count, int after) {
            // this space intentionally left blank
        }

        public void afterTextChanged(Editable c) {
            // this one too
        }
    }
}