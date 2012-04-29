package edu.mit.printAtMIT;

import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.mit.printAtMIT.view.login.MITClientData;
import edu.mit.printAtMIT.view.listPrinter.MainMenuActivity;
import edu.mit.printAtMIT.view.login.ConnectionWrapper;
import edu.mit.printAtMIT.view.login.ConnectionWrapper.ConnectionInterface;
import edu.mit.printAtMIT.view.login.ConnectionWrapper.ErrorType;
import edu.mit.printAtMIT.view.login.LoginActivity;
import edu.mit.printAtMIT.view.login.MITClient;
import edu.mit.printAtMIT.view.login.MITConnectionWrapper;
import edu.mit.printAtMIT.view.login.MobileWebApi.HttpClientType;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
    private static SharedPreferences settings;
    private static final String TAG = "PrintAtMITActivity";
    

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (settings.getString(MITClient.TOUCHSTONE_USERNAME, null) == null) {
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

        if (settings.getString(USERNAME, null) == null) {
            startLogin();
        } else {
        	Intent intent = new Intent(this, MainMenuActivity.class);
        	startActivity(intent);
        	finish();
        }
    }
    private void startLogin() {

        setContentView(R.layout.login);

        final Bundle extras = getIntent().getExtras();

        if (extras != null && extras.getString("error") != null) {
        	String error = extras.getString("error");
        	if (error.equals(PrintAtMITActivity.NO_DATA_ENTRY)) {
        		String text = "Please enter username and password";
	        	Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
	        	toast.show();
        	}
        }
        Button button01 = (Button) findViewById(R.id.touchstoneLoginButton);
        button01.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	EditText touchstoneUsername = (EditText) findViewById(R.id.touchstoneUsername);
                EditText touchstonePassword = (EditText) findViewById(R.id.touchstonePassword);
                
            	SharedPreferences.Editor prefsEditor = settings.edit();
            	prefsEditor.putString(MITClient.TOUCHSTONE_USERNAME, touchstoneUsername.getText().toString());
            	prefsEditor.putString(MITClient.TOUCHSTONE_PASSWORD, touchstonePassword.getText().toString());
            	prefsEditor.commit();
            	
            	if (extras != null) {
            		String requestKey = extras.getString("requestKey");
            		MITClientData clientData = (MITClientData)MITClient.requestMap.get(requestKey);
    				clientData.setTouchstoneState(MITClient.TOUCHSTONE_LOGIN);
            	}
            	ConnectionWrapper connection = new ConnectionWrapper(view.getContext());
                // TODO add error handling
            	boolean isStarted = connection.openURL("wayf.mit.edu");
//            	Intent intent = new Intent(view.getContext(), LoginActivity.class);
//            	startActivity(intent);
//            	finish();
//                EditText textfield = (EditText) findViewById(R.id.entry);
//                if (!textfield.getText().toString().equals("")) {
//                    SharedPreferences userSettings = getSharedPreferences(
//                            PrintAtMITActivity.PREFS_NAME, MODE_PRIVATE);
//                    SharedPreferences.Editor editor = userSettings.edit();
//                    editor.putString(PrintAtMITActivity.USERNAME, textfield
//                            .getText().toString());
//                    editor.putString(PrintAtMITActivity.INKCOLOR,
//                            PrintAtMITActivity.BLACKWHITE);
//                    editor.putInt(PrintAtMITActivity.COPIES, 1);
//
//                    // Commit the edits!
//                    editor.commit();
//
//                    Intent intent = new Intent(view.getContext(), MainMenuActivity.class);
//                	startActivity(intent);
//                	finish();
//
//                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        if (!settings.getString(USERNAME, "").equals("")) {
//            MenuInflater inflater = getMenuInflater();
//            inflater.inflate(R.menu.mainmenu_menu, menu);
//        }
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (!settings.getString(USERNAME, "").equals("")) {
//            // Handle item selection
//            switch (item.getItemId()) {
//            case R.id.setting:
//                Intent intent = new Intent(findViewById(android.R.id.content)
//                        .getContext(), SettingsActivity.class);
//                startActivity(intent);
//                return true;
//            case R.id.about:
//            	showAboutDialog();
//                super.onOptionsItemSelected(item);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//            }
//        } else {
//            return super.onOptionsItemSelected(item);
//        }
//    }
    
//    private void showAboutDialog() {
//		showDialog(0);
//	}
//	@Override
//	protected Dialog onCreateDialog(int id) {
//		final Dialog dialog = new Dialog(this);
//    	dialog.setContentView(R.layout.about_dialog);
//    	dialog.setTitle("About");
//    	TextView tv = (TextView) dialog.findViewById(R.id.about_text);
//    	Linkify.addLinks(tv, Linkify.ALL);
//    	tv.setMovementMethod(LinkMovementMethod.getInstance());
//		return dialog;
//	}
}