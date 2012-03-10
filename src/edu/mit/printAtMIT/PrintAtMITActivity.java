package edu.mit.printAtMIT;

import com.parse.Parse;

import edu.mit.printAtMIT.view.listPrinter.MainMenuActivity;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.content.res.Configuration;

public class PrintAtMITActivity extends Activity {

    public static final String PREFS_NAME = "user_preferences";
    public static final String USERNAME = "kerberosId";
    public static final String INKCOLOR = "inkcolor";
    public static final String COPIES = "copies";

    public static final String BLACKWHITE = "Black and White";
    public static final String COLOR = "Color";

    private static SharedPreferences settings;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(this, "KIb9mNtPKDtkDk7FJ9W6b7MiAr925a10vNuCPRer",
                "dSFuQYQXSvslh9UdznzzS9Vb0kDgcKnfzgglLUHT");
        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (settings.getString(USERNAME, "").equals("")) {
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
        Parse.initialize(this, "KIb9mNtPKDtkDk7FJ9W6b7MiAr925a10vNuCPRer",
                "dSFuQYQXSvslh9UdznzzS9Vb0kDgcKnfzgglLUHT");

        if (settings.getString(USERNAME, "").equals("")) {
            startLogin();
        } else {
        	Intent intent = new Intent(this, MainMenuActivity.class);
        	startActivity(intent);
        	finish();
        }
    }
    private void startLogin() {

        setContentView(R.layout.login);

        Button button01 = (Button) findViewById(R.id.continue_button);
        button01.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                EditText textfield = (EditText) findViewById(R.id.entry);
                if (!textfield.getText().toString().equals("")) {
                    SharedPreferences userSettings = getSharedPreferences(
                            PrintAtMITActivity.PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = userSettings.edit();
                    editor.putString(PrintAtMITActivity.USERNAME, textfield
                            .getText().toString());
                    editor.putString(PrintAtMITActivity.INKCOLOR,
                            PrintAtMITActivity.BLACKWHITE);
                    editor.putInt(PrintAtMITActivity.COPIES, 1);

                    // Commit the edits!
                    editor.commit();

                    Intent intent = new Intent(view.getContext(), MainMenuActivity.class);
                	startActivity(intent);
                	finish();

                }
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