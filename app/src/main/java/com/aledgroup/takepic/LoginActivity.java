package com.aledgroup.takepic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aledgroup.takepic.Common.CfUserInfo;
import com.aledgroup.takepic.Common.EndPoint;
import com.aledgroup.takepic.Utils.getMD5;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aledgroup.takepic.app.AppController;
import com.aledgroup.takepic.helper.SQLiteHandler;
import com.aledgroup.takepic.helper.SessionManager;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Button btnLogin;
    private EditText inputUserName;
    private EditText inputPassword;
    private ProgressDialog pDialog;
    private SessionManager session;
    SQLiteHandler db = new SQLiteHandler(this);

    public static String serverName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // for hiding title
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        inputUserName = (EditText) findViewById(R.id.username);
        inputPassword = (EditText) findViewById(R.id.password);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        List<CfUserInfo> userInfo = db.getAllSettings();
        for (CfUserInfo cn : userInfo) {
            serverName = cn.getSettings();
        }

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        // Login button Click Event
        btnLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {

                String userName = inputUserName.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                // Check for empty data in the form
                if (!userName.isEmpty() && !password.isEmpty()) {
                    // login user
                    if(userName.equals("ADMIN") && password.equals("4dm1n123%"))
                    {
                        // Launch main activity
                        Intent intent = new Intent(LoginActivity.this,
                                Settings.class);
                        startActivity(intent);
                    }
                    else {
                        if(serverName.equals(""))
                        {
                            Toast.makeText(getApplicationContext(),"Please set server name!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        checkLogin(userName, getMD5.md5(password));
                    }
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),"Please enter the credentials!", Toast.LENGTH_LONG).show();
                }
            }

        });
    }

    /**
     * function to verify login details in mysql db
     * */
    private void checkLogin(final String userName, final String password) {
        // Tag used to cancel the request
        String tag_string_req = "req_login";

        pDialog.setMessage("Logging in ...");
        showDialog();

        StringRequest strReq = new StringRequest(Method.POST,
                serverName + EndPoint.URL_LOGIN, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Login Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // user successfully logged in
                        // Create login session
                        session.setLogin(true);

                        // Now store the user in SQLite
                        String rowId = jObj.getString("RowId");

                        JSONObject userInfo = jObj.getJSONObject("UserInfo");
                        String loginCode = userInfo.getString("LoginCode");
                        String userName = userInfo.getString("UserName");
                        String userAlias = userInfo.getString("UserAlias");
                        String templateUser = userInfo.getString("TemplateUser");

                        db.deleteUsers();
                        // Inserting row in users table
                        db.addUserInfo(rowId,loginCode, userName, userAlias, templateUser);

                        // Launch main activity
                        Intent intent = new Intent(LoginActivity.this,
                                MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("LoginCode", userName);
                params.put("Password", password);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }
    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}

