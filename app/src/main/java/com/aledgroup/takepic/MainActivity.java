package com.aledgroup.takepic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aledgroup.takepic.Common.CfUserInfo;
import com.aledgroup.takepic.Common.EndPoint;
import com.aledgroup.takepic.Utils.AndroidMultiPartEntity;
import com.aledgroup.takepic.Utils.GlobalConfig;
import com.aledgroup.takepic.Utils.MultipartUtility;
import com.aledgroup.takepic.app.AppController;
import com.aledgroup.takepic.helper.SQLiteHandler;
import com.aledgroup.takepic.helper.SessionManager;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by aled on 05/03/2016.
 */
public class MainActivity extends Activity {

    private Button btnUpload;
    private Button btnRefresh;
    private String filePath = null;
    private ProgressDialog pDialog;
    private String rowIds;
    private EditText edtTemplateUser;
    private String templateUsers;

    private SessionManager session;
    public static String serverName = "";

    private static int SELECT_FILE = 1;

    static Bitmap mRawImage;
    long totalSize = 0;

    SQLiteHandler db = new SQLiteHandler(this);

    //region: Fields and Consts

    public static Uri fileUri; // file url to store image

    DrawerLayout mDrawerLayout;

    private ActionBarDrawerToggle mDrawerToggle;
    private MainFragment mCurrentFragment;
    private Uri mCropImageUri;
    private CropImageViewOptions mCropImageViewOptions = new CropImageViewOptions();
    //endregion

    public void setCurrentFragment(MainFragment fragment) {
        mCurrentFragment = fragment;
    }

