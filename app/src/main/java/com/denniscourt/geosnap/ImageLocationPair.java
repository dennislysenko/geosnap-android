package com.denniscourt.geosnap;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;

/**
 * Created by dennis on 1/4/15.
 */
public class ImageLocationPair {
    private LatLng location;
    private File imageFile;

    public ImageLocationPair(LatLng location, File imageFile) {
        this.location = location;
        this.imageFile = imageFile;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }
}
