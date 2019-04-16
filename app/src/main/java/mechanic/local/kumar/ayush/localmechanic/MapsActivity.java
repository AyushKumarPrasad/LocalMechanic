package mechanic.local.kumar.ayush.localmechanic;



import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import mechanic.local.kumar.ayush.localmechanic.Model.UserInformation;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback ,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener ,
        LocationListener , GoogleMap.OnMarkerClickListener ,  GoogleMap.OnInfoWindowClickListener
{
    private GoogleMap mMap;

    private static final int MY_PERMISSION_REQUEST_CODE = 7192 ;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 300193 ;

    private LocationRequest mLocationRequest ;
    private GoogleApiClient mGoogleApiClient ;
    private Location mLastLocation ;

    private static final int UPDATE_INTERVAL = 5000;
    private static final int FATEST_INTERVAL = 5000;
    private static final int DISPLACEMENT = 10 ;

    DatabaseReference ref ;
    GeoFire geoFire;
    Marker mCurrent ;

    VerticalSeekBar mSeekbar ;

    private DatabaseReference mUsers;
    UserInformation user ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mSeekbar = (VerticalSeekBar) findViewById(R.id.vericalSeekBar);
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mMap.animateCamera(CameraUpdateFactory.zoomTo(i) , 2000 , null );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ref = FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire = new GeoFire(ref);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mUsers= FirebaseDatabase.getInstance().getReference("Users");

        setUpLocation();
    }

    private void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode , this , PLAY_SERVICES_RESOLUTION_REQUEST).show();
            else
            {
                Toast.makeText(this,"Device Not Supported" , Toast.LENGTH_SHORT).show();
                finish();
            }

            return false ;
        }

        return true;
    }

    private void setUpLocation()
    {
        if (ActivityCompat.checkSelfPermission(this , Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this , new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            } , MY_PERMISSION_REQUEST_CODE);
        }
        else
        {
            if (checkPlayServices())
            {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSION_REQUEST_CODE :
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (checkPlayServices())
                    {
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }

                break;
            }
        }
    }

    private void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void displayLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED   )
        {
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
        {

            final double latitude = mLastLocation.getLatitude();
            final double longitude = mLastLocation.getLongitude();

            geoFire.setLocation("You", new GeoLocation(latitude, longitude),
                    new GeoFire.CompletionListener()
                    {
                        @Override
                        public void onComplete(String key, DatabaseError error)
                        {
                            if (mCurrent != null)

                                mCurrent.remove();
                            mCurrent = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitude , longitude))
                                    .title("Your Position"));
                            mCurrent.showInfoWindow();

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude , longitude) , 12.0f));

                            final LatLng dangerous_area = new LatLng(latitude, longitude);

                            mMap.addCircle(new CircleOptions()
                                    .center(dangerous_area)
                                    .radius(1000)  // in metres  => 500m
                                    .strokeColor(Color.BLUE)
                                    .fillColor(0x220000FF)
                                    .strokeWidth(5.0f));

                            //   0.5f = 0.5km = 500m

                            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(dangerous_area.latitude, dangerous_area.longitude) ,
                                    1.0f);
                            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                                @Override
                                public void onKeyEntered(String key, GeoLocation location) {
                                    sendNotification("Uniflow" , String.format("%s entered the dangerous area " , key));
                                }

                                @Override
                                public void onKeyExited(String key) {
                                    sendNotification("Uniflow" , String.format("%s you came out of danger area " , key));
                                }

                                @Override
                                public void onKeyMoved(String key, GeoLocation location) {
                                    Log.d("MOVING" , String.format("%s moving with in dangerous area [%f/%f]" , key , location.latitude ,
                                            location.latitude ));
                                }

                                @Override
                                public void onGeoQueryReady() {

                                }

                                @Override
                                public void onGeoQueryError(DatabaseError error)
                                {
                                    Log.e("ERROR" , "" + error) ;
                                }
                            });
                        }
                    });

            mMap.setOnMarkerClickListener(MapsActivity.this);
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    for (DataSnapshot s : dataSnapshot.getChildren())
                    {
                        user = s.getValue(UserInformation.class);
                        LatLng location = new LatLng(user.latitude,user.longitude);

                        mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(user.name + user.latitude)
                                .snippet(user.rating + user.longitude)
                                //  .rotation((float) 33.5)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

                    }


                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {

                }
            });


        }
        else
        {
            Log.d("Hero" , "Couldn't get location");
        }


    }
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        displayLocation();
    }

    public void showNotification(Context context, String title, String body, Intent intent) {

    }

    private void startLocationUpdates()
    {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED   )
        {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient , mLocationRequest , (LocationListener) this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location)
    {
        mLastLocation = location ;
        displayLocation();
    }

    @Override
    public boolean onMarkerClick(Marker marker)
    {
        return true;
    }

    @Override
    public void onInfoWindowClick(Marker marker)
    {

    }

    private void sendNotification(String title, String content)
    {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content);

        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this , MapsActivity.class);
        PendingIntent contnetIntent = PendingIntent.getActivity(this , 0 , intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(contnetIntent);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;

        manager.notify(new Random().nextInt() , notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {

            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

            int notificationId = 1;
            String channelId = "channel-01";
            String channelName = "Channel Name";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(
                        channelId, channelName, importance);
                notificationManager.createNotificationChannel(mChannel);
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(content);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntent(intent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            mBuilder.setContentIntent(resultPendingIntent);

            notificationManager.notify(notificationId, mBuilder.build());
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}

/*

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap mMap) {

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mMap.clear();

        CameraPosition googlePlex = CameraPosition.builder()
                .target(new LatLng(25.1413,55.1853))
                .zoom(2)
                .bearing(0)
                .tilt(45)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(googlePlex), 10000, null);

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(27.1750, 78.0422))
                .title("Taj Mahal" )
                .snippet("It is located in India" + " " + "hello ayush kumar")
                .rotation((float) 3.5)
                .icon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.taj)));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(48.8584, 2.2945))
                .title("Eiffel Tower")
                .snippet("It is located in France" + " " + "hello ayush kumar")
                .rotation((float) 33.5)
                .icon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.effil)));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(25.1413, 55.1853))
                .title("burj al arab")
                .snippet("It is located in Dubai" + " " + "hello ayush kumar")
                .rotation((float) 93.5)
                .icon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.dubai)));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(38.9637, 35.2433))
                .title("Turkey")
                .snippet("It is located in Turkey" + " " + "hello ayush kumar")
                .rotation((float) 33.5)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));


    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
  */




