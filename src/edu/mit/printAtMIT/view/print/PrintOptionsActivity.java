package edu.mit.printAtMIT.view.print;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.mit.printAtMIT.PrintAtMITActivity;
import edu.mit.printAtMIT.R;
import edu.mit.printAtMIT.controller.client.PrinterClient;
import edu.mit.printAtMIT.model.print.Lpr;
import edu.mit.printAtMIT.view.list.EntryAdapter;
import edu.mit.printAtMIT.view.list.EntryItem;
import edu.mit.printAtMIT.view.list.Item;
import edu.mit.printAtMIT.view.list.SectionItem;
import edu.mit.printAtMIT.view.listPrinter.MainMenuActivity;
import edu.mit.printAtMIT.view.main.SettingsActivity;

/**
 * User selects print options: bw/color copies Print button
 * 
 * Menu Items: Settings About
 */

public class PrintOptionsActivity extends ListActivity {
    public static final String TAG = "PrintOptionsActivity";
    private String fileLoc;
    private String fileName;
    private String queue;
    private String userName;
    private int numCopies;

    private static final String FILE = "FILE";
    private static final String IMAGE = "IMAGE";
    private static final String WEB = "WEB";
    private String type = FILE;

    private static final String hostName = "mitprint.mit.edu";

    ArrayList<Item> items = new ArrayList<Item>();
    private static final int ITEM_FILENAME = 1;
    private static final int ITEM_USERNAME = 3;
    private static final int ITEM_INKCOLOR = 5;
    private static final int ITEM_COPIES = 6;

