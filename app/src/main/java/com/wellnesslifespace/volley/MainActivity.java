package com.wellnesslifespace.volley;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.os.Build.VERSION_CODES.M;
import static com.wellnesslifespace.volley.R.id.pass;

public class MainActivity extends AppCompatActivity {

    final String TAG = this.getClass().getSimpleName();
    Button blogin;
    EditText username, password;
    SharedPreferences sp;
    String log, user, intime, outtime, id;
    ProgressDialog pdialog;
    TelephonyManager telephonyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username = (EditText) findViewById(R.id.user);
        password = (EditText) findViewById(pass);
        pdialog = new ProgressDialog(this);
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        blogin = (Button) findViewById(R.id.btn);


        checkgps();

        sp = getSharedPreferences("Login", MODE_PRIVATE);
        if (sp.contains("result")) {
            startActivity(new Intent(MainActivity.this, Main2Activity.class));
            finish();
        }

        blogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkInternetConnection();
            }
        });

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA};

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
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
            login();
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

    public void login() {

        pdialog.setTitle("loading...");
        pdialog.show();

        final String etuser = username.getText().toString();
        final String etpass = password.getText().toString();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        final String etimsi = telephonyManager.getSubscriberId().toString();

        String url = "http://arasoftwares.in/atnc-app/android_atncfile.php?action=login_details";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.e(TAG, "RESPONSE--" + response);

                        pdialog.dismiss();
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject js = jsonArray.getJSONObject(i);
                                log = js.getString("login");
                                id = js.getString("empid");
                                user = js.getString("empname");
                                intime = js.getString("intime");
                                outtime = js.getString("outtime");

                                SharedPreferences.Editor editor = sp.edit();
                                editor.putString("result", log);
                                editor.putString("idd", id);
                                editor.putString("name", user);

                                editor.commit();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "ERROR--" + e);

                        }
                        if (log.equalsIgnoreCase("success")) {


                            Toast.makeText(MainActivity.this, "login success", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, Main2Activity.class));
                            finish();

                        } else {
                            Toast.makeText(MainActivity.this, "unable to login", Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "check the username or password", Toast.LENGTH_SHORT).show();
                        }

                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.e(TAG, "ERROR--" + error);
                Toast.makeText(MainActivity.this, "" + error, Toast.LENGTH_SHORT).show();
            }

        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("username", etuser);
                params.put("password", etpass);
                params.put("sim_imsi", etimsi);
                return params;
            }
        };

        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

}