    public void setCurrentOptions(CropImageViewOptions options) {
        mCropImageViewOptions = options;
        updateDrawerTogglesByOptions(options);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // Session manager
        session = new SessionManager(getApplicationContext());

        btnUpload = (Button) findViewById(R.id.btnUpload);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.main_drawer_open, R.string.main_drawer_close);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            setMainFragmentByPreset(CropDemoPreset.RECT);
        }

        List<CfUserInfo> userInfo1 = db.getAllSettings();
        for (CfUserInfo cn : userInfo1) {
            serverName = cn.getSettings();
        }

        edtTemplateUser = (EditText) findViewById(R.id.txtTemplate);
        List<CfUserInfo> userInfo = db.getAllUserInfo();
        String templateUser = "";
        for (CfUserInfo cn : userInfo) {
            rowIds =  cn.getRowId();
            templateUser = cn.getTemplateUser();
        }

        edtTemplateUser.setText(templateUser);
        templateUsers = templateUser;

        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                reloadData(rowIds);
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                filePath = getRealPathFromURI(fileUri);
                if(filePath.equals("/drawable/ic_camera"))
                {
                    Toast.makeText(getApplicationContext(),
                            "Please select image",
                            Toast.LENGTH_LONG).show();
                    return ;
                }
                sendFile(filePath);
            }
        });

        // Checking camera availability
        if (!isDeviceSupportCamera()) {
            Toast.makeText(getApplicationContext(),
                    "Sorry! Your device doesn't support camera",
                    Toast.LENGTH_LONG).show();
            // will close the app if the device does't have camera
            finish();
        }

    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
        mCurrentFragment.updateCurrentCropViewOptions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (mCurrentFragment != null && mCurrentFragment.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Checking device has camera hardware or not
     * */
    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * Receiving activity result method will be called after closing the camera
     * */
    @Override
    /*protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == GlobalConfig.CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                previewMedia();
            } else if (resultCode == RESULT_CANCELED) {

                // user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();

            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }

        }
    }*/

    @SuppressLint("NewApi")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            fileUri = imageUri;

            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            boolean requirePermissions = false;
            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)) {

                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true;
                mCropImageUri = imageUri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            } else {

                mCurrentFragment.setImageUri(imageUri);
            }
        }

       /*if (requestCode == SELECT_FILE) {
           if (resultCode == RESULT_OK) {
               Uri selectedImageUri = data.getData();
           *//* String[] projection = {MediaStore.MediaColumns.DATA};
            CursorLoader cursorLoader = new CursorLoader(this, selectedImageUri, projection, null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            cursor.moveToFirst();
            String selectedImagePath = cursor.getString(column_index);*//*
               fileUri = selectedImageUri;
               mCurrentFragment.setImageUri(fileUri);
           } else if (resultCode == RESULT_CANCELED) {

               // user cancelled Image capture
               Toast.makeText(getApplicationContext(),
                       "User cancelled load image", Toast.LENGTH_SHORT)
                       .show();

           } else {
               // failed to capture image
               Toast.makeText(getApplicationContext(),
                       "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                       .show();
           }

        }*/

        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                mCurrentFragment.setImageUri(fileUri);
            } else if (resultCode == RESULT_CANCELED) {

                // user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();

            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mCurrentFragment.setImageUri(mCropImageUri);
        } else {
            Toast.makeText(this, "Cancelling, required permissions are not granted", Toast.LENGTH_LONG).show();
        }
    }

    public void onDrawerOptionClicked(View view) {
        switch (view.getId()) {
            case R.id.drawer_option_capture:
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fileUri = getOutputMediaFileUri(GlobalConfig.MEDIA_TYPE_IMAGE);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                // start the image capture Intent
                startActivityForResult(intent, GlobalConfig.CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_load:
                CropImage.startPickImageActivity(this);
               /* Intent intImage = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intImage.setType("image*//*");
                startActivityForResult(
                        Intent.createChooser(intImage, "Select File"),
                        SELECT_FILE);*/
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_oval:
                setMainFragmentByPreset(CropDemoPreset.CIRCULAR);
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_rect:
                setMainFragmentByPreset(CropDemoPreset.RECT);
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_logout:
                session.setLogin(false);
                // Launch main activity
                Intent intents = new Intent(MainActivity.this,
                        LoginActivity.class);
                startActivity(intents);
                finish();
                break;
            default:
                Toast.makeText(this, "Unknown drawer option clicked", Toast.LENGTH_LONG).show();
        }
    }
    private void setMainFragmentByPreset(CropDemoPreset demoPreset) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(demoPreset))
                .commit();
    }

    private void updateDrawerTogglesByOptions(CropImageViewOptions options) {

        String aspectRatio = "FREE";
        if (options.fixAspectRatio) {
            aspectRatio = options.aspectRatio.first + ":" + options.aspectRatio.second;
        }
    }

    /**
     * ------------ Helper Methods ----------------------
     * */

    /**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                EndPoint.IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(GlobalConfig.TAG, "Oops! Failed create "
                        + EndPoint.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == GlobalConfig.MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void reloadData(final String rowId) {
        // Tag used to cancel the request
        String tag_string_req = "req_refresh";

        pDialog.setMessage("Refreshing ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                serverName +  EndPoint.URL_RELOAD, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // Now store the user in SQLite
                        String rowId = jObj.getString("RowId");

                        JSONObject userInfo = jObj.getJSONObject("UserInfo");
                        String loginCode = userInfo.getString("LoginCode");
                        String userName = userInfo.getString("UserName");
                        String userAlias = userInfo.getString("UserAlias");
                        String templateUser = userInfo.getString("TemplateUser");
                        edtTemplateUser.setText(templateUser);
                        templateUsers = templateUser;
                        // Inserting row in users table
                        CfUserInfo userInfo1 = new CfUserInfo();
                        userInfo1.setRowId(rowId);
                        userInfo1.setTemplateUser(templateUser);
                        db.updatUserInfo(userInfo1);

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
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("RowId", rowId);

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

    private String imageToBase64(String filePath) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(filePath);//You can get an inputStream using any IO API
        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void sendFile(final String filePath) {

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                if (pDialog != null)
                    pDialog.dismiss();
                pDialog.setMessage("Uploading ...");
                showDialog();
            }

            @Override
            protected String doInBackground(Void... params) {
                try {
                    File file = new File(filePath);

                    MultipartUtility mpUtil = new MultipartUtility(serverName + EndPoint.URL_UPLOAD, "UTF-8");

                    mpUtil.addFormField("RowId", rowIds);
                    mpUtil.addFormField("TemplateUser", templateUsers);
                    mpUtil.addFilePart("image", file);

                    return mpUtil.connect();
                } catch (Exception e) {
                    hideDialog();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (pDialog != null)
                    pDialog.dismiss();

                hideDialog();
                if (!s.isEmpty()) {
                    Toast.makeText(getApplicationContext(),
                            "Upload success", Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Upload failed", Toast.LENGTH_LONG)
                            .show();
                }
            }
        };

        task.execute();
    }

    public void uploadFile() {
        try {
            final String image = imageToBase64(filePath);

            pDialog.setMessage("Uploading ...");
            showDialog();

            StringRequest strReq = new StringRequest(Request.Method.POST,
                    serverName + EndPoint.URL_UPLOAD, new Response.Listener<String>() {
                @Override
                public void onResponse(String res) {
                   /* Toast.makeText(getApplicationContext(),
                            res.toString(), Toast.LENGTH_LONG)
                            .show();*/
                    hideDialog();
                    /*res = res.replaceAll("\\r\\n|\\r|\\n", "");
                    if(res.equals("OK"))
                    {
                        hideDialog();
                    }*/
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideDialog();
                    /*Toast.makeText(getApplicationContext(),
                            "No Internet Connection" + error.getMessage(), Toast.LENGTH_LONG)
                            .show();*/
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new Hashtable<>();
                    params.put("RowId", rowIds);
                    params.put("Image", image);

                    return params;
                }
            };

            AppController.getInstance().addToRequestQueue(strReq);
        } catch (FileNotFoundException e) {
            hideDialog();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static class MultipartRequest extends Request<String> {

        MultipartEntityBuilder entity = MultipartEntityBuilder.create();
        HttpEntity httpentity;
        private static final String FILE_PART_NAME = "file";

        private final Response.Listener<String> mListener;
        private final File mFilePart;
        private final Map<String, String> mStringPart;

        public MultipartRequest(String url, Response.ErrorListener errorListener,
                                Response.Listener<String> listener, File file,
                                Map<String, String> mStringPart) {
            super(Method.POST, url, errorListener);

            mListener = listener;
            mFilePart = file;
            this.mStringPart = mStringPart;
            entity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            buildMultipartEntity();
        }

        public void addStringBody(String param, String value) {
            mStringPart.put(param, value);
        }

        private void buildMultipartEntity() {
            entity.addPart(FILE_PART_NAME, new FileBody(mFilePart));
            for (Map.Entry<String, String> entry : mStringPart.entrySet()) {
                entity.addTextBody(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public String getBodyContentType() {
            return httpentity.getContentType().getValue();
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                httpentity = entity.build();
                httpentity.writeTo(bos);
            } catch (IOException e) {
                VolleyLog.e("IOException writing to ByteArrayOutputStream");
            }
            return bos.toByteArray();
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            return Response.success("Uploaded", getCacheEntry());
        }

        @Override
        protected void deliverResponse(String response) {
            mListener.onResponse(response);
        }
    }

    /**
     * Uploading the file to server
     * */
    private class UploadFileToServer extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog.setMessage("Uploading ...");
            showDialog();
        }

        @Override
        protected String doInBackground(Void... params) {
            return uploadFile();
        }

        private String uploadFile() {
            String responseString = null;

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(serverName + EndPoint.URL_UPLOAD);

            try {
                AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
                        new AndroidMultiPartEntity.ProgressListener() {

                            @Override
                            public void transferred(long num) {
                                publishProgress((int) ((num / (float) totalSize) * 100));
                            }
                        });

                File sourceFiles = new File(filePath);
                // Adding file data to http body
                entity.addPart("Image", new FileBody(sourceFiles));

                totalSize = entity.getContentLength();
                httppost.setEntity(entity);

                // Making server call
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // Server response
                    responseString = EntityUtils.toString(r_entity);
                } else {
                    responseString = "Error occurred! Http Status Code: "
                            + statusCode;
                }

            } catch (ClientProtocolException e) {
                responseString = e.toString();
            } catch (IOException e) {
                responseString = e.toString();
            }

            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            hideDialog();
            Toast.makeText(getApplicationContext(),
                    "Upload completed ...",
                    Toast.LENGTH_LONG).show();
        }

    }
}
