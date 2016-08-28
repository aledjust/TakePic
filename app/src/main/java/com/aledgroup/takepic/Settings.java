package com.aledgroup.takepic;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aledgroup.takepic.Common.CfUserInfo;
import com.aledgroup.takepic.Utils.RandomStringUUID;
import com.aledgroup.takepic.helper.SQLiteHandler;

import java.util.List;

/**
 * Created by aled on 05/04/2016.
 */
public class Settings extends Activity {

    private Button btnSaveServerName;
    private EditText inputServerName;

    SQLiteHandler db = new SQLiteHandler(this);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        inputServerName = (EditText) findViewById(R.id.serverName);

        List<CfUserInfo> userInfo = db.getAllSettings();
        String serverName = "";
        for (CfUserInfo cn : userInfo) {
            serverName = cn.getSettings();
        }

        inputServerName.setText(serverName);

        btnSaveServerName = (Button) findViewById(R.id.btnSaveAddress);
        btnSaveServerName.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Inserting row in users table
                db.deleteSettings();
                String serverName = inputServerName.getText().toString();
                db.addSettings(RandomStringUUID.GUID(),serverName);
                LoginActivity.serverName = serverName;
                Toast.makeText(getApplicationContext(),
                        "Saved success",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}

