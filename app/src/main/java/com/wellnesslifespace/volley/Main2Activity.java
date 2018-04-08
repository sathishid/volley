package com.wellnesslifespace.volley;

import android.Manifest;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;

import java.io.ByteArrayOutputStream;

import java.io.File;

import java.io.IOException;
import java.io.InputStreamReader;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.List;
import java.util.Locale;

public class Main2Activity extends AppCompatActivity implements LocationListener {

    static final int REQUEST_TAKE_PHOTO = 100;
    final String TAG = this.getClass().getSimpleName();
    Button CheckIn, CheckOut;

    LocationManager locationManager;
    TextView date, userName;
    String eid, euser, loc = "", la = "", lo = "";
    ProgressDialog dialog;
    String mCurrentPhotoPath;
    Uri photoURI;

    BufferedReader reader;
    String defaultCameraPackage;
    String imageFileName;
    Bitmap bm;
    ByteArrayBody bab;

    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        CheckIn = (Button) findViewById(R.id.getL);
        CheckOut = (Button) findViewById(R.id.up);
        dialog = new ProgressDialog(this);
        date = (TextView) findViewById(R.id.date);
        userName = (TextView) findViewById(R.id.userN);
        checkInternetConnection();
        checkgps();

        List<ApplicationInfo> list = getPackageManager().getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (int n = 0; n < list.size(); n++) {
            if ((list.get(n).flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                Log.e("TAG", "Installed Applications  : " + list.get(n).loadLabel(this.getPackageManager()).toString());
                Log.e("TAG", "package name  : " + list.get(n).packageName);
                if (list.get(n).loadLabel(this.getPackageManager()).toString().equalsIgnoreCase("Camera")) {
                    defaultCameraPackage = list.get(n).packageName;
                    break;
                }
            }
        }
        getLocation();
        SharedPreferences sp = getSharedPreferences("Login", MODE_PRIVATE);
        String dateForText = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        euser = sp.getString("name", "");
        eid = sp.getString("idd", "");

        userName.setText("User : " + euser);
        date.setText("Date:" + dateForText);


        CheckIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                url = "http://arasoftwares.in/atnc-app/android_atncfile.php?action=attendance";
                //Toast.makeText(Main2Activity.this, "Opening Camera", Toast.LENGTH_SHORT).show();
                dispatchTakePictureIntent();


            }
        });

        CheckOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                url = "http://arasoftwares.in/atnc-app/android_atncfile.php?action=attendance_out";
                //Toast.makeText(Main2Activity.this, "Opening Camera", Toast.LENGTH_SHORT).show();
                dispatchTakePictureIntent();

            }
        });


    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?" +
                "system Need GPS To Open This Application")
                .setCancelable(false)
                .setPositiveButton("yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void checkInternetConnection() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().isAvailable() && conMgr.getActiveNetworkInfo().isConnected()) {

        } else {
            Toast.makeText(getApplicationContext(), "Internet connection is not present", Toast.LENGTH_SHORT).show();
        }
    }

    public void checkgps() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

        } else {
            showGPSDisabledAlertToUser();
        }
    }


    public void dispatchTakePictureIntent() {


        Intent takePictureIntent = new Intent((android.provider.MediaStore.ACTION_IMAGE_CAPTURE));
        takePictureIntent.setPackage(defaultCameraPackage);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "IOException" + ex);
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, "com.wellnesslifespace.volley.fileprovider", photoFile);
                Log.e(TAG, "photo Uri:--" + photoURI);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {


            new ImageUploadTask().execute();


        } else if (resultCode == RESULT_CANCELED) {

            Toast.makeText(getApplicationContext(), "USER CANCEL IMAGE CAPTURE", Toast.LENGTH_SHORT).show();


        } else {

            Toast.makeText(getApplicationContext(), "SORRY ! FAILED TO CAPTURE IMAGE", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void setPic() {


    }

    public File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/");
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.e(TAG, "save a path is :--" + mCurrentPhotoPath);
        return image;
    }

    private void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 1, this);
        } catch (SecurityException e) {
            Log.e(TAG, "ERROR--" + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        la = Double.toString(location.getLatitude());
        Log.e(TAG, "LATTITUDE--" + la);
        lo = Double.toString(location.getLongitude());
        Log.e(TAG, "LONGITUDE--" + lo);
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            loc = addresses.get(0).getAddressLine(0);
            Log.e(TAG, "LOCATION--" + loc);
        } catch (Exception e) {
            Log.e(TAG, "ERROR--" + e);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(this, "PLEASE TURN ON GPS", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                SharedPreferences sp = getSharedPreferences("Login", MODE_PRIVATE);
                SharedPreferences.Editor e = sp.edit();
                e.clear();
                e.commit();
                startActivity(new Intent(Main2Activity.this, MainActivity.class));
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    class ImageUploadTask extends AsyncTask<Void, Void, String> {


        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(Main2Activity.this);
            dialog.setTitle("Uploading Image...");
            dialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... unsued) {
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(url);
                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                entity.addPart("name", new StringBody(euser));
                entity.addPart("userimage", new FileBody(new File(mCurrentPhotoPath),"image/jpeg"));
                entity.addPart("id", new StringBody(eid));
                entity.addPart("location", new StringBody(loc));
                entity.addPart("lattitude", new StringBody(la));
                entity.addPart("langitude", new StringBody(lo));
                httpPost.setEntity(entity);
                HttpResponse response = httpClient.execute(httpPost);
                reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                String sResponse = reader.readLine();
                if (sResponse != null) {
                    dialog.dismiss();
                    Log.e(TAG, "response" + sResponse);
                }
                return sResponse;
            } catch (Exception e) {
                if (dialog.isShowing())
                    dialog.dismiss();
                Log.e(TAG, "-------" + e.getMessage(), e);
                return null;
            }


        }

        @Override
        protected void onProgressUpdate(Void... unsued) {

        }

        @Override
        protected void onPostExecute(String sResponse) {
            if (dialog.isShowing())
                dialog.dismiss();

            if (sResponse != null) {
                dialog.dismiss();
                Log.e("fa", "----------" + sResponse);
                Toast.makeText(Main2Activity.this, "Success ", Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
                Log.e("fa", "----------" + sResponse);
            }
        }


    }


}
