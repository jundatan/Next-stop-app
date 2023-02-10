package com.example.mycurrentlocation;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private Button button_location;
    private TextView textView_show;
    private TextView textView_location;
    private LocationManager locationManager;
    private double target_lat;
    private double target_longt;
    private double dist;
    private boolean miss_stop_flag;
    private NotificationCompat.Builder builder;
    private int notification_request_code = 2;
    private int location_request_code = 100;


    //For testing purpose. Target location.
    private EditText editLat;
    private EditText editLongt;

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button_location = findViewById(R.id.button_location);
        textView_show = findViewById(R.id.text_show);
        textView_location = findViewById(R.id.text_location);

        createNotificationChannel();

        button_location.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                //create method
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){
                    requestLocationPermission();
                }

                if(ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestNotificationPermission();
                }

                textView_show.setText("");
                textView_location.setText("Starting");
                miss_stop_flag = false;
                editLat = findViewById(R.id.edtLatitude);
                editLongt = findViewById(R.id.edtLongtitude);
                getLocation();
            }
        });
    }


    private double distance(double lat1, double lon1, double lat2, double lon2, char unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == 'K') {
            dist = dist * 1.609344;
        } else if (unit == 'N') {
            dist = dist * 0.8684;
        }
        return (dist);
    }

    private void requestLocationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Important Permission Required")
                    .setMessage("Permission required for GPS location.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, location_request_code);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, location_request_code);
        }
    }

    private void requestNotificationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            new AlertDialog.Builder(this)
                    .setTitle("Important Permission Required")
                    .setMessage("Permission required for push notification when to alight.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, notification_request_code);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, notification_request_code);
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Notification Alert", "Notification Alert", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notification Alert");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        try {
                locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, MainActivity.this);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            //Printing of the current location (optional)
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            String address = addresses.get(0).getAddressLine(0);
            textView_location.setText(address);

            //Finding the distance of the user current location against the targeted point.
            target_lat = Double.parseDouble(editLat.getText().toString());
            target_longt =  Double.parseDouble(editLongt.getText().toString());
            dist = distance(target_lat, target_longt, location.getLatitude(),
                    location.getLongitude(), 'K') * 1000;

            //Distance under 150 from the stop, please alight.
            if(dist < 150 && !miss_stop_flag) {
                Toast.makeText(this, "You have reached!", Toast.LENGTH_SHORT).show();
                textView_show.setTextColor(Color.parseColor("#358600"));
                textView_show.setText("Please alight at the next stop!");

                //Builder for push notification.
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

                builder = new NotificationCompat.Builder(this, "Notification Alert")
                        .setSmallIcon(R.drawable.transport)
                        .setContentTitle("NextStopWhen?")
                        .setContentText("Please alight at the next stop!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(10, builder.build());

                miss_stop_flag = true;
            } else if(!miss_stop_flag) { // Bus has not arrived at destination.
                Toast.makeText(this, "You are roughly " + (int) dist + "m away!", Toast.LENGTH_SHORT).show();
                textView_show.setTextColor(Color.RED);
                textView_show.setText("Not Reached!");
            } else { // Bus has missed the destination.
                textView_show.setTextColor(Color.RED);
                textView_show.setText("You have missed your stop!");
                locationManager.removeUpdates(this);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
