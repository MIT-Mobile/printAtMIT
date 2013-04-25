package edu.mit.printAtMIT;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.UUID;

import org.apache.http.client.HttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.mit.mobile.R;
import edu.mit.mobile.api.ConnectionListener;
import edu.mit.mobile.api.MobileRequest;
import edu.mit.mobile.api.MobileResult;
import edu.mit.mobile.api.MobileTaskException;
import edu.mit.mobile.api.touchstone.BasicTouchstoneManager;
import edu.mit.mobile.api.touchstone.InteractiveTouchstoneManager;
import edu.mit.mobile.demo.TouchstoneTest;
import edu.mit.printAtMIT.controller.client.PrinterClientException;
import edu.mit.printAtMIT.model.touchstoneOld.MobileAPI;
import edu.mit.printAtMIT.model.touchstoneOld.MobileResponseHandler;
import edu.mit.printAtMIT.model.touchstoneOld.PrintAuthenticationHandler;
import edu.mit.printAtMIT.view.listPrinter.MainMenuActivity;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.res.Configuration;

public class PrintAtMITActivity extends Activity {

    public static final String PREFS_NAME = "user_preferences";
    public static final String USERNAME = "kerberosId";
    public static final String INKCOLOR = "inkcolor";
    public static final String COPIES = "copies";

    public static final String BLACKWHITE = "Black and White";
    public static final String COLOR = "Color";

    public static final String NO_DATA_ENTRY = "NO_DATA_ENTRY";
    public static HttpClient HTTP_CLIENT;                     // Singleton to store session after logging in. USE FOR ALL HTTP REQUESTS
    private static SharedPreferences settings;
    private static final String TAG = "PrintAtMITActivity";
    public static final String TOUCHSTONE_USERNAME = "TOUCHSTONE_USERNAME";
	public static final String TOUCHSTONE_PASSWORD = "TOUCHSTONE_PASSWORD";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Log.d(TAG, "testing");
        if (settings.getString(PrintAtMITActivity.TOUCHSTONE_USERNAME, null) == null || settings.getString(PrintAtMITActivity.TOUCHSTONE_USERNAME, null).equals("")) {
            startLogin();
        } else {
        	Intent intent = new Intent(this, MainMenuActivity.class);
        	startActivity(intent);
        	finish();
        }
        
        // Make sure we create a Touchstone manager otherwise the connection will
        // fail with a TOUCHSTONE_UNAVAILABLE error
        MobileRequest.setTouchstoneManager(new InteractiveTouchstoneManager(PrintAtMITActivity.this));

        try {
            KeyStore trusted = KeyStore.getInstance("BKS");
            InputStream in = this.getApplicationContext().getResources().openRawResource(R.raw.mit_ts);
            try {
                trusted.load(in, "304mitca".toCharArray());
            } finally {
                in.close();
            }

            //MobileAPI.setAlternativeTrustStore(trusted);
        } catch (Exception e) {
            Log.i("TT:TrustStore", "Failed to initialize MIT trust store: " + e.getMessage());
        }

