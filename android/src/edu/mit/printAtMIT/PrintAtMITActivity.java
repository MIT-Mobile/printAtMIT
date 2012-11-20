package edu.mit.printAtMIT;

import java.io.InputStream;
import java.security.KeyStore;

import org.apache.http.client.HttpClient;
import edu.mit.printAtMIT.controller.client.PrinterClientException;
import edu.mit.printAtMIT.model.touchstone.MobileAPI;
import edu.mit.printAtMIT.model.touchstone.MobileResponseHandler;
import edu.mit.printAtMIT.model.touchstone.PrintAuthenticationHandler;
import edu.mit.printAtMIT.view.listPrinter.MainMenuActivity;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
					login(v);
					return true;
				}
				return false;
			}
		});
        button01.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	login(view);
            }
        });
    }

    private void login(View view) {
    	EditText touchstoneUsername = (EditText) findViewById(R.id.touchstoneUsername);
        EditText touchstonePassword = (EditText) findViewById(R.id.touchstonePassword);
        
        final String username = touchstoneUsername.getText().toString();
        final String password = touchstonePassword.getText().toString();
        Log.d(TAG, "starting login");
        Log.d(TAG, "username: " + username);
        final View v = view;
        final ProgressDialog progress = ProgressDialog.show(view.getContext(), "", "Logging in...");
    	MobileResponseHandler handler = new MobileResponseHandler() {
            public void onRequestCompleted(String result, HttpClient client) throws PrinterClientException {
            	HTTP_CLIENT  = client;
                Log.d("MobileAPI:libraries", "Request successfully completed:\n----\n'" + result + "'\n----");
            	SharedPreferences.Editor prefsEditor = settings.edit();
            	prefsEditor.putString(PrintAtMITActivity.TOUCHSTONE_USERNAME, username);
            	prefsEditor.putString(PrintAtMITActivity.TOUCHSTONE_PASSWORD, password);
            	prefsEditor.commit();
                progress.cancel();
                Intent intent = new Intent(v.getContext(), MainMenuActivity.class);
            	startActivity(intent);
            	finish();
            }

            public void onCanceled() {
                Log.d("MobileAPI:libraries", "Request was canceled");
                progress.cancel();
                System.out.println("cancelled");
                Toast toast = Toast.makeText(v.getContext(), "Error logging in: check username and password", Toast.LENGTH_SHORT);
            	toast.show();
            }
            public void onError(int code, String message) {
                Log.d("MobileAPI:libraries", "Request encountered an error[" + code + "]: " + message);
                System.out.println(message);
                progress.cancel();
                Toast toast = Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT);
            	toast.show();
            }
        };

        try {
            KeyStore trusted = KeyStore.getInstance("BKS");
            InputStream in = this.getApplicationContext().getResources().openRawResource(R.raw.mit_ts);
            try {
                trusted.load(in, "304mitca".toCharArray());
            } finally {
                in.close();
            }
            
            MobileAPI.setAlternativeTrustStore(trusted);
        } catch (Exception e) {
            Log.d("TT:TrustStore", "Failed to initialize MIT trust store: " + e.getMessage());
        }

        MobileAPI apiTest = new MobileAPI(Uri.parse("https://mobile-print-dev.mit.edu/printatmit/query_result/"), null, handler);
        
        apiTest.execute(new PrintAuthenticationHandler(username, password));
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}