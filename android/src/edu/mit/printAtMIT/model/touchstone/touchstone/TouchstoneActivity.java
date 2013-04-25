package edu.mit.printAtMIT.model.touchstone.touchstone;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import edu.mit.mobile.R;
import edu.mit.mobile.R.id;
import edu.mit.mobile.api.touchstone.InteractiveTouchstoneManager.Constants;

public class TouchstoneActivity extends Activity {
    public static final String TAG = TouchstoneActivity.class.getSimpleName();
    private final int LAYOUT_RESOURCE = R.layout.default_touchstone_activity;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Button loginButton = (Button) findViewById(R.id.touchstoneLoginButton);
            final EditText userField = (EditText) findViewById(id.touchstoneUsername);
            final EditText passwordField = (EditText) findViewById(id.touchstonePassword);
            final CheckBox rememberPassword = (CheckBox) findViewById(id.rememberLoginCB);

            if (intent.hasExtra(Constants.NOTICE_STATUS)) {
                if (intent.hasExtra(Constants.FLAG_SUCCESS)) {
                    Log.e(TAG, "authn request succeeded");
                    Toast.makeText(TouchstoneActivity.this, "Touchstone authentication successful", 3).show();
                    finish();
                } else {
                    if (intent.hasExtra(Constants.FLAG_ERROR)) {
                        if (intent.hasExtra(Constants.FLAG_ERROR_CREDENTIALS)) {
                            Log.e(TAG, "authn request failed due to incorrect credentials");
                            Toast.makeText(TouchstoneActivity.this, "Incorrect username or password", 3).show();
                            passwordField.setText("");
                        } else {
                            Log.e(TAG, "authn request failed due to an error");
                            Toast.makeText(TouchstoneActivity.this, "There was an unknown error while completing your request", 3).show();
                            finish();
                        }
                    } else {
                        if (intent.hasExtra(Constants.FLAG_ABORT)) {
                            // Get out of here, NOW!
                            finish();
                        }
                    }
                }

                loginButton.setEnabled(false);
                userField.setEnabled(false);
                passwordField.setEnabled(false);
                rememberPassword.setEnabled(false);
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(this.LAYOUT_RESOURCE);

        final EditText userField = (EditText) findViewById(id.touchstoneUsername);
        final EditText passwordField = (EditText) findViewById(id.touchstonePassword);
        final CheckBox rememberPassword = (CheckBox) findViewById(id.rememberLoginCB);

        final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(TouchstoneActivity.this);
        final Button loginButton = (Button) findViewById(R.id.touchstoneLoginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent broadcast = new Intent(Constants.BROADCAST_INTENT_TOUCHSTONE);
                broadcast.putExtra(Constants.NOTICE_CREDENTIALS, true);
                broadcast.putExtra(Constants.EXTRA_USERNAME, userField.getText().toString());
                broadcast.putExtra(Constants.EXTRA_PASSWORD, passwordField.getText().toString());
                broadcast.putExtra(Constants.FLAG_ALLOW_SAVE, rememberPassword.isChecked());

                manager.sendBroadcast(broadcast);

                loginButton.setEnabled(false);
                userField.setEnabled(false);
                passwordField.setEnabled(false);
                rememberPassword.setEnabled(false);
            }
        });

        Pair<String,String> credentials = BasicTouchstoneManager.getTouchstoneCredentials(this);

        if (credentials != null) {
            boolean shouldRemember = false;
            if ((credentials.first != null) && (credentials.first.length() > 0)) {
                userField.setText(credentials.first);
                shouldRemember = (credentials.first.length() > 0);
            }

            if ((credentials.second != null) && (credentials.second.length() > 0)) {
                passwordField.setText(credentials.second);
                shouldRemember = shouldRemember && (credentials.second.length() > 0);
            }

            rememberPassword.setChecked(shouldRemember);
        } else {
            rememberPassword.setChecked(false);
        }

        manager.registerReceiver(this.mReceiver, new IntentFilter(Constants.BROADCAST_INTENT_TOUCHSTONE));
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);

        Intent broadcast = new Intent(Constants.BROADCAST_INTENT_TOUCHSTONE);
        broadcast.putExtra(Constants.NOTICE_DESTROYED, true);

        manager.sendBroadcast(broadcast);
        manager.unregisterReceiver(this.mReceiver);

        super.onDestroy();
    }
}