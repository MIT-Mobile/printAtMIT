package edu.mit.printAtMIT.view.print;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.mit.printAtMIT.R;
import edu.mit.printAtMIT.view.listPrinter.MainMenuActivity;
import edu.mit.printAtMIT.view.main.SettingsActivity;

/**
 * Lists files from Downloads folder
 * 
 * Menu Items: Settings About
 */
public class PrintDownloadsActivity extends FileViewActivity {
	private File files;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.print_downloads);
		files = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

		if (files.list() == null) {
			// Need to make this return to previous menu
			Toast.makeText(getApplicationContext(), "No Downloads",
					Toast.LENGTH_SHORT).show();
			return;
		}

		String[] fileNames = files.list();

		setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item,
				fileNames));

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				// Toast.makeText(getApplicationContext(),
				// files.listFiles()[position].toString(),
				// Toast.LENGTH_SHORT).show();
				final String fileName = files.list()[position];
				final int pos = position;
				final View v = view;
				// Must be correct format
				if (fileName.endsWith(".pdf") || fileName.endsWith(".ps") || fileName.endsWith(".txt") 
						|| fileName.endsWith(".gif") || fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")
						|| fileName.endsWith(".png") || fileName.endsWith(".tiff") || fileName.endsWith(".bmp")) {

					AlertDialog.Builder builder = new AlertDialog.Builder(view
							.getContext());
					builder.setMessage("Do you want to print this file?")
							.setCancelable(false)
							.setPositiveButton("Yes",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											Intent intent = new Intent(v
													.getContext(),
													PrintOptionsActivity.class);
											intent.putExtra("fileLoc", files
													.listFiles()[pos]
													.toString());
											intent.putExtra("fileName",
													fileName);
											startActivity(intent);
										}
									})
							.setNegativeButton("No",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									});
					AlertDialog alert = builder.create();
					alert.show();

				} else {
					Toast.makeText(getApplicationContext(),
							"Invalid file type", Toast.LENGTH_SHORT).show();
				}

			}
		});
		
		Button settingsButton = (Button) findViewById(R.id.settings_icon);
    	Button listButton = (Button) findViewById(R.id.list_icon);
    	Button printButton = (Button) findViewById(R.id.printer_icon);
    	
    	printButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(),
						PrintMenuActivity.class);
				startActivity(intent);
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
