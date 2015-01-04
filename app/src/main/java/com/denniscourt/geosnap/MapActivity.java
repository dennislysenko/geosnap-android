package com.denniscourt.geosnap;

import android.app.AlertDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MapActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private Map<String, ImageLocationPair> snaps;
    private int snapId = 0;

    private static final int MAX_WIDTH = 2048;
    private static final int MAX_HEIGHT = 2048;

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    getImageLocationPairsFromGallery();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            public void getImageLocationPairsFromGallery() throws IOException {
                Cursor newImageCursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Images.ImageColumns.DATA,
                                MediaStore.Images.ImageColumns.DATE_TAKEN,
                                BaseColumns._ID },
                                null,
                                null,
                                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

                snaps = new HashMap<>();
                if (newImageCursor.moveToFirst()) {
                    do {
                        String path = newImageCursor.getString(0);
                        ExifInterface exif = new ExifInterface(path);

                        float[] latLong = new float[2];
                        exif.getLatLong(latLong);

                        LatLng location = new LatLng(latLong[0], latLong[1]);

                        snaps.put("" + (snapId++), new ImageLocationPair(location, new File(path)));
                    } while (newImageCursor.moveToNext());
                }
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if (snaps != null && snaps.size() > 0) {
                    for (String snapId : snaps.keySet()) {
                        ImageLocationPair firstPair = snaps.get(snapId);
                        mMap.addMarker(
                                new MarkerOptions()
                                        .position(firstPair.getLocation())
                                        .snippet(snapId)
                        );
                    }
                }
            }
        }.execute();

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                ImageLocationPair pair = snaps.get(marker.getSnippet());

                ImageView image = new ImageView(getApplicationContext());

                // Calculate bounds first to see if the image is too big to fit in memory
                BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
                boundsOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(pair.getImageFile().toString(), boundsOptions);

                // Then set inSampleSize high enough that the read image will fit in memory
                int inSamplesBasedOnWidth = Math.max(1, (int) Math.ceil((double) boundsOptions.outWidth / MAX_WIDTH));
                int inSamplesBasedOnHeight = Math.max(1, (int)Math.ceil((double) boundsOptions.outHeight / MAX_HEIGHT));

                int inSamples = Math.max(inSamplesBasedOnWidth, inSamplesBasedOnHeight);

                BitmapFactory.Options realOptions = new BitmapFactory.Options();
                realOptions.inSampleSize = inSamples;

                // And read it in
                Bitmap imageBitmap = BitmapFactory.decodeFile(pair.getImageFile().toString(), realOptions);
                image.setImageBitmap(imageBitmap);


                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                builder.setTitle("Image")
                        .setView(image)
                        .create().show();
                return false;
            }
        });
    }
}