    private String getImageFile(Uri imageUri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(imageUri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        fileLoc = cursor.getString(column_index);
        return fileLoc;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.print_options);

        Button btnStart = (Button) findViewById(R.id.print_button);

        btnStart.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                ConvertAndPrintTask printTask = new ConvertAndPrintTask();
                printTask.execute();

            }
        });

        Intent i = getIntent();
        // when called from opening a file or image outside of app (view intent)
        Uri data = i.getData();
        if (data != null) {
            String scheme = data.getScheme();

            // opening an image that's in gallery or previewing from gmail
            if (scheme.equals("content")) {
                if (data.getHost().equals("gmail-ls")) {
                    Log.d("PrintOptionsActivity",
                            "gmail preview - don't do anything");
                    Toast.makeText(this,
                            "Please download item to device before printing",
                            Toast.LENGTH_SHORT).show();
                }
                if (data.getHost().equals("media")) {
                    fileLoc = getImageFile(data);
                    File f = new File(fileLoc);
                    fileName = f.getName();
                    type = IMAGE;
                }
            }
            // opening a file or image not in gallery (ex: downloaded from
            // gmail)
            if (scheme.equals("file")) {
                File f = new File(data.getPath());
                fileLoc = f.getPath().toString();
                fileName = f.getName();

                // must be an image
                if (!(fileName.endsWith(".pdf") || fileName.endsWith(".ps") || fileName
                        .endsWith(".txt")))
                    type = IMAGE;
            }
        }
        // gotten from send (share) intent or print activity
        else {
            // get url from sharing web page (send intent)
            Bundle extras = i.getExtras();
            String url = extras.getString("android.intent.extra.TEXT");

            // Get filepath from uri from sharing image (send intent)
            Uri imageUri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                fileLoc = getImageFile(imageUri);
                File f = new File(fileLoc);
                fileName = f.getName();
                type = IMAGE;
            }

            // if not a shared web page
            if (url == null) {
                // gotten from print activity - pdf, ps, or txt.
                fileName = (fileName == null) ? extras.getString("fileName")
                        : fileName;
                fileLoc = (fileLoc == null) ? extras.getString("fileLoc")
                        : fileLoc;

                // gotten from print activity or send intent - image files, need
                // to be converted
                if (!(fileName.endsWith(".pdf") || fileName.endsWith(".ps") || fileName
                        .endsWith(".txt"))) {
                    type = IMAGE;
                }
            } else {
                fileLoc = url;
                fileName = url;
                type = WEB;
            }
        }

        SharedPreferences userSettings = getSharedPreferences(
                PrintAtMITActivity.PREFS_NAME, MODE_PRIVATE);
        userName = userSettings.getString(PrintAtMITActivity.USERNAME, "");
        numCopies = userSettings.getInt(PrintAtMITActivity.COPIES, 1);
        if (userSettings.getString(PrintAtMITActivity.INKCOLOR,
                PrintAtMITActivity.BLACKWHITE).equals("Color"))
            queue = "color";
        else
            queue = "bw";

        items.add(new SectionItem("File name"));
        items.add(new EntryItem(fileName, fileLoc, ITEM_FILENAME));
        items.add(new SectionItem("Kerberos Id"));
        items.add(new EntryItem("Change Kerberos Id", userSettings.getString(PrintAtMITActivity.TOUCHSTONE_USERNAME, ""), ITEM_USERNAME));
        items.add(new SectionItem("Printer Preferences"));
        items.add(new EntryItem("Ink Color", userSettings.getString(
                PrintAtMITActivity.INKCOLOR, PrintAtMITActivity.BLACKWHITE),
                ITEM_INKCOLOR));
        items.add(new EntryItem("Copies", ""
                + userSettings.getInt(PrintAtMITActivity.COPIES, 1),
                ITEM_COPIES));

        EntryAdapter adapter = new EntryAdapter(this, items);

        setListAdapter(adapter);

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
        inflater.inflate(R.menu.mainmenu_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Ink Color Settings");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.inkcolor_menu, menu);
        // checks the correct option based on saved user preference
        // default is black and white
        SharedPreferences userSettings = getSharedPreferences(
                PrintAtMITActivity.PREFS_NAME, MODE_PRIVATE);
        String color = userSettings.getString(PrintAtMITActivity.INKCOLOR,
                PrintAtMITActivity.BLACKWHITE);
        if (color.equals(PrintAtMITActivity.COLOR)) {
            MenuItem item = (MenuItem) menu.findItem(R.id.color);
            item.setChecked(true);
        } else {
            MenuItem item = (MenuItem) menu.findItem(R.id.bw);
            item.setChecked(true);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        EntryAdapter adapter;
        // changes user preference based on what user has selected
        switch (item.getItemId()) {
        case R.id.bw:
            queue = "bw";
            items.set(ITEM_INKCOLOR, new EntryItem("Ink Color",
                    PrintAtMITActivity.BLACKWHITE, ITEM_INKCOLOR));
            adapter = new EntryAdapter(this, items);
            setListAdapter(adapter);
            return true;
        case R.id.color:
            queue = "color";
            items.set(ITEM_INKCOLOR, new EntryItem("Ink Color",
                    PrintAtMITActivity.COLOR, ITEM_INKCOLOR));
            adapter = new EntryAdapter(this, items);
            setListAdapter(adapter);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (!items.get(position).isSection()) {
            switch (position) {
            // popup dialog appears for username
            // saves user-inputted username
            case ITEM_USERNAME:
                final Dialog dialog = new Dialog(this);

                dialog.setContentView(R.layout.username_dialog);
                dialog.setTitle("Change Kerberos Id");
                dialog.show();

                Button saveButton = (Button) dialog.findViewById(R.id.save);
                EditText textfield = (EditText) dialog
                        .findViewById(R.id.change_username);
                textfield.setImeOptions(EditorInfo.IME_ACTION_DONE);

                saveButton.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View view) {
                        SharedPreferences userSettings = getSharedPreferences(
                                PrintAtMITActivity.PREFS_NAME, MODE_PRIVATE);
                        EditText textfield = (EditText) dialog
                                .findViewById(R.id.change_username);
                        SharedPreferences.Editor editor = userSettings.edit();
                        editor.putString(PrintAtMITActivity.USERNAME, textfield
                                .getText().toString());
                        userName = textfield.getText().toString();
                        editor.commit();

                        items.set(ITEM_USERNAME, new EntryItem(
                                "Change Kerberos Id", textfield.getText()
                                        .toString(), ITEM_USERNAME));
                        EntryAdapter adapter = new EntryAdapter(view
                                .getContext(), items);
                        setListAdapter(adapter);
                        dialog.dismiss();
                    }
                });

                return;
            case ITEM_FILENAME:
                /*
                 * final Dialog fileDialog = new Dialog(this);
                 * 
                 * fileDialog.setContentView(R.layout.about_dialog);
                 * fileDialog.setTitle(fileName); TextView filename = (TextView)
                 * fileDialog.findViewById(R.id.about_text);
                 * filename.setText(fileLoc); fileDialog.show();
                 */
                return;
                // context menu appears for ink color
            case ITEM_INKCOLOR:
                registerForContextMenu(v);
                v.setLongClickable(false);
                this.openContextMenu(v);

                break;
            case ITEM_COPIES:
                // dialog pops up for copy number
                final View view = v;

                final SharedPreferences userSettings = getSharedPreferences(
                        PrintAtMITActivity.PREFS_NAME, MODE_PRIVATE);

                final EditText copy = new EditText(this);
                copy.setInputType(InputType.TYPE_CLASS_NUMBER);

                AlertDialog.Builder builder = new AlertDialog.Builder(
                        v.getContext());
                builder.setMessage("Number of copies:")
                        .setCancelable(false)
                        .setView(copy)
                        .setPositiveButton("Save",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        int copies = userSettings.getInt(
                                                PrintAtMITActivity.COPIES, 1);
                                        String text = copy.getText().toString();
                                        copies = (text.equals("") || text
                                                .equals("0")) ? copies
                                                : Integer.parseInt(copy
                                                        .getText().toString());
                                        numCopies = copies;

                                        items.set(ITEM_COPIES, new EntryItem(
                                                "Copies", "" + copies,
                                                ITEM_COPIES));

                                        EntryAdapter adapter = new EntryAdapter(
                                                view.getContext(), items);
                                        setListAdapter(adapter);

                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();

                // have soft keyboard automatically show up
                alert.getWindow()
                        .setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

                break;
            default:
                Toast.makeText(this, "herp derp", Toast.LENGTH_SHORT).show();
                break;
            }

        }

        super.onListItemClick(l, v, position, id);
    	if(!items.get(position).isSection()){
    		switch(position) {
    		//popup dialog appears for username
    		//saves user-inputted username
    		case ITEM_USERNAME:
    			final Dialog dialog = new Dialog(this);

    			dialog.setContentView(R.layout.username_dialog);
    			dialog.setTitle("Change Kerberos Id");
    			dialog.show();
    			
    			Button saveButton = (Button) dialog.findViewById(R.id.save);
    			EditText textfield = (EditText) dialog.findViewById(R.id.change_username);
    			textfield.setImeOptions(EditorInfo.IME_ACTION_DONE);
    	        
    	        saveButton.setOnClickListener(new View.OnClickListener() {

    	            public void onClick(View view) {
    	            	SharedPreferences userSettings = getSharedPreferences(PrintAtMITActivity.PREFS_NAME, MODE_PRIVATE);
    	            	EditText textfield = (EditText) dialog.findViewById(R.id.change_username);
    	            	SharedPreferences.Editor editor = userSettings.edit();
    	                editor.putString(PrintAtMITActivity.TOUCHSTONE_USERNAME, textfield.getText().toString());
    	                userName = textfield.getText().toString();
    	                editor.commit();
    	      
    	                items.set(ITEM_USERNAME, new EntryItem("Change Kerberos Id", textfield.getText().toString(), ITEM_USERNAME));
    	                EntryAdapter adapter = new EntryAdapter(view.getContext(), items);
    	                setListAdapter(adapter);
    	                dialog.dismiss();
    	            }
    	        });
    	        
        		return;
    		case ITEM_FILENAME:
/*    			final Dialog fileDialog = new Dialog(this);

    			fileDialog.setContentView(R.layout.about_dialog);
    			fileDialog.setTitle(fileName);
    			TextView filename = (TextView) fileDialog.findViewById(R.id.about_text);
    			filename.setText(fileLoc);
    			fileDialog.show();
    	        */
        		return;
        	//context menu appears for ink color
    		case ITEM_INKCOLOR: 
    			registerForContextMenu( v ); 
    		    v.setLongClickable(false); 
    		    this.openContextMenu(v);
    		   
    			break;
    		case ITEM_COPIES:
    			// dialog pops up for copy number
    			final View view = v;

    			final SharedPreferences userSettings = getSharedPreferences(PrintAtMITActivity.PREFS_NAME, MODE_PRIVATE);
    		  
    			final EditText copy = new EditText(this);
    			copy.setInputType(InputType.TYPE_CLASS_NUMBER);
              
    			AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
      		  	builder.setMessage("Number of copies:")
      		  			.setCancelable(false)
	        	        .setView(copy)
	        	        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
	        	        	public void onClick(DialogInterface dialog, int id) {	        	        		
	        	    			int copies = userSettings.getInt(PrintAtMITActivity.COPIES, 1);
	        	    			String text = copy.getText().toString();
	        	        		copies = (text.equals("") || text.equals("0")) ? copies : Integer.parseInt(copy.getText().toString());
	        	       	      	numCopies = copies;
	        	       	      	
	         	                items.set(ITEM_COPIES, new EntryItem("Copies", "" + copies, ITEM_COPIES));
	
	         	                EntryAdapter adapter = new EntryAdapter(view.getContext(), items);
	         	                setListAdapter(adapter);
	         	                
	         	                dialog.dismiss();
	        	             }
	        	         })
	        	         .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        	             public void onClick(DialogInterface dialog, int id) {
	        	                  dialog.cancel();
	        	             }
	        	         });
      		  	AlertDialog alert = builder.create();
	        	alert.show();
	        	
	        	// have soft keyboard automatically show up
				alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

	        	break;
    		default: Toast.makeText(this, "herp derp", Toast.LENGTH_SHORT).show(); break;
    		}
    		
    	}
    	
    	super.onListItemClick(l, v, position, id);
    }

    public class ConvertAndPrintTask extends AsyncTask<Void, Void, Boolean> {
        private ProgressDialog dialog;
        private boolean error = false;

        @Override
        protected void onPreExecute() {
            Log.i("AsyncTask", "onPreExecute");
            dialog = ProgressDialog.show(PrintOptionsActivity.this, "",
                    "Sending print job...", true);
        }

        private String convertImage(String imgLoc) {
            Log.i(TAG, "starting image converting");
            Log.i(TAG, "imgLoc: " + imgLoc);
            String intStorageDirectory = getFilesDir().toString();
            File f = new File(intStorageDirectory, "printAtMIT.pdf");
            Document document = new Document();

            try {
                Image jpg = Image.getInstance(imgLoc);
                Log.i(TAG, "height: " + jpg.getHeight());
                Log.i(TAG, "width: " + jpg.getWidth());

                // create a writer that listens to the document and directs a
                // PDF-stream to a file
                FileOutputStream fos = new FileOutputStream(f);
                PdfWriter.getInstance(document, fos);
                document.open();

                // scale image to fit pdf page
                jpg.scalePercent((document.right() - document.left())
                        / jpg.getWidth() * 100);
                document.add(jpg);

            } catch (BadElementException e) {
                e.printStackTrace();
                error = true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                error = true;
            } catch (IOException e) {
                e.printStackTrace();
                error = true;
            } catch (DocumentException e) {
                e.printStackTrace();
                error = true;
            }
            document.close();
            Log.i(TAG, "finished converting image");
            return f.getPath();
        }

        private String convertUrl(String url) {
            try {
                String encodedURL = URLEncoder.encode(url, "UTF-8");
                String uri = String.format(PrinterClient.HTMLTOPDF_URL, encodedURL);
                HttpGet request = new HttpGet();
                request.setURI(new URI(uri));
                HttpResponse response = (PrintAtMITActivity.HTTP_CLIENT).execute(request);
                HttpEntity resEntity = response.getEntity();
                String intStorageDirectory = getFilesDir().toString();
                File f = new File(intStorageDirectory, "printAtMIT.pdf");

                if (response.getStatusLine().getStatusCode() == 200) {
                    Log.i("ConvertAndPrintTask", "200 code");

                    InputStream inputStream = resEntity.getContent();
                    OutputStream out = new FileOutputStream(f);

                    int read = 0;
                    byte[] bytes = new byte[1024];

                    while ((read = inputStream.read(bytes)) != -1) {
                        out.write(bytes, 0, read);
                    }

                    inputStream.close();
                    out.flush();
                    out.close();
                }
                else {
                    Log.i("ConvertAndPrintTask", "400 code");

                    error = true;
                }

            } catch (Exception e) {
                Log.i("ConvertAndPrintTask", "Error converting url");
                e.printStackTrace();
                error = true;
            }

            String intStorageDirectory = getFilesDir().toString();
            File f = new File(intStorageDirectory, "printAtMIT.pdf");
            return f.getPath();
        }

        @Override
        protected Boolean doInBackground(Void... params) { // This runs on a
                                                           // different thread
            if (type == IMAGE) {
                File imgFile = new File(fileLoc);
                fileName = imgFile.getName();
                fileLoc = convertImage(fileLoc);
            }
            if (type == WEB) {
                fileName = fileLoc;
                fileLoc = convertUrl(fileLoc);
            }

            Lpr lpr = new Lpr();
            try {
                File f = new File(fileLoc);
                lpr.printFile(f, userName, hostName, queue, fileName, numCopies);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.i("ConvertAndPrintTask", "doInBackground: IOException");
                error = true;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.i("ConvertAndPrintTask", "doInBackground: Exception");
                error = true;
            }
            return error;
        }

        @Override
        protected void onCancelled() {
            Log.i("ConvertAndPrintTask", "Cancelled.");
        }

        @Override
        protected void onPostExecute(Boolean result) {
            dialog.dismiss();
            String intStorageDirectory = getFilesDir().toString();
            File f = new File(intStorageDirectory, "printAtMIT.pdf");
            f.delete();

            if (result) {
                finish();
                Toast.makeText(getApplicationContext(),
                        "Error sending, try again", Toast.LENGTH_SHORT).show();
                Log.i("ConvertAndPrintTask",
                        "onPostExecute: Completed with an Error.");
            } else {
                finish();
                Toast.makeText(getApplicationContext(), "Successfully sent",
                        Toast.LENGTH_SHORT).show();
                Log.i("ConvertAndPrintTask", "onPostExecute: Completed.");
            }
        }
    }
}