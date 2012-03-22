package edu.mit.printAtMIT.view.print;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import edu.mit.printAtMIT.R;
import edu.mit.printAtMIT.view.listPrinter.MainMenuActivity;
import edu.mit.printAtMIT.view.main.SettingsActivity;

/***
 * Show list of things to print: Downloads(for now), Images, Chat, Email Grid of
 * icons
 * 
 * Menu buttons: Settings About
 */
public class PrintMenuActivity extends Activity {
	private static final int PICK_IMAGE = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    
		setContentView(R.layout.print_menu);

		Button downloadsButton = (Button) findViewById(R.id.downloads_image);
		downloadsButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				Intent intent = new Intent(view.getContext(),
						PrintDownloadsActivity.class);
				startActivity(intent);
			}
		});
		
		Button imagesButton = (Button) findViewById(R.id.images_image);
		Button settingsButton = (Button) findViewById(R.id.settings_icon);
    	Button listButton = (Button) findViewById(R.id.list_icon);
		imagesButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				Intent i = new Intent(Intent.ACTION_PICK,
			               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(i, PICK_IMAGE);
			}
		});
    	settingsButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(),
						SettingsActivity.class);
				startActivity(intent);
			}
		});
    	
    	listButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(),
						MainMenuActivity.class);
				startActivity(intent);
			}
		});
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

	    switch(requestCode) { 
	    case PICK_IMAGE:
	        if(resultCode == RESULT_OK){  
	            Uri selectedImage = imageReturnedIntent.getData();

	            String[] filePathColumn = {MediaStore.Images.Media.DATA};

	            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
	            cursor.moveToFirst();

	            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	            String filePath = cursor.getString(columnIndex);
	            cursor.close();
                File f = new File(filePath);
	            //Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);
                
				Intent intent = new Intent(getApplicationContext(), PrintOptionsActivity.class);
				intent.putExtra("fileLoc", filePath);
				intent.putExtra("fileName",	f.getName());
				startActivity(intent);
	        }
	    }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.printmenu_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Intent intent;
		switch (item.getItemId()) {
		case R.id.home:
			intent = new Intent(
					findViewById(android.R.id.content).getContext(),
					MainMenuActivity.class);
			startActivity(intent);
			return true;
		case R.id.setting:
			intent = new Intent(
					findViewById(android.R.id.content).getContext(),
					SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.about:
			showAboutDialog();
	    	
			super.onOptionsItemSelected(item);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void showAboutDialog() {
		showDialog(0);
	}
	@Override
	protected Dialog onCreateDialog(int id) {
		final Dialog dialog = new Dialog(this);
    	dialog.setContentView(R.layout.about_dialog);
    	dialog.setTitle("About");
    	TextView tv = (TextView) dialog.findViewById(R.id.about_text);
    	Linkify.addLinks(tv, Linkify.ALL);
    	tv.setMovementMethod(LinkMovementMethod.getInstance());
		return dialog;
	}
}
