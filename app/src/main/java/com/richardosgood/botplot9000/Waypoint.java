package com.richardosgood.botplot9000;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;

/**
 * Created by Rick on 1/17/2018.
 */

public class Waypoint implements Parcelable{
    private double longitude, latitude;
    private int type;               // Start,Waypoint,Cone
    private String description;     // Comment about what this waypoint is for

    public Waypoint() {
    }

    public Waypoint(Double latitude, Double longitude, int type) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.type = type;
        this.description = "";
    }

    public Waypoint(Double latitude, Double longitude, String type, String description) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.description = description;
        switch (type){
            case "start": this.type=0; break;
            case "waypoint": this.type=1; break;
            case "target": this.type = 2; break;
        }
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(double lon) {
        longitude = lon;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(double lat) {
        latitude = lat;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDescription(){ return description; }

    public void setDescription(String desc){
        description = desc;
    }

    public void updateGps(Location mLocation){
        double lat = mLocation.getLatitude();
        double lon = mLocation.getLongitude();

        setLatitude(lat);
        setLongitude(lon);
    }

    public String typeToString() {
        switch (this.type){
            case 0: return "start";
            case 1: return "waypoint";
            case 2: return "target";
        }
        return "unknown";
    }

        /* everything below here is for implementing Parcelable */

    // 99.9% of the time you can just ignore this
    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(latitude);
        out.writeDouble(longitude);
        out.writeInt(type);
        out.writeString(description);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Waypoint createFromParcel(Parcel in) {
            return new Waypoint(in);
        }

        public Waypoint[] newArray(int size) {
            return new Waypoint[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private Waypoint(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        type = in.readInt();
        description = in.readString();
    }
}