        updateCredentialFields();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (settings.getString(PrintAtMITActivity.TOUCHSTONE_USERNAME, null) == null || settings.getString(PrintAtMITActivity.TOUCHSTONE_USERNAME, null).equals("")) {
            startLogin();
        } else {
        	Intent intent = new Intent(this, MainMenuActivity.class);
        	startActivity(intent);
        	finish();
        }
    }

    private void startLogin() {

        setContentView(R.layout.login);
        Button button01 = (Button) findViewById(R.id.touchstoneLoginButton);
        EditText touchstonePassword = (EditText) findViewById(R.id.touchstonePassword);
        
        touchstonePassword.setOnKeyListener(new View.OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
					tryTouchstoneAuthentication();
					return true;
				}
				return false;
			}
		});
        button01.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                tryTouchstoneAuthentication();
            }
        });
    }

    private void tryTouchstoneAuthentication() {
        final ConnectionListener listener = new ConnectionListener() {
            @Override
            public void onConnectionError(UUID id, MobileTaskException exception) {
                Log.e(TAG, "request '" + id + "' failed: " + exception.getMessage());
                //final TextView textView = (TextView) findViewById(R.id.response_view);
                //textView.setText(exception.getLocalizedMessage());
                Toast toast = Toast.makeText(getBaseContext(), "Error logging in: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT);
                toast.show();
                //final Button loginButton = (Button) findViewById(R.id.login_button);
                //loginButton.setEnabled(true);
            }

            @Override
            public void onConnectionFinished(UUID id, MobileResult result) {
                //final TextView textView = (TextView) findViewById(R.id.response_view);
                JSONObject jsonDict = result.getJSONObject();
                JSONArray jsonArray = result.getJSONArray();

                try {
                    if (jsonDict != null) {
                        //textView.setText(jsonDict.toString(2));
                    	Toast toast = Toast.makeText(getBaseContext(), "Error logging in: " + jsonDict.toString(2), Toast.LENGTH_SHORT);
                    	toast.show();
                    } else {
                        if (jsonArray != null) {
                            //textView.setText(jsonArray.toString(2));
                        	Toast toast = Toast.makeText(getBaseContext(), "Error logging in: " + jsonArray.toString(2), Toast.LENGTH_SHORT);
                        	toast.show();
                        }
                    }
                } catch (JSONException e) {
                    //textView.setText(result.getContentBody());
                	Toast toast = Toast.makeText(getBaseContext(), "Error logging in: " + result.getContentBody(), Toast.LENGTH_SHORT);
                	toast.show();
                }

                Log.e(TAG, "request '" + id + "' finished with '" + result.getMimeType() + "'");
                updateCredentialFields();

                //final Button loginButton = (Button) findViewById(R.id.login_button);
                //loginButton.setEnabled(true);
            }
        };

        MobileRequest.execute(new MobileRequest("libraries", "getUserIdentity", null), listener);
    }

    private void updateCredentialFields() {
        final EditText usernameField = (EditText) findViewById(R.id.touchstoneUsername);
        final EditText passwordField = (EditText) findViewById(R.id.touchstonePassword);

        Pair<String,String> credentials = BasicTouchstoneManager.getTouchstoneCredentials(this);

        if (credentials.first != null) {
            usernameField.setText(credentials.first.toString());
        }

        if (credentials.second != null) {
            passwordField.setText(credentials.second.toString());
        }
    }

//
//    private void login(View view) {
//    	EditText touchstoneUsername = (EditText) findViewById(R.id.touchstoneUsername);
//        EditText touchstonePassword = (EditText) findViewById(R.id.touchstonePassword);
//        
//        final String username = touchstoneUsername.getText().toString();
//        final String password = touchstonePassword.getText().toString();
//        Log.d(TAG, "starting login");
//        Log.d(TAG, "username: " + username);
//        final View v = view;
//        final ProgressDialog progress = ProgressDialog.show(view.getContext(), "", "Logging in...");
//    	MobileResponseHandler handler = new MobileResponseHandler() {
//            public void onRequestCompleted(String result, HttpClient client) throws PrinterClientException {
//            	HTTP_CLIENT  = client;
//                Log.d("MobileAPI:libraries", "Request successfully completed:\n----\n'" + result + "'\n----");
//            	SharedPreferences.Editor prefsEditor = settings.edit();
//            	prefsEditor.putString(PrintAtMITActivity.TOUCHSTONE_USERNAME, username);
//            	prefsEditor.putString(PrintAtMITActivity.TOUCHSTONE_PASSWORD, password);
//            	prefsEditor.commit();
//                progress.cancel();
//                Intent intent = new Intent(v.getContext(), MainMenuActivity.class);
//            	startActivity(intent);
//            	finish();
//            }
//
//            public void onCanceled() {
//                Log.d("MobileAPI:libraries", "Request was canceled");
//                progress.cancel();
//                System.out.println("cancelled");
//                Toast toast = Toast.makeText(v.getContext(), "Error logging in: check username and password", Toast.LENGTH_SHORT);
//            	toast.show();
//            }
//            public void onError(int code, String message) {
//                Log.d("MobileAPI:libraries", "Request encountered an error[" + code + "]: " + message);
//                System.out.println(message);
//                progress.cancel();
//                Toast toast = Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT);
//            	toast.show();
//            }
//        };
//
//        try {
//            KeyStore trusted = KeyStore.getInstance("BKS");
//            InputStream in = this.getApplicationContext().getResources().openRawResource(R.raw.mit_ts);
//            try {
//                trusted.load(in, "304mitca".toCharArray());
//            } finally {
//                in.close();
//            }
//            
//            MobileAPI.setAlternativeTrustStore(trusted);
//        } catch (Exception e) {
//            Log.d("TT:TrustStore", "Failed to initialize MIT trust store: " + e.getMessage());
//        }
//
//        MobileAPI apiTest = new MobileAPI(Uri.parse("https://mobile-print-dev.mit.edu/printatmit/query_result/"), null, handler);
//        
//        apiTest.execute(new PrintAuthenticationHandler(username, password));
//    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}